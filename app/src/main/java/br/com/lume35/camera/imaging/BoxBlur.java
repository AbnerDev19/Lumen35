package br.com.lume35.camera.imaging;

public final class BoxBlur {
    private BoxBlur() {}

    public static void blurLines(int[] input, int[] output, int lines, int length,
                                  int lineStride, int pixelStride, int radius) {
        int window = radius * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int base = line * lineStride;
            int alpha = 0, red = 0, green = 0, blue = 0;
            for (int delta = -radius; delta <= radius; delta++) {
                int pixel = input[base + clamp(delta, length) * pixelStride];
                alpha += (pixel >>> 24);
                red += ((pixel >>> 16) & 255);
                green += ((pixel >>> 8) & 255);
                blue += (pixel & 255);
            }
            for (int position = 0; position < length; position++) {
                output[base + position * pixelStride] =
                        ((alpha / window) << 24) | ((red / window) << 16) | ((green / window) << 8) | (blue / window);
                int leaving = input[base + clamp(position - radius, length) * pixelStride];
                int entering = input[base + clamp(position + radius + 1, length) * pixelStride];
                alpha += (entering >>> 24) - (leaving >>> 24);
                red += ((entering >>> 16) & 255) - ((leaving >>> 16) & 255);
                green += ((entering >>> 8) & 255) - ((leaving >>> 8) & 255);
                blue += (entering & 255) - (leaving & 255);
            }
        }
    }

    private static int clamp(int position, int length) {
        return Math.max(0, Math.min(length - 1, position));
    }
}
