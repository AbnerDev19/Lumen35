package br.com.lume35.camera.ui;

import br.com.lume35.camera.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import java.util.function.IntConsumer;

@android.annotation.SuppressLint("AppCompatCustomView")
final class ZoomImageView extends ImageView {
    private final Matrix transform = new Matrix();
    private final RectF visible = new RectF();
    private final float[] values = new float[9];
    private final ScaleGestureDetector pinch;
    private final GestureDetector taps;
    private float fit = 1;
    private IntConsumer swipe;
    private boolean multiTouch;

    ZoomImageView(Context context) {
        super(context);
        setScaleType(ScaleType.MATRIX);
        setContentDescription(getContext().getString(R.string.ui_foto_pinca_ou_duplo_toque_para_ampliar));
        pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                zoom(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                return true;
            }
        });
        pinch.setQuickScaleEnabled(false);
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onDoubleTap(MotionEvent event) {
                if (isZoomed()) reset();
                else zoom(3, event.getX(), event.getY());
                return true;
            }
            @Override public boolean onSingleTapConfirmed(MotionEvent event) { return performClick(); }
            @Override public boolean onScroll(MotionEvent first, MotionEvent last, float x, float y) {
                if (!multiTouch && isZoomed()) {
                    transform.postTranslate(-x, -y);
                    clamp();
                }
                return true;
            }
            @Override public boolean onFling(MotionEvent first, MotionEvent last, float vx, float vy) {
                if (first == null || swipe == null || isZoomed() || multiTouch) return false;
                float dx = last.getX() - first.getX();
                float dy = last.getY() - first.getY();
                if (Math.abs(dx) < getWidth() * .14f || Math.abs(dx) < Math.abs(dy) * 1.5f) return false;
                swipe.accept(dx < 0 ? 1 : -1);
                return true;
            }
        });
    }

    void setSwipeListener(IntConsumer listener) { swipe = listener; }

    private float scale() {
        transform.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private boolean isZoomed() { return scale() > fit * 1.02f; }

    @Override public void setImageBitmap(Bitmap image) {
        super.setImageBitmap(image);
        reset();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        reset();
    }

    private void reset() {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) return;
        float width = getDrawable().getIntrinsicWidth(), height = getDrawable().getIntrinsicHeight();
        fit = Math.min(getWidth() / width, getHeight() / height);
        transform.setScale(fit, fit);
        transform.postTranslate((getWidth() - width * fit) / 2, (getHeight() - height * fit) / 2);
        setImageMatrix(transform);
    }

    private void zoom(float factor, float x, float y) {
        if (getDrawable() == null) return;
        float current = scale();
        float next = Math.max(fit, Math.min(fit * 6, current * factor));
        transform.postScale(next / current, next / current, x, y);
        clamp();
    }

    private void clamp() {
        if (getDrawable() == null) return;
        visible.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        transform.mapRect(visible);
        transform.postTranslate(correction(visible.left, visible.right, getWidth()),
                correction(visible.top, visible.bottom, getHeight()));
        setImageMatrix(transform);
    }

    private static float correction(float start, float end, int available) {
        float size = end - start;
        if (size <= available) return (available - size) / 2 - start;
        if (start > 0) return -start;
        return Math.max(0, available - end);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (event.getPointerCount() > 1) multiTouch = true;
        pinch.onTouchEvent(event);
        taps.onTouchEvent(event);
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
