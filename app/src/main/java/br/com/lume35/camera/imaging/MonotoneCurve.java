package br.com.lume35.camera.imaging;

final class MonotoneCurve {
    private static final int[] X = {0, 32, 80, 128, 192, 240, 255};

    private MonotoneCurve() {}

    static float[] lookup(int[] y) {
        float[] slope = new float[X.length - 1];
        float[] tangent = new float[X.length];
        float[] result = new float[256];
        for (int i = 0; i < slope.length; i++) {
            slope[i] = (y[i + 1] - y[i]) / (float) (X[i + 1] - X[i]);
        }
        tangent[0] = slope[0];
        tangent[tangent.length - 1] = slope[slope.length - 1];
        for (int i = 1; i < tangent.length - 1; i++) {
            float a = slope[i - 1], b = slope[i];
            tangent[i] = a * b <= 0 ? 0 : 2 * a * b / (a + b);
        }
        for (int i = 0; i < slope.length; i++) {
            int span = X[i + 1] - X[i];
            for (int p = 0; p <= span; p++) {
                float t = p / (float) span, t2 = t * t, t3 = t2 * t;
                float value = (2 * t3 - 3 * t2 + 1) * y[i]
                        + (t3 - 2 * t2 + t) * span * tangent[i]
                        + (3 * t2 - 2 * t3) * y[i + 1]
                        + (t3 - t2) * span * tangent[i + 1];
                result[X[i] + p] = Math.max(y[i], Math.min(y[i + 1], value)) / 255f;
            }
        }
        return result;
    }
}
