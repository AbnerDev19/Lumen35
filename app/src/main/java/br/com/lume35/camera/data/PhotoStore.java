package br.com.lume35.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import br.com.lume35.camera.model.Film;
import br.com.lume35.camera.model.CameraStyle;
import br.com.lume35.camera.model.LensStyle;
import br.com.lume35.camera.imaging.PortraitProcessor;
import br.com.lume35.camera.imaging.DateStamp;
import br.com.lume35.camera.imaging.FilmPipeline;
import br.com.lume35.camera.imaging.PrintFrame;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PhotoStore {
    private static final DateTimeFormatter EXIF_DATE = DateTimeFormatter
            .ofPattern("yyyy:MM:dd HH:mm:ss", Locale.ROOT).withZone(ZoneId.systemDefault());

    public record Effects(int dust, int scratches, int edge, long seed, String overlayUri, int overlayMix) {}

    public record Request(Film film, CameraStyle body, LensStyle lens, float aspect, int portraitStrength,
                   FilmPipeline development, PrintFrame frame, int stampColor, String stamp,
                   String name, boolean showInfo, int quality, int maxEdge, long taken, File originalDirectory, Effects effects, int frameNumber) {}

    public static final class Result {
        public final Uri uri;
        public final boolean portraitApplied;
        public final boolean overlayMissing;
        public final String lastExposure;

        Result(Uri uri, boolean portraitApplied, boolean overlayMissing, String lastExposure) {
            this.uri = uri;
            this.portraitApplied = portraitApplied;
            this.overlayMissing = overlayMissing;
            this.lastExposure = lastExposure;
        }
    }

    private PhotoStore() {}

    public static Result save(Context context, File input, Request capture) throws IOException {
        ExifInterface exif = new ExifInterface(input.getAbsolutePath());
        Bitmap source = ImageDecoder.decodeBitmap(ImageDecoder.createSource(input), (decoder, info, ignored) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            float scale = Math.min(1f, capture.maxEdge / (float) Math.max(width, height));
            decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
        });
        Bitmap output = null;
        boolean portraitApplied = false;
        try {
            if (capture.portraitStrength > 0) {
                PortraitProcessor.Result result = PortraitProcessor.apply(source, capture.portraitStrength / 100f);
                portraitApplied = result.applied;
                if (result.bitmap != source) {
                    source.recycle();
                    source = result.bitmap;
                }
            }
            int width = Math.min(source.getWidth(), Math.round(source.getHeight() * capture.aspect));
            int height = Math.min(source.getHeight(), Math.round(width / capture.aspect));
            int left = (source.getWidth() - width) / 2;
            int top = (source.getHeight() - height) / 2;
            int[] pixels = new int[width * height];
            source.getPixels(pixels, 0, width, left, top, width, height);
            source.recycle();
            source = null;
            boolean overlayMissing = capture.effects.overlayMix > 0 && !blendOverlay(context, pixels, width, height, capture.effects);
            capture.development.apply(pixels, width, height);

            PrintFrame frame = capture.frame;
            int border = frame.side(width), header = frame.top(width);
            int footer = frame.bottom(width, capture.showInfo);
            output = Bitmap.createBitmap(width + border * 2, height + header + footer, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            String exposure = capture.showInfo ? exposure(exif) : null;
            if (frame != PrintFrame.NONE) {
                frame.draw(canvas, output.getWidth(), output.getHeight(), width, height,
                        capture.film.label(context), capture.body.label(context), exposure, capture.frameNumber);
            }
            output.setPixels(pixels, 0, width, border, header, width, height);
            canvas.save();
            canvas.translate(border, header);
            br.com.lume35.camera.imaging.FilmPatina.draw(canvas, width, height,
                    capture.effects.dust, capture.effects.scratches, capture.effects.edge, capture.effects.seed);
            new DateStamp(capture.stampColor).draw(canvas, width, height, capture.stamp);
            if (frame == PrintFrame.NONE && exposure != null) {
                frame.draw(canvas, width, height, width, height, capture.film.label(context), capture.body.label(context), exposure, capture.frameNumber);
            }
            canvas.restore();
            File original = keepOriginal(input, capture);
            try {
                return new Result(publish(context, output, capture, exif), portraitApplied, overlayMissing, lastExposure(exif));
            } catch (IOException | RuntimeException failure) {
                if (original != null) original.delete();
                throw failure;
            }
        } finally {
            if (source != null) source.recycle();
            if (output != null) output.recycle();
        }
    }

    private static boolean blendOverlay(Context context, int[] pixels, int w, int h, Effects effects) {
        if (effects.overlayUri == null) return false;
        Bitmap image = null, aligned = null;
        try {
            image = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),
                    Uri.parse(effects.overlayUri)), (decoder, info, ignored) -> {
                int iw = info.getSize().getWidth(), ih = info.getSize().getHeight();
                float scale = Math.min(1, 1600f / Math.max(iw, ih));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
                decoder.setTargetSize(Math.max(1, Math.round(iw * scale)), Math.max(1, Math.round(ih * scale)));
            });
            aligned = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(aligned);
            canvas.drawColor(android.graphics.Color.BLACK);
            float scale = Math.max(w / (float) image.getWidth(), h / (float) image.getHeight());
            float iw = image.getWidth() * scale, ih = image.getHeight() * scale;
            canvas.drawBitmap(image, null, new android.graphics.RectF((w-iw)/2, (h-ih)/2, (w+iw)/2, (h+ih)/2),
                    new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
            int[] row = new int[w];
            float mix = Math.min(100, effects.overlayMix) / 100f;
            for (int y = 0; y < h; y++) {
                aligned.getPixels(row, 0, w, 0, y, w, 1);
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    pixels[i] = br.com.lume35.camera.imaging.LinearRgb.mix(pixels[i], row[x], mix);
                }
            }
            return true;
        } catch (IOException | SecurityException | IllegalArgumentException error) {
            android.util.Log.w("Lume35", "Second exposure unavailable", error);
            return false;
        } finally {
            if (image != null) image.recycle();
            if (aligned != null) aligned.recycle();
        }
    }

    private static File keepOriginal(File input, Request capture) {
        if (capture.originalDirectory == null) return null;
        File copy = new File(capture.originalDirectory, capture.name + ".jpg");
        try {
            java.nio.file.Files.createDirectories(capture.originalDirectory.toPath());
            java.nio.file.Files.copy(input.toPath(), copy.toPath());
            return copy;
        } catch (IOException | SecurityException error) {
            copy.delete();
            android.util.Log.w("Lume35", "Original JPEG unavailable", error);
            return null;
        }
    }

    private static String lastExposure(ExifInterface exif) {
        double aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0);
        return (aperture > 0 ? String.format(Locale.ROOT, "f/%.1f", aperture) : "f/—") + "  ·  " + exposure(exif);
    }

    private static String exposure(ExifInterface exif) {
        int iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0);
        double seconds = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0);
        String speed = seconds <= 0 ? "—" : seconds < 1 ? "1/" + Math.round(1 / seconds) + " s"
                : String.format(Locale.ROOT, "%.2f s", seconds);
        return "ISO " + (iso > 0 ? iso : "—") + "  ·  " + speed;
    }

    private static Uri publish(Context context, Bitmap bitmap, Request request, ExifInterface input)
            throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, request.name + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lume 35");
        values.put(MediaStore.Images.Media.DATE_TAKEN, request.taken);
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("MediaStore insert failed");

        boolean published = false;
        try {
            try (OutputStream stream = resolver.openOutputStream(uri, "wt")) {
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, request.quality, stream)) {
                    throw new IOException("JPEG encoding failed");
                }
            }
            try (ParcelFileDescriptor fd = resolver.openFileDescriptor(uri, "rw")) {
                if (fd == null) throw new IOException("EXIF descriptor unavailable");
                ExifInterface exif = new ExifInterface(fd.getFileDescriptor());
                String time = EXIF_DATE.format(Instant.ofEpochMilli(request.taken));
                exif.setAttribute(ExifInterface.TAG_DATETIME, time);
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, time);
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_NORMAL));
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, request.film.label(context) + "\n"
                        + request.body.label(context) + "\n" + request.lens.label(context) + "\n" + exposure(input) + "\n" + request.frameNumber);
                for (String tag : new String[]{ExifInterface.TAG_ISO_SPEED_RATINGS, ExifInterface.TAG_EXPOSURE_TIME,
                        ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL}) {
                    String value = input.getAttribute(tag);
                    if (value != null) exif.setAttribute(tag, value);
                }
                exif.saveAttributes();
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(uri, ready, null, null) != 1) throw new IOException("MediaStore publish failed");
            published = true;
            return uri;
        } finally {
            if (!published) resolver.delete(uri, null, null);
        }
    }
}
