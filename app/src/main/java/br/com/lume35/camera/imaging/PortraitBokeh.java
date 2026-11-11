package br.com.lume35.camera.imaging;

/**
 * Desfoque de fundo para o modo Retrato. Trabalha numa imagem reduzida (mantendo a proporção) e numa
 * máscara registrada geometricamente.
 *
 * <p>Em vez de espalhar poucas amostras por pixel (que gerava ruído, bandas e halo duro), o fundo é
 * desfocado em quatro níveis com convolução normalizada: a cor do sujeito nunca vaza para o fundo e o
 * raio de cada pixel interpola suavemente entre os níveis. As luzes fortes pesam mais na média, o que
 * produz o brilho arredondado típico de lentes luminosas.
 */
final class PortraitBokeh {
    private static final int LEVELS = 4;

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

    // ---- máscara ----

    /** Ajusta a máscara às bordas reais da imagem com um filtro guiado (guided filter). */
    static float[] refine(int[] pixels, float[] mask, int w, int h) {
        int n = w * h;
        float[] guide = new float[n];
        for (int i = 0; i < n; i++) {
            int c = pixels[i];
            guide[i] = (.299f * ((c >> 16) & 255) + .587f * ((c >> 8) & 255) + .114f * (c & 255)) / 255f;
        }
        int r = Math.max(3, Math.round(Math.max(w, h) / 110f));
        float eps = .0025f;
        float[] meanI = box(guide, w, h, r);
        float[] meanP = box(mask, w, h, r);
        float[] ii = new float[n], ip = new float[n];
        for (int i = 0; i < n; i++) { ii[i] = guide[i] * guide[i]; ip[i] = guide[i] * mask[i]; }
        float[] corrI = box(ii, w, h, r), corrIp = box(ip, w, h, r);
        float[] a = new float[n], b = new float[n];
        for (int i = 0; i < n; i++) {
            float var = corrI[i] - meanI[i] * meanI[i];
            float cov = corrIp[i] - meanI[i] * meanP[i];
            a[i] = cov / (var + eps);
            b[i] = meanP[i] - a[i] * meanI[i];
        }
        float[] meanA = box(a, w, h, r), meanB = box(b, w, h, r);
        float[] result = new float[n];
        for (int i = 0; i < n; i++) {
            float q = meanA[i] * guide[i] + meanB[i];
            // longe da borda, confia na máscara original; perto dela, usa o resultado guiado
            float uncertain = 1 - Math.abs(mask[i] * 2 - 1);
            float mixed = mask[i] + (q - mask[i]) * Math.min(1, uncertain * 3 + .35f);
            result[i] = smooth(.10f, .90f, mixed);
        }
        return result;
    }

    // ---- desfoque ----

    /** Devolve a imagem inteira com o fundo desfocado; o sujeito é recomposto por quem chama. */
    static int[] render(int[] pixels, float[] mask, int w, int h, float amount) {
        int n = w * h;
        float scale = Math.min(w, h);
        float maxRadius = Math.max(1f, amount * scale * .04f);
        if (maxRadius < 1.5f) return pixels.clone();

        float[] weight = new float[n], red = new float[n], green = new float[n], blue = new float[n];
        for (int i = 0; i < n; i++) {
            int c = pixels[i];
            float rr = LinearRgb.get(c, 16), gg = LinearRgb.get(c, 8), bb = LinearRgb.get(c, 0);
            float luma = .2126f * rr + .7152f * gg + .0722f * bb;
            float bg = 1 - smooth(.04f, .45f, mask[i]);
            float wgt = bg * (1 + 2.5f * smooth(.55f, 1f, luma));
            weight[i] = wgt;
            red[i] = rr * wgt;
            green[i] = gg * wgt;
            blue[i] = bb * wgt;
        }

        int[][] level = new int[LEVELS + 1][];
        level[0] = pixels;
        for (int k = 1; k <= LEVELS; k++) {
            int radius = Math.max(1, Math.round(maxRadius * k / LEVELS));
            float[] den = gaussianish(weight, w, h, radius);
            float[] r = gaussianish(red, w, h, radius);
            float[] g = gaussianish(green, w, h, radius);
            float[] b = gaussianish(blue, w, h, radius);
            int[] out = new int[n];
            for (int i = 0; i < n; i++) {
                out[i] = den[i] < .02f ? pixels[i] : LinearRgb.rgb(r[i] / den[i], g[i] / den[i], b[i] / den[i]);
            }
            level[k] = out;
        }

        float[] distance = distance(mask, w, h);
        float ramp = Math.max(2f, scale * .10f);
        int[] output = new int[n];
        for (int i = 0; i < n; i++) {
            // junto ao sujeito o desfoque já é forte; cresce com a distância até o máximo
            float target = .62f + .38f * smooth(0, ramp, distance[i]);
            float position = target * LEVELS;
            int low = Math.min(LEVELS - 1, (int) position);
            output[i] = LinearRgb.mix(level[low][i], level[low + 1][i], position - low);
        }
        return output;
    }

    /** Três passadas de caixa aproximam um desfoque gaussiano. */
    private static float[] gaussianish(float[] src, int w, int h, int radius) {
        float[] out = src;
        for (int pass = 0; pass < 3; pass++) out = boxSum(out, w, h, radius);
        return out;
    }

    private static float[] boxSum(float[] src, int w, int h, int r) {
        float[] tmp = new float[src.length], out = new float[src.length];
        float window = 2 * r + 1;
        for (int y = 0; y < h; y++) {
            int base = y * w;
            float sum = 0;
            for (int d = -r; d <= r; d++) sum += src[base + nearest(d, w)];
            for (int x = 0; x < w; x++) {
                tmp[base + x] = sum / window;
                sum += src[base + nearest(x + r + 1, w)] - src[base + nearest(x - r, w)];
            }
        }
        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int d = -r; d <= r; d++) sum += tmp[nearest(d, h) * w + x];
            for (int y = 0; y < h; y++) {
                out[y * w + x] = sum / window;
                sum += tmp[nearest(y + r + 1, h) * w + x] - tmp[nearest(y - r, h) * w + x];
            }
        }
        return out;
    }

    private static float[] box(float[] src, int w, int h, int r) { return boxSum(src, w, h, r); }

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
