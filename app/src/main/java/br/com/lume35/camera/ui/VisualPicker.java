package br.com.lume35.camera.ui;

import br.com.lume35.camera.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.LruCache;
import android.view.Gravity;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class VisualPicker {
    private static final LruCache<Integer, Bitmap> CACHE = new LruCache<>(32);

    private VisualPicker() {}

    public record Item(String title, String subtitle, int asset, int color) {}

    @FunctionalInterface
    public interface Selection { void select(int index); }

    public static int dp(Context context, int n) {
        return Math.round(n * context.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context context, String value, int size, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    public static GradientDrawable shape(Context context, int fill, int line) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(context, 12));
        background.setStroke(dp(context, 1), line);
        return background;
    }

    public static void show(Activity activity, String title, String note, List<Item> items,
                            int selected, Selection choose) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 18), dp(activity, 13), dp(activity, 18), dp(activity, 16));
        root.setBackground(shape(activity, 0xff202124, 0xff73767c));

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(activity, title, 26, 0xfff3eddf);
        heading.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = text(activity, activity.getString(R.string.ui_symbol_243), 22, 0xffddd3bd);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription(activity.getString(R.string.ui_fechar_selecao));
        header.addView(close, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        close.setOnClickListener(view -> dialog.dismiss());
        root.addView(header);

        TextView intro = text(activity, note, 13, 0xffc8bba7);
        intro.setPadding(0, dp(activity, 4), 0, dp(activity, 14));
        root.addView(intro);
        ScrollView scroll = new ScrollView(activity);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        for (int i = 0; i < items.size(); i++) {
            int index = i;
            LinearLayout card = itemView(activity, items.get(i), i == selected);
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(-1, -2);
            slot.bottomMargin = dp(activity, 8);
            list.addView(card, slot);
            card.setOnClickListener(view -> {
                dialog.dismiss();
                choose.select(index);
            });
        }

        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(.65f);
            window.setLayout(-1, Math.round(activity.getResources().getDisplayMetrics().heightPixels * .78f));
        }
        if (selected > 0 && selected < items.size()) {
            scroll.post(() -> scroll.scrollTo(0, list.getChildAt(selected).getTop()));
        }
    }

    private static LinearLayout itemView(Context context, Item item, boolean active) {
        LinearLayout card = (LinearLayout) android.view.LayoutInflater.from(context)
                .inflate(R.layout.industrial_catalog_card, null, false);
        ImageView art = card.findViewById(R.id.industrial_art);
        art.setImageDrawable(icon(context, item.asset, item.color));
        TextView name = card.findViewById(R.id.industrial_name);
        name.setText(item.title.toUpperCase(Locale.ROOT));
        TextView detail = card.findViewById(R.id.industrial_detail);
        detail.setText(active ? item.subtitle + " · " + context.getString(R.string.ui_em_uso) : item.subtitle);
        card.setSelected(active);
        card.setContentDescription(item.title + ". " + detail.getText());
        return card;
    }

    public static Drawable icon(Context context, int asset, int color) {
        int width = dp(context, 42), height = dp(context, 36);
        if (asset <= -100) return new CatalogArt(asset, color, width, height);
        Bitmap image = load(context, asset);
        if (image == null) return new CatalogArt(-200, color, width, height);
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            private final RectF target = new RectF();

            @Override public int getIntrinsicWidth() { return width; }
            @Override public int getIntrinsicHeight() { return height; }
            @Override protected void onBoundsChange(android.graphics.Rect bounds) {
                float scale = Math.min(bounds.width() / (float) image.getWidth(), bounds.height() / (float) image.getHeight());
                float w = image.getWidth() * scale / 2, h = image.getHeight() * scale / 2;
                target.set(bounds.exactCenterX() - w, bounds.exactCenterY() - h,
                        bounds.exactCenterX() + w, bounds.exactCenterY() + h);
            }
            @Override public void draw(Canvas canvas) { canvas.drawBitmap(image, null, target, paint); }
            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
            @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }

    private static Bitmap load(Context context, int asset) {
        if (asset < 0) return null;
        Bitmap image = CACHE.get(asset);
        if (image != null) return image;
        String path = String.format(Locale.ROOT, "catalog/asset%02d.webp", asset);
        try (var input = context.getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            image = BitmapFactory.decodeStream(input, null, options);
            if (image != null) CACHE.put(asset, image);
            return image;
        } catch (IOException error) {
            return null;
        }
    }
}
