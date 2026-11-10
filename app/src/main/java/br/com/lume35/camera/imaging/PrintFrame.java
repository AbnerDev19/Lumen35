package br.com.lume35.camera.imaging;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.Random;

public enum PrintFrame {
    NONE, THIN, POLAROID, STRIP_35, MEDIUM, INSTAX;

    public int side(int width) {
        return Math.round(width * switch (this) {
            case NONE -> 0;
            case THIN -> .025f;
            case POLAROID -> .06f;
            case INSTAX -> .054f;
            case STRIP_35 -> .04f;
            case MEDIUM -> .067f;
        });
    }

    public int top(int width) {
        return this == STRIP_35 ? Math.round(width * .075f) : side(width);
    }

    public int bottom(int width, boolean showInfo) {
        int extra = showInfo ? Math.round(width * .085f) : 0;
        return switch (this) {
            case NONE -> 0;
            case POLAROID -> Math.round(width * .13f) + extra;
            case INSTAX -> Math.round(width * .195f) + extra;
            default -> top(width) + extra;
        };
    }

    public void draw(Canvas canvas, int totalWidth, int totalHeight, int width, int height,
                     String film, String camera, String exposure, int frameNumber) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (this == NONE) {
            if (exposure != null) {
                paint.setColor(0xd9fff0da);
                paint.setTextSize(width * .021f);
                paint.setTypeface(Typeface.MONOSPACE);
                paint.setShadowLayer(width * .002f, 0, 1, 0xff1c120d);
                canvas.drawText(exposure, width * .044f, height * .954f, paint);
            }
            return;
        }
        boolean dark = this == STRIP_35 || this == MEDIUM;
        int base = dark ? 0xff191713 : 0xfff4efe3;
        canvas.drawColor(base);
        Random texture = new Random(35);
        float step = Math.max(1f, width / 900f);
        paint.setColor(dark ? 0x124d4538 : 0x114b3723);
        for (int i = 0; i < 9500; i++) {
            float x = texture.nextFloat() * totalWidth, y = texture.nextFloat() * totalHeight;
            if (x < side(width) || x >= side(width) + width || y < top(width) || y >= top(width) + height) {
                canvas.drawRect(x, y, x + step, y + step, paint);
            }
        }
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setColor(dark ? 0xffd39857 : 0xff4e453a);
        paint.setTextSize(width * .021f);
        paint.setTextAlign(Paint.Align.CENTER);
        float imageBottom = top(width) + height;
        if (this == STRIP_35) {
            paint.setColor(0xffc8b99c);
            float gap = totalWidth / 19f;
            for (int i = 0; i < 19; i++) {
                float x = gap * (i + .22f), h = top(width) * .22f;
                canvas.drawRoundRect(x, h, x + gap * .53f, h * 2.35f, h * .21f, h * .21f, paint);
                canvas.drawRoundRect(x, imageBottom + h * 2, x + gap * .53f, imageBottom + h * 3.25f, h * .21f, h * .21f, paint);
            }
            paint.setColor(0xffd39857);
            canvas.drawText(film.toUpperCase(java.util.Locale.ROOT) + String.format(java.util.Locale.ROOT, "  ▸  %02dA", frameNumber), totalWidth / 2f,
                    top(width) * .87f, paint);
        } else if (this == MEDIUM) {
            canvas.save();
            canvas.rotate(-90, side(width) * .67f, totalHeight * .5f);
            canvas.drawText(film.toUpperCase(java.util.Locale.ROOT) + "  /  120", side(width) * .67f, totalHeight * .5f, paint);
            canvas.restore();
        } else if (this == POLAROID || this == INSTAX) {
            paint.setTextSize(width * .027f);
            canvas.drawText(film + "  ·  " + camera, totalWidth / 2f, imageBottom + width * .062f, paint);
        }
        if (exposure != null) {
            paint.setTextSize(width * .021f);
            canvas.drawText(exposure, totalWidth / 2f, totalHeight - width * .024f, paint);
        }
    }
}
