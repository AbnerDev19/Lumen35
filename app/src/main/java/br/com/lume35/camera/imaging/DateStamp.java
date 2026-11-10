package br.com.lume35.camera.imaging;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public final class DateStamp {
    private static final int[] DIGITS = {63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
    private static final Path[] SEGMENTS = {
            segment(1, 1, false), segment(7, 2, true), segment(7, 9, true),
            segment(1, 15, false), segment(0, 9, true), segment(0, 2, true), segment(1, 8, false)
    };
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DateStamp(int color) {
        light.setColor(color);
        light.setShadowLayer(1.2f, 0, 0, color);
    }

    public void draw(Canvas canvas, int width, int height, String value) {
        if (value == null) return;
        float size = Math.min(width, height) * .0032f;
        canvas.save();
        canvas.translate(width - Math.min(width, height) * .054f - value.length() * 11 * size,
                height - Math.min(width, height) * .089f);
        canvas.scale(size, size);
        for (int i = 0; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digit >= '0' && digit <= '9') {
                int bits = DIGITS[digit - '0'];
                for (int j = 0; j < 7; j++) if ((bits & (1 << j)) != 0) canvas.drawPath(SEGMENTS[j], light);
            } else if (digit == '.') {
                canvas.drawCircle(4, 15.5f, .8f, light);
            }
            canvas.translate(11, 0);
        }
        canvas.restore();
    }

    private static Path segment(float x, float y, boolean vertical) {
        Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(1, -1);
        path.lineTo(5, -1);
        path.lineTo(6, 0);
        path.lineTo(5, 1);
        path.lineTo(1, 1);
        path.close();
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        if (vertical) matrix.setRotate(90);
        matrix.postTranslate(x, y);
        path.transform(matrix);
        return path;
    }
}
