package br.com.lume35.camera.imaging;

/** Operates on a reduced, aspect-preserving image and a geometrically registered matte. */
final class PortraitBokeh {
    private static final int TAPS = 29;
    private static final float[] DX = new float[TAPS];
    private static final float[] DY = new float[TAPS];
    static {
        for (int i = 1; i < TAPS; i++) {
            double angle = i * 2.39996323;
            double radius = Math.sqrt(i / (double) (TAPS - 1));
            double sector = Math.PI / 3;
            double local = angle % sector - sector / 2;
            double hexagon = Math.cos(Math.PI / 6) / Math.cos(local);
            radius *= .78 + .22 * hexagon;
            DX[i] = (float) (radius * Math.cos(angle));
            DY[i] = (float) (radius * Math.sin(angle));
        }
    }

    private PortraitBokeh() {}

    static float smooth(float lo, float hi, float x) {
        float t = Math.max(0, Math.min(1, (x - lo) / (hi - lo)));
        return t * t * (3 - 2 * t);
    }

    static int nearest(int p, int end) { return Math.max(0, Math.min(end - 1, p)); }

    static float sample(float[] data, int w, int h, float x, float y) {
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        int i = (int) x, j = (int) y, ni = Math.min(i + 1, w - 1), nj = Math.min(j + 1, h - 1);
        float u = x - i, v = y - j;
        return (data[j * w + i] * (1 - u) + data[j * w + ni] * u) * (1 - v)
                + (data[nj * w + i] * (1 - u) + data[nj * w + ni] * u) * v;
    }

    static float[] refine(int[] pixels, float[] mask, int w, int h) {
        float[] result = mask.clone();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = y * w + x;
            float center = mask[p];
            if (center < .025f || center > .975f) continue;
            float value = 0, total = 0;
            float luma = LinearRgb.luminance(pixels[p]);
            for (int j = -2; j <= 2; j++) for (int i = -2; i <= 2; i++) {
                int q = nearest(y + j, h) * w + nearest(x + i, w);
                float diff = Math.abs(LinearRgb.luminance(pixels[q]) - luma);
                float weight = 1 / (1 + .65f * (i*i + j*j) + 48 * diff);
                value += mask[q] * weight;
                total += weight;
            }
            result[p] = smooth(.12f, .88f, .3f * center + .7f * value / total);
        }
        return result;
    }

    static int[] render(int[] pixels, float[] mask, int w, int h, float amount) {
        int[] output = new int[pixels.length];
        float[] distance = distance(mask, w, h);
        float[] luma = new float[pixels.length];
        for (int i = 0; i < luma.length; i++) luma[i] = LinearRgb.luminance(pixels[i]);
        float scale = Math.min(w, h);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = y * w + x;
            float radius = amount * Math.min(24, scale * .075f)
                    * (.26f + .74f * smooth(1, scale * .23f + 1, distance[p]));
            if (radius < .65f) {
                output[p] = pixels[p];
                continue;
            }
            float r = 0, g = 0, b = 0, total = 0;
            for (int i = 0; i < TAPS; i++) {
                int sx = nearest(Math.round(x + radius * DX[i]), w);
                int sy = nearest(Math.round(y + radius * DY[i]), h);
                int q = sy * w + sx;
                float bg = 1 - smooth(.06f, .42f, mask[q]);
                if (bg == 0) continue;
                float highlight = 1 + .20f * smooth(.70f, .98f, luma[q]);
                int color = pixels[q];
                r += LinearRgb.get(color, 16) * bg * highlight;
                g += LinearRgb.get(color, 8) * bg * highlight;
                b += LinearRgb.get(color, 0) * bg * highlight;
                total += bg;
            }
            output[p] = total < .3f ? pixels[p] : LinearRgb.rgb(r / total, g / total, b / total);
        }
        return output;
    }

    private static float[] distance(float[] mask, int w, int h) {
        float[] d = new float[mask.length];
        for (int p = 0; p < d.length; p++) d[p] = mask[p] >= .55f ? 0 : w + h;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = y * w + x;
            if (x > 0) d[p] = Math.min(d[p], d[p - 1] + 1);
            if (y > 0) {
                d[p] = Math.min(d[p], d[p - w] + 1);
                if (x > 0) d[p] = Math.min(d[p], d[p - w - 1] + 1.414f);
                if (x + 1 < w) d[p] = Math.min(d[p], d[p - w + 1] + 1.414f);
            }
        }
        for (int y = h - 1; y >= 0; y--) for (int x = w - 1; x >= 0; x--) {
            int p = y * w + x;
            if (x + 1 < w) d[p] = Math.min(d[p], d[p + 1] + 1);
            if (y + 1 < h) {
                d[p] = Math.min(d[p], d[p + w] + 1);
                if (x > 0) d[p] = Math.min(d[p], d[p + w - 1] + 1.414f);
                if (x + 1 < w) d[p] = Math.min(d[p], d[p + w + 1] + 1.414f);
            }
        }
        return d;
    }
}
