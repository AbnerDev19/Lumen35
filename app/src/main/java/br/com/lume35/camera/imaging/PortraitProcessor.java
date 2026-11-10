package br.com.lume35.camera.imaging;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public final class PortraitProcessor {
    public static final class Result {
        public final Bitmap bitmap;
        public final boolean applied;
        Result(Bitmap bitmap, boolean applied) {
            this.bitmap = bitmap;
            this.applied = applied;
        }
    }

    private static final int MODEL_INPUT = 512;

    public static Result apply(Bitmap source, float amount) {
        if (!(amount > 0)) return new Result(source, false);
        Segmenter segmenter = null;
        Bitmap input = null, reduced = null, background = null, output = null;
        try {
            int width = source.getWidth(), height = source.getHeight();
            Paint sampling = new Paint(Paint.FILTER_BITMAP_FLAG);
            float fit = MODEL_INPUT / (float) Math.max(width, height);
            float modelW = width * fit, modelH = height * fit;
            float left = (MODEL_INPUT - modelW) / 2, top = (MODEL_INPUT - modelH) / 2;
            input = Bitmap.createBitmap(MODEL_INPUT, MODEL_INPUT, Bitmap.Config.ARGB_8888);
            Canvas model = new Canvas(input);
            model.drawColor(0xff777777);
            model.drawBitmap(source, null, new android.graphics.RectF(left, top, left + modelW, top + modelH), sampling);
            segmenter = Segmentation.getClient(new SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build());
            SegmentationMask segmentation = Tasks.await(
                    segmenter.process(InputImage.fromBitmap(input, 0)), 8, TimeUnit.SECONDS);
            ByteBuffer buffer = segmentation.getBuffer();
            buffer.rewind();
            int mw = segmentation.getWidth(), mh = segmentation.getHeight();
            float[] confidence = new float[mw * mh];
            for (int i = 0; i < confidence.length; i++) confidence[i] = buffer.getFloat();

            float scale = Math.min(1, 400f / Math.max(width, height));
            int w = Math.max(1, Math.round(width * scale)), h = Math.max(1, Math.round(height * scale));
            reduced = Bitmap.createScaledBitmap(source, w, h, true);
            int[] pixels = new int[w * h];
            reduced.getPixels(pixels, 0, w, 0, 0, w, h);
            float[] mask = new float[pixels.length];
            int people = 0, scenery = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                float u = (left + (x + .5f) / w * modelW) * mw / MODEL_INPUT - .5f;
                float v = (top + (y + .5f) / h * modelH) * mh / MODEL_INPUT - .5f;
                float confidenceAtPixel = PortraitBokeh.sample(confidence, mw, mh, u, v);
                if (confidenceAtPixel > .8f) people++;
                if (confidenceAtPixel < .12f) scenery++;
                mask[y * w + x] = PortraitBokeh.smooth(.14f, .86f, confidenceAtPixel);
            }
            if (people < mask.length * .015f || scenery < mask.length * .015f) return new Result(source, false);
            mask = PortraitBokeh.refine(pixels, mask, w, h);
            int[] blurred = PortraitBokeh.render(pixels, mask, w, h, Math.min(1, amount));
            background = Bitmap.createBitmap(blurred, w, h, Bitmap.Config.ARGB_8888);
            output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            new Canvas(output).drawBitmap(background, null, new Rect(0, 0, width, height), sampling);
            int[] fg = new int[width], bg = new int[width];
            for (int y = 0; y < height; y++) {
                source.getPixels(fg, 0, width, 0, y, width, 1);
                output.getPixels(bg, 0, width, 0, y, width, 1);
                float v = (y + .5f) * h / height - .5f;
                for (int x = 0; x < width; x++) {
                    float u = (x + .5f) * w / width - .5f;
                    bg[x] = LinearRgb.mix(bg[x], fg[x], PortraitBokeh.sample(mask, w, h, u, v));
                }
                output.setPixels(bg, 0, width, 0, y, width, 1);
            }
            Bitmap result = output;
            output = null;
            return new Result(result, true);
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            android.util.Log.w("Lume35", "Portrait unavailable", error);
            return new Result(source, false);
        } finally {
            if (segmenter != null) segmenter.close();
            if (input != null) input.recycle();
            if (reduced != null && reduced != source) reduced.recycle();
            if (background != null) background.recycle();
            if (output != null) output.recycle();
        }
    }
}
