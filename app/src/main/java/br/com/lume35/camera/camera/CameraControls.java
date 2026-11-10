package br.com.lume35.camera.camera;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;

public final class CameraControls {
    private CameraControls() {}

    public static void configure(LifecycleCameraController camera) {
        camera.setImageCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);
        camera.setTapToFocusEnabled(true);
        camera.setPinchToZoomEnabled(true);
        camera.setImageCaptureResolutionSelector(new ResolutionSelector.Builder()
                .setResolutionFilter((sizes, rotation) -> {
                    var supported = new java.util.ArrayList<android.util.Size>();
                    for (var size : sizes) {
                        if ((long) size.getWidth() * size.getHeight() <= 12_600_000) supported.add(size);
                    }
                    return supported.isEmpty() ? sizes : supported;
                }).build());
    }

    public static void installGestures(PreviewView preview, FrameLayout finder,
                                       Runnable original, Runnable developed) {
        int size = Math.round(49 * preview.getResources().getDisplayMetrics().density);
        View ring = new View(preview.getContext());
        GradientDrawable outline = new GradientDrawable();
        outline.setShape(GradientDrawable.OVAL);
        outline.setColor(Color.TRANSPARENT);
        outline.setStroke(Math.max(1, size / 28), 0xffe1aa71);
        ring.setBackground(outline);
        ring.setAlpha(0);
        finder.addView(ring, new FrameLayout.LayoutParams(size, size));
        GestureDetector detector = new GestureDetector(preview.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent event) { return true; }
                    @Override public void onLongPress(MotionEvent event) { original.run(); }
                    @Override public boolean onSingleTapUp(MotionEvent event) {
                        ring.animate().cancel();
                        ring.setTranslationX(event.getX() - size / 2f);
                        ring.setTranslationY(event.getY() - size / 2f);
                        ring.setScaleX(1.3f);
                        ring.setScaleY(1.3f);
                        ring.setAlpha(1);
                        ring.animate().scaleX(1).scaleY(1).setDuration(160).withEndAction(
                                () -> ring.animate().alpha(0).setDuration(420).start()).start();
                        return false;
                    }
                });
        preview.setOnTouchListener((view, event) -> {
            detector.onTouchEvent(event);
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_POINTER_DOWN) developed.run();
            return false;
        });
    }
}
