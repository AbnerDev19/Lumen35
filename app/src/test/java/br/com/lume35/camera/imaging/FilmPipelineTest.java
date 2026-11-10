package br.com.lume35.camera.imaging;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;

public class FilmPipelineTest {
    @Test public void cubicCurveKeepsKnotsAndIsMonotone() {
        float[] values = MonotoneCurve.lookup(new int[]{4, 27, 86, 140, 212, 249, 253});
        assertEquals(256, values.length);
        assertEquals(86 / 255f, values[80], .0001f);
        assertEquals(253 / 255f, values[255], .0001f);
        assertNotEquals((27 + 86) / 2f / 255f, values[56], .0001f);
        for (int i = 1; i < values.length; i++) assertTrue(values[i] >= values[i - 1]);
    }

    @Test public void allCurvesHaveDistinctResponseAndBoundedBlackPoint() {
        HashSet<Integer> curves = new HashSet<>();
        for (FilmRecipe film : FilmRecipe.values()) {
            assertTrue(curves.add(Arrays.deepHashCode(film.curve)));
            float limit = (film == FilmRecipe.PORCELANA || film == FilmRecipe.EXPIRED ? 27 : 10) / 255f;
            assertTrue(film.name(), film.blackPoint() <= limit);
        }
        assertEquals(21, curves.size());
    }

    @Test public void zeroIntensityPreservesEveryPixelWithEffectsEnabled() {
        int[] scene = synthetic(48, 40), developed = scene.clone();
        for (FilmRecipe film : FilmRecipe.values()) {
            plan(film, 0, 9, true).apply(developed, 48, 40);
            assertArrayEquals(scene, developed);
        }
    }

    @Test public void differentSeedsMoveGrainAndLeak() {
        int[] a = synthetic(64, 52), b = a.clone(), c = a.clone();
        plan(FilmRecipe.EXPIRED, 1, 123, true).apply(a, 64, 52);
        plan(FilmRecipe.EXPIRED, 1, 123, true).apply(b, 64, 52);
        plan(FilmRecipe.EXPIRED, 1, 456, true).apply(c, 64, 52);
        assertArrayEquals(a, b);
        assertFalse(Arrays.equals(a, c));
    }

    @Test public void previewQuantizedCurvesTrackCpuToneOnSyntheticColors() {
        for (FilmRecipe film : FilmRecipe.values()) {
            FilmPipeline plan = new FilmPipeline(film, 1, 1, 0, 0, 0, 0, 1, 0, false, false, 7);
            for (int input : synthetic(40, 32)) {
                int file = plan.develop(input, input, .5f, .5f, 0, 0, 0);
                float r = channel(input, 16), g = channel(input, 8), b = channel(input, 0);
                float y = r * film.redMix + g * film.greenMix + b * (1 - film.redMix - film.greenMix);
                float saturation = film.saturation;
                if (film != FilmRecipe.ORIGINAL) saturation *= 1 - .34f * smooth(.67f, 1, y)
                        - .32f * (1 - smooth(0, .24f, y));
                float split = 4 * y * (1 - y) * mix(film.shadow, film.light, y);
                float[] values = {r, g, b};
                for (int index = 0; index < 3; index++) {
                    float position = clamp(mix(y, mix(values[index], values[(index + 1) % 3], film.cross), saturation)) * 255;
                    int start = (int) position, end = Math.min(255, start + 1);
                    float preview = mix(Math.round(film.curve[index][start] * 255) / 255f,
                            Math.round(film.curve[index][end] * 255) / 255f, position - start);
                    preview = clamp(preview + split * (index == 0 ? 1 : index == 1 ? .21f : -1));
                    assertEquals(film.name(), channel(file, (2 - index) * 8), preview, 1.6f / 255);
                }
            }
        }
    }

    private static FilmPipeline plan(FilmRecipe film, float strength, long seed, boolean effects) {
        return new FilmPipeline(film, strength, 1.05f, .015f, .06f, .15f, .2f, 1.1f, 0, effects, effects, seed);
    }

    private static int[] synthetic(int width, int height) {
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int r = x * 255 / (width - 1), g = y * 255 / (height - 1), b = (x + y) * 255 / (width + height - 2);
            pixels[y * width + x] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
        return pixels;
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float mix(float a, float b, float f) { return a * (1 - f) + b * f; }
    private static float channel(int rgb, int shift) { return ((rgb >>> shift) & 255) / 255f; }
    private static float smooth(float a, float b, float value) {
        float t = clamp((value - a) / (b - a));
        return t * t * (3 - 2 * t);
    }
}
