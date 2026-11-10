package br.com.lume35.camera.imaging;

public final class LinearRgb {
    private static final float[] DECODE = new float[256];
    private static final int[] ENCODE = new int[4097];
    static {
        for (int i = 0; i < DECODE.length; i++) {
            double x = i / 255.0;
            DECODE[i] = (float) (x <= .04045 ? x / 12.92 : Math.pow((x + .055) / 1.055, 2.4));
        }
        for (int i = 0; i < ENCODE.length; i++) {
            double x = i / 4096.0;
            ENCODE[i] = (int) Math.round(255 * (x <= .0031308 ? 12.92 * x : 1.055 * Math.pow(x, 1 / 2.4) - .055));
        }
    }

    private LinearRgb() {}
    public static float get(int color, int shift) { return DECODE[(color >>> shift) & 255]; }
    public static float luminance(int color) { return .2126f * get(color, 16) + .7152f * get(color, 8) + .0722f * get(color, 0); }
    private static int encode(float value) { return ENCODE[Math.round(Math.max(0, Math.min(1, value)) * 4096)]; }
    public static int rgb(float r, float g, float b) {
        return 0xff000000 | (encode(r) << 16) | (encode(g) << 8) | encode(b);
    }
    public static int mix(int a, int b, float amount) {
        if (amount <= 0) return a;
        if (amount >= 1) return b;
        float keep = 1 - amount;
        return rgb(keep * get(a, 16) + amount * get(b, 16), keep * get(a, 8) + amount * get(b, 8),
                keep * get(a, 0) + amount * get(b, 0));
    }
}
