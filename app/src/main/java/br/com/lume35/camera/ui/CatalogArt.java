package br.com.lume35.camera.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

final class CatalogArt extends Drawable {
    private static final int[] GLASS = {0xff699795, 0xffb38470, 0xff79a47c, 0xff526f99, 0xff9c6f96,
            0xffa79969, 0xff83a9b8, 0xffb8844c, 0xffb8a57d};
    private static final String[] ROLLS = {"", "EXP", "800T", "HP5", "APX", "400", "200", "200"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path leader = new Path();
    private final int asset, color, width, height;
    private int opacity = 255;

    CatalogArt(int asset, int color, int width, int height) {
        this.asset = asset;
        this.color = color;
        this.width = width;
        this.height = height;
        paint.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        leader.moveTo(20, 26);
        leader.lineTo(9, 28);
        leader.lineTo(9, 52);
        leader.quadTo(21, 49, 26, 66);
        leader.close();
    }

    @Override public int getIntrinsicWidth() { return width; }
    @Override public int getIntrinsicHeight() { return height; }

    @Override public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 72f, getBounds().height() / 80f);
        if (asset <= -300) body(canvas, -300 - asset);
        else if (asset <= -200) roll(canvas, -200 - asset);
        else lens(canvas, -100 - asset);
        canvas.restore();
    }

    private void body(Canvas canvas, int index) {
        float top = 23 + (index % 3) * 4;
        float bottom = 59 + index % 4 * 3;
        boolean compact = index > 10;
        ink(0xff1f1d1a);
        canvas.drawRoundRect(6, top, 66, bottom, compact ? 9 : 4, compact ? 9 : 4, paint);
        ink(color);
        canvas.drawRoundRect(7, top + 1, 65, top + 12, 4, 4, paint);
        ink(0xff4f4438);
        canvas.drawRoundRect(13 + index % 9, top - 6, compact ? 33 : 43, top + 3, 2, 2, paint);
        ink(0xff2b393a);
        canvas.drawRoundRect(49, top + 3, 60, top + 10, 2, 2, paint);
        float center = 31 + index % 6, radius = compact ? 13 : 16;
        float y = top + (bottom - top) * .56f;
        ink(0xffaca293);
        canvas.drawCircle(center, y, radius, paint);
        ink(0xff302c2d);
        canvas.drawCircle(center, y, radius - 3, paint);
        ink(GLASS[index % GLASS.length]);
        canvas.drawCircle(center, y, radius - 7, paint);
        ink(0xb3efe4c6);
        canvas.drawCircle(center - 3, y - 4, 2, paint);
    }

    private void lens(Canvas canvas, int index) {
        float length = 20 + (index * 7 % 19), rim = 18 + (index * 4 % 8);
        float cy = 42;
        canvas.rotate(index % 2 == 0 ? -17 : 12, 37, cy);
        ink(0xff1e1d1b);
        canvas.drawRoundRect(12, cy - rim, 41 + length * .39f, cy + rim, 9, 9, paint);
        ink(index == 4 || index == 8 ? 0xff9b8d76 : 0xff5e5a53);
        canvas.drawRoundRect(14, cy - rim + 2, 33 + length * .42f, cy + rim - 2, 8, 8, paint);
        ink(0xff32312e);
        for (int i = 0; i < 3 + index % 4; i++) {
            float x = 16 + 3f * i;
            canvas.drawRect(x, cy - rim + 4, x + 1, cy + rim - 4, paint);
        }
        ink(0xffbcb3a2);
        canvas.drawOval(35, cy - rim - 1, 65, cy + rim + 1, paint);
        ink(0xff29292a);
        canvas.drawOval(37, cy - rim + 2, 63, cy + rim - 2, paint);
        ink(GLASS[index]);
        canvas.drawOval(42, cy - rim * .68f, 60, cy + rim * .68f, paint);
        ink(0xff252e36);
        canvas.drawOval(45, cy - rim * .55f, 59, cy + rim * .53f, paint);
        ink(0x75f2efe4);
        canvas.drawOval(46, cy - rim * .45f, 52, cy - rim * .09f, paint);
    }

    private void roll(Canvas canvas, int index) {
        if (index == 0) {
            ink(0xff2d2925);
            canvas.drawRoundRect(12, 16, 61, 63, 5, 5, paint);
            ink(color);
            canvas.drawRect(18, 22, 55, 57, paint);
            ink(0xff211e1b);
            canvas.drawRect(21, 25, 52, 54, paint);
            ink(0xffece2cd);
            canvas.drawCircle(44, 33, 3, paint);
            canvas.drawLine(23, 50, 34, 36, paint);
            canvas.drawLine(34, 36, 48, 50, paint);
            return;
        }
        ink(index == 1 ? 0xff733c35 : 0xff5f4834);
        canvas.drawPath(leader, paint);
        ink(0xffdccbb0);
        canvas.drawRoundRect(11, 31, 14, 34, 1, 1, paint);
        canvas.drawRoundRect(16, 31, 19, 34, 1, 1, paint);
        ink(0xff292829);
        canvas.drawRoundRect(22, 13, 59, 72, 6, 6, paint);
        canvas.drawRoundRect(32, 6, 48, 19, 3, 3, paint);
        ink(color);
        canvas.drawRect(23, 21, 58, 64, paint);
        ink(0x55fff8dc);
        canvas.drawRect(25 + index, 23, 28 + index, 62, paint);
        ink(index % 2 == 0 ? 0xffeddfc8 : 0xff45372a);
        float band = 28 + (index * 7 % 20);
        canvas.drawRect(23, band, 58, band + 10, paint);
        ink(index % 2 == 0 ? 0xff43372a : 0xffffeed7);
        paint.setTextSize(8);
        canvas.drawText(ROLLS[index], 40, band + 8, paint);
    }

    private void ink(int color) {
        paint.setColor(color);
        paint.setAlpha((color >>> 24) * opacity / 255);
    }

    @Override public void setAlpha(int alpha) { opacity = alpha; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
