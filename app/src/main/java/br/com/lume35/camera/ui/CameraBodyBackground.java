package br.com.lume35.camera.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public final class CameraBodyBackground extends Drawable {
    private final Paint surface = new Paint();
    private final Paint grain = new Paint();
    private final int top, bottom;

    public CameraBodyBackground(int top, int bottom) {
        this.top = top;
        this.bottom = bottom;
        grain.setColor(0x09d6c6aa);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        if (!bounds.isEmpty()) surface.setShader(new LinearGradient(bounds.left, bounds.top,
                bounds.right * .6f, bounds.bottom, top, bottom, Shader.TileMode.CLAMP));
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        canvas.drawRect(bounds, surface);
        for (int y = bounds.top + 1; y < bounds.bottom; y += 4) {
            canvas.drawLine(bounds.left, y, bounds.right, y, grain);
        }
    }

    @Override public void setAlpha(int alpha) {
        surface.setAlpha(alpha);
        grain.setAlpha(alpha * 9 / 255);
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter filter) {
        surface.setColorFilter(filter);
        grain.setColorFilter(filter);
        invalidateSelf();
    }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
