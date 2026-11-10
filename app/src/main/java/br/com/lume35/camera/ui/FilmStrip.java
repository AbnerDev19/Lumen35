package br.com.lume35.camera.ui;

import br.com.lume35.camera.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import br.com.lume35.camera.model.Film;

import java.util.EnumMap;
import java.util.function.Consumer;

public final class FilmStrip extends HorizontalScrollView {
    private static final int PAPER = 0xfff7ebd7;
    private static final int PANEL = 0xff25211b;
    private static final int LINE = 0xff494033;
    private final EnumMap<Film, TextView> cards = new EnumMap<>(Film.class);
    private final Runnable snap = this::snapToFilm;
    private final Consumer<Film> onSnap;
    private boolean dragging, settling;

    public FilmStrip(Context context, Runnable openCatalog, Consumer<Film> onTap, Consumer<Film> onSnap) {
        super(context);
        this.onSnap = onSnap;
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addView(row);
        TextView catalog = card(context.getString(R.string.ui_symbol_246));
        catalog.setBackground(outline(PANEL, LINE));
        catalog.setContentDescription(context.getString(R.string.ui_abrir_catalogo_de_filmes));
        catalog.setOnClickListener(v -> openCatalog.run());
        row.addView(catalog, new LinearLayout.LayoutParams(dp(48), dp(58)));
        for (Film film : Film.values()) {
            TextView button = card(film.label(context));
            button.setPadding(dp(8), 0, dp(8), 0);
            button.setCompoundDrawablesWithIntrinsicBounds(null,
                    VisualPicker.icon(context, film.asset, film.swatch), null, null);
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(dp(116), dp(60));
            slot.leftMargin = dp(6);
            row.addView(button, slot);
            cards.put(film, button);
            button.setOnClickListener(v -> onTap.accept(film));
        }
        post(() -> {
            int edge = Math.max(0, (getWidth() - dp(116)) / 2);
            row.setPadding(Math.max(0, edge - dp(54)), 0, edge, 0);
        });
    }

    public void select(Film film) {
        removeCallbacks(snap);
        settling = false;
        for (var entry : cards.entrySet()) {
            boolean active = entry.getKey() == film;
            TextView card = entry.getValue();
            int accent = entry.getKey().swatch;
            card.setTextColor(active ? accent : PAPER);
            card.setSelected(active);
            card.setBackground(outline(active ? 0xff36291e : PANEL, active ? accent : LINE));
        }
        TextView active = cards.get(film);
        post(() -> smoothScrollTo(active.getLeft() - (getWidth() - active.getWidth()) / 2, 0));
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dragging = false;
            settling = false;
            removeCallbacks(snap);
        }
        boolean intercepted = super.onInterceptTouchEvent(event);
        if (intercepted && event.getActionMasked() == MotionEvent.ACTION_MOVE) dragging = true;
        return intercepted;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP && dragging) {
            dragging = false;
            settling = true;
            postDelayed(snap, 170);
        }
        if (action == MotionEvent.ACTION_CANCEL) dragging = false;
        return super.onTouchEvent(event);
    }

    @Override protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
        super.onScrollChanged(left, top, oldLeft, oldTop);
        if (settling) {
            removeCallbacks(snap);
            postDelayed(snap, 140);
        }
    }

    private void snapToFilm() {
        settling = false;
        int center = getScrollX() + getWidth() / 2;
        int distance = Integer.MAX_VALUE;
        Film nearest = null;
        for (var entry : cards.entrySet()) {
            TextView button = entry.getValue();
            int next = Math.abs(button.getLeft() + button.getWidth() / 2 - center);
            if (next < distance) {
                nearest = entry.getKey();
                distance = next;
            }
        }
        if (nearest != null) onSnap.accept(nearest);
    }

    private TextView card(String label) {
        TextView text = VisualPicker.text(getContext(), label, 11, PAPER);
        text.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        text.setFocusable(true);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        return text;
    }

    private RippleDrawable outline(int fill, int stroke) {
        return new RippleDrawable(ColorStateList.valueOf(0x2cffffff),
                VisualPicker.shape(getContext(), fill, stroke), null);
    }

    private int dp(int value) { return VisualPicker.dp(getContext(), value); }
}
