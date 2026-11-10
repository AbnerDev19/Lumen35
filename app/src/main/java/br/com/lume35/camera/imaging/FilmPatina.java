package br.com.lume35.camera.imaging;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import java.util.Random;

/** Print defects restricted to the photographed area, after development. */
public final class FilmPatina {
    private FilmPatina() {}

    public static void draw(Canvas canvas, int w, int h, int dust, int scratches, int edge, long seed) {
        if (dust == 0 && scratches == 0 && edge == 0) return;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random(seed ^ 0x35ca7eL);
        float unit = Math.min(w, h) / 1000f;
        canvas.save();
        canvas.clipRect(0, 0, w, h);
        if (edge > 0) {
            boolean right = random.nextBoolean();
            float x = right ? w : 0;
            int tint = (Math.min(100, edge) * 62 / 100 << 24) | 0xff7437;
            paint.setShader(new LinearGradient(x, 0, x + (right ? -1 : 1) * w * .11f, 0,
                    tint, 0x00ff7437, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
        }
        for (int i = 0; i < dust * 35 / 100; i++) {
            float x = random.nextFloat() * w, y = random.nextFloat() * h;
            float r = (.45f + 1.3f * random.nextFloat()) * unit;
            paint.setColor(random.nextBoolean() ? 0x55fff3db : 0x55201b18);
            canvas.drawOval(x - r, y - r * .6f, x + r, y + r * .6f, paint);
        }
        for (int i = 0; i < (scratches + 24) / 25; i++) {
            float x = random.nextFloat() * w, y = random.nextFloat() * h;
            paint.setColor(0xfff6e9d2);
            paint.setAlpha(Math.round(scratches * (.22f + .37f * random.nextFloat())));
            paint.setStrokeWidth(Math.max(.5f, .7f * unit));
            canvas.drawLine(x, y, x + (random.nextFloat() - .5f) * w * .016f,
                    y + h * (.08f + .24f * random.nextFloat()), paint);
        }
        canvas.restore();
    }

}
