package br.com.lume35.camera.imaging;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;

/** Immutable development settings used by both renderers. */
public final class FilmPipeline {
    public static final int TILE_SIZE = 256;
    static final int[] GRAIN = grainTile();
    private static final int THREADS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()));
    private static final ExecutorService ROWS = Executors.newFixedThreadPool(THREADS, work -> {
        Thread thread = new Thread(work, "Lume film rows");
        thread.setDaemon(true);
        return thread;
    });

    final FilmRecipe film;
    final float strength, vignette, contrast, warmth, softness, aberration, oval, halo;
    final float offsetX, offsetY, toe, flare;
    final float leakR, leakG, leakB;
    final float[][] leaks = new float[2][4];

    public FilmPipeline(FilmRecipe film, float strength, float contrast, float warmth,
                        float vignette, float softness, float aberration, float oval,
                        float leakChance, boolean leak, boolean halo, long seed) {
        this(film, strength, contrast, warmth, vignette, softness, aberration, oval,
                leakChance, leak, halo, seed, 0, 0);
    }

    public FilmPipeline(FilmRecipe film, float strength, float contrast, float warmth,
                        float vignette, float softness, float aberration, float oval,
                        float leakChance, boolean leak, boolean halo, long seed, int toe, float flare) {
        this(film, strength, contrast, warmth, vignette, softness, aberration, oval,
                leakChance, leak, halo, seed, toe, flare, 0, -1, 100);
    }

    public FilmPipeline(FilmRecipe film, float strength, float contrast, float warmth,
                        float vignette, float softness, float aberration, float oval,
                        float leakChance, boolean leak, boolean halo, long seed, int toe, float flare,
                        int leakColor, int haloPercent, int vignettePercent) {
        leakR = leakColor == 3 ? .7f : 1;
        leakG = switch (leakColor) { case 1 -> .67f; case 2 -> .20f; case 3 -> .35f; default -> .39f; };
        leakB = switch (leakColor) { case 1 -> .13f; case 2 -> .10f; case 3 -> 1; default -> .07f; };
        this.film = film;
        this.strength = clamp(strength);
        this.contrast = contrast;
        this.warmth = warmth;
        this.vignette = Math.min(.34f, film.vignette + vignette) * clamp(vignettePercent / 100f);
        this.softness = softness;
        this.aberration = aberration;
        this.oval = oval;
        this.halo = haloPercent < 0 ? (halo ? Math.max(.26f, film.halo) : film.halo)
                : .52f * clamp(haloPercent / 100f);
        this.toe = Math.max(-film.blackPoint(), Math.min(toe / 255f, (film == FilmRecipe.EXPIRED
                || film == FilmRecipe.PORCELANA ? .11f : 10 / 255f) - film.blackPoint()));
        this.flare = flare;
        Random random = new Random(seed);
        offsetX = random.nextFloat() * TILE_SIZE;
        offsetY = random.nextFloat() * TILE_SIZE;
        if (leak || film.leak > 0 || (film != FilmRecipe.ORIGINAL && random.nextFloat() < leakChance)) {
            configureLeak(leaks[0], random, film.leak > 0 ? .32f : .19f);
            if (random.nextBoolean()) configureLeak(leaks[1], random, .14f);
        }
    }

    private static void configureLeak(float[] leak, Random random, float amount) {
        boolean vertical = random.nextBoolean();
        float edge = random.nextBoolean() ? -.07f : 1.07f;
        float position = .08f + random.nextFloat() * .84f;
        leak[0] = vertical ? position : edge;
        leak[1] = vertical ? edge : position;
        leak[2] = .14f + random.nextFloat() * .14f;
        leak[3] = amount * (.75f + random.nextFloat() * .5f);
    }

    public void apply(int[] pixels, int width, int height) {
        if (strength == 0) return;
        int[] samples = softness > 0 || aberration > 0 ? pixels.clone() : pixels;
        int gw = Math.max(1, width / 8), gh = Math.max(1, height / 8);
        int[] glow = halo > 0 || flare > 0 ? highlights(samples, width, height, gw, gh) : null;
        float grainScale = grainScale(width, height);
        ArrayList<Callable<Void>> tasks = new ArrayList<>(THREADS);
        for (int stripe = 0; stripe < THREADS; stripe++) {
            int start = height * stripe / THREADS, end = height * (stripe + 1) / THREADS;
            tasks.add(() -> {
                for (int y = start; y < end; y++) {
                    for (int x = 0; x < width; x++) {
                        float u = (x + .5f) / width, v = (y + .5f) / height;
                        int original = samples[y * width + x];
                        float radius = radius(u, v);
                        int optic = optics(samples, width, height, x, y, original, radius);
                        float noise = film.grain == 0 ? 0 : noise(x / grainScale + offsetX, y / grainScale + offsetY);
                        float highlight = glow == null ? 0 : sample(glow, gw, gh, u * gw - .5f, v * gh - .5f, false);
                        pixels[y * width + x] = develop(original, optic, u, v, radius, noise, highlight);
                    }
                }
                return null;
            });
        }
        try {
            for (var task : ROWS.invokeAll(tasks)) task.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Film development interrupted", error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("Film development failed", error.getCause());
        }
    }

    private int optics(int[] pixels, int width, int height, int x, int y, int value, float radius) {
        if (softness == 0 && aberration == 0) return value;
        float edge = smooth(.25f, 1.25f, radius);
        int distance = Math.max(1, Math.round(Math.min(width, height) * .0024f));
        int a = pixels[y * width + Math.max(0, x - distance)];
        int b = pixels[y * width + Math.min(width - 1, x + distance)];
        int c = pixels[Math.max(0, y - distance) * width + x];
        int d = pixels[Math.min(height - 1, y + distance) * width + x];
        float mix = softness * (.22f + .78f * edge);
        return pack(
                mix(mix(red(value), (red(a) + red(b) + red(c) + red(d)) * .25f, mix), red(pixels[y * width + Math.min(width - 1, x + 1)]), aberration * edge),
                mix(green(value), (green(a) + green(b) + green(c) + green(d)) * .25f, mix),
                mix(mix(blue(value), (blue(a) + blue(b) + blue(c) + blue(d)) * .25f, mix), blue(pixels[y * width + Math.max(0, x - 1)]), aberration * edge));
    }

    int develop(int original, int optic, float u, float v, float radius, float noise, float highlight) {
        float r = red(optic), g = green(optic), b = blue(optic);
        float y = r * film.redMix + g * film.greenMix + b * (1 - film.redMix - film.greenMix);
        float saturation = film.saturation * (film == FilmRecipe.ORIGINAL ? 1
                : 1 - .34f * smooth(.67f, 1, y) - .32f * (1 - smooth(0, .24f, y)));
        float rr = mix(y, mix(r, g, film.cross), saturation);
        float gg = mix(y, mix(g, b, film.cross), saturation);
        float bb = mix(y, mix(b, r, film.cross), saturation);
        float mid = 4 * y * (1 - y);
        float split = mid * (film.shadow * (1 - y) + film.light * y + warmth);
        float delta = (contrast - 1) * (y - .5f) * mid + toe * (1 - smooth(0, .34f, y));
        r = lut(0, rr) + split + delta;
        g = lut(1, gg) + split * .21f + delta;
        b = lut(2, bb) - split + delta;
        float texture = noise * film.grain * mid;
        float falloff = 1 - vignette * smooth(.37f, 1.47f, radius);
        r = softLight(r, texture) * falloff;
        g = softLight(g, texture) * falloff;
        b = softLight(b, texture) * falloff;
        float first = leak(leaks[0], u, v), second = leak(leaks[1], u, v), glow = highlight * halo;
        r = screen(r, leakR * (first + second) + glow);
        g = screen(g, leakG * (first + second * .487f) + glow * .24f);
        b = screen(b, leakB * (first + second * .429f) + glow * .07f);
        float bloom = highlight * flare;
        r = screen(r, bloom);
        g = screen(g, bloom * .74f);
        b = screen(b, bloom * .5f);
        return pack(mix(red(original), r, strength), mix(green(original), g, strength), mix(blue(original), b, strength));
    }

    static float grainScale(int width, int height) { return Math.max(.4f, Math.min(width, height) / 790f); }
    private float radius(float u, float v) {
        float x = (u - .5f) * 2 * oval, y = (v - .5f) * 2;
        return x * x + y * y;
    }

    private float lut(int channel, float value) {
        float p = clamp(value) * 255;
        int i = (int) p;
        return mix(film.curve[channel][i], film.curve[channel][Math.min(255, i + 1)], p - i);
    }

    private static float leak(float[] leak, float u, float v) {
        float x = (u - leak[0]) / Math.max(.01f, leak[2]), y = (v - leak[1]) / Math.max(.01f, leak[2]);
        return leak[3] * (1 - smooth(0, 3.7f, x * x + y * y));
    }

    private static float softLight(float color, float noise) {
        color = clamp(color);
        float high = color <= .25f ? ((16 * color - 12) * color + 4) * color : (float) Math.sqrt(color);
        return color + noise * (noise >= 0 ? high - color : color * (1 - color));
    }

    private static int[] highlights(int[] source, int width, int height, int gw, int gh) {
        int[] glow = new int[gw * gh], temp = new int[glow.length];
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                float peak = 0;
                for (int dy = 1; dy <= 5; dy += 4) {
                    for (int dx = 1; dx <= 5; dx += 4) {
                        int pixel = source[Math.min(height - 1, y * 8 + dy) * width + Math.min(width - 1, x * 8 + dx)];
                        peak += smooth(.73f, .99f, .2126f * red(pixel) + .7152f * green(pixel) + .0722f * blue(pixel));
                    }
                }
                glow[y * gw + x] = Math.round(peak * 63.75f);
            }
        }
        int radius = Math.max(1, Math.round(Math.min(gw, gh) * .013f));
        for (int pass = 0; pass < 3; pass++) {
            BoxBlur.blurLines(glow, temp, gh, gw, gw, 1, radius);
            BoxBlur.blurLines(temp, glow, gw, gh, 1, gw, radius);
        }
        return glow;
    }

    private static int[] grainTile() {
        Random random = new Random(35029);
        int[] raw = new int[TILE_SIZE * TILE_SIZE], blurred = new int[raw.length], tile = new int[raw.length];
        for (int i = 0; i < raw.length; i++) raw[i] = Math.round(255 * clamp(.5f + .2f * (float) random.nextGaussian()));
        BoxBlur.blurLines(raw, blurred, TILE_SIZE, TILE_SIZE, TILE_SIZE, 1, 1);
        BoxBlur.blurLines(blurred, tile, TILE_SIZE, TILE_SIZE, 1, TILE_SIZE, 1);
        for (int i = 0; i < raw.length; i++) tile[i] = Math.round(raw[i] * .65f + tile[i] * .35f);
        return tile;
    }

    private static float noise(float x, float y) {
        return (sample(GRAIN, TILE_SIZE, TILE_SIZE, x, y, true) - .5f) * 5.4f;
    }

    private static float sample(int[] pixels, int w, int h, float x, float y, boolean repeat) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        int x0 = repeat ? ix & (w - 1) : Math.max(0, Math.min(w - 1, ix));
        int x1 = repeat ? (ix + 1) & (w - 1) : Math.max(0, Math.min(w - 1, ix + 1));
        int y0 = repeat ? iy & (h - 1) : Math.max(0, Math.min(h - 1, iy));
        int y1 = repeat ? (iy + 1) & (h - 1) : Math.max(0, Math.min(h - 1, iy + 1));
        return mix(mix(pixels[y0 * w + x0], pixels[y0 * w + x1], x - ix),
                mix(pixels[y1 * w + x0], pixels[y1 * w + x1], x - ix), y - iy) / 255f;
    }

    static float clamp(float x) { return Math.max(0, Math.min(1, x)); }
    private static float smooth(float a, float b, float x) { float t = clamp((x - a) / (b - a)); return t * t * (3 - 2 * t); }
    private static float mix(float a, float b, float t) { return a + (b - a) * t; }
    private static float screen(float a, float b) { return 1 - (1 - a) * (1 - clamp(b)); }
    private static float red(int rgb) { return ((rgb >>> 16) & 255) / 255f; }
    private static float green(int rgb) { return ((rgb >>> 8) & 255) / 255f; }
    private static float blue(int rgb) { return (rgb & 255) / 255f; }
    private static int pack(float r, float g, float b) {
        return 0xff000000 | (Math.round(clamp(r) * 255) << 16) | (Math.round(clamp(g) * 255) << 8) | Math.round(clamp(b) * 255);
    }
}
