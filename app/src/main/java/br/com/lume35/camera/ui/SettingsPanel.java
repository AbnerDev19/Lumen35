package br.com.lume35.camera.ui;

import br.com.lume35.camera.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Painel de ajustes: seções agrupadas, interruptores e controles deslizantes no visual escuro do app. */
public final class SettingsPanel {
    private static final int SHEET = 0xff202124;
    private static final int CARD = 0xff17181a;
    private static final int LINE = 0xff3a3d42;
    private static final int PAPER = 0xfff3eddf;
    private static final int MUTED = 0xffb9ae9b;
    private static final int AMBER = 0xffffa454;
    private static final int AMBER_DIM = 0xff6b4a2c;

    private final Activity activity;
    private final SharedPreferences prefs;
    private LinearLayout page;
    private LinearLayout group;

    private SettingsPanel(Activity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
    }

    public static void show(Activity activity, SharedPreferences prefs, Runnable pickOverlay, Runnable update) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        new SettingsPanel(activity, prefs).show(pickOverlay, update);
    }

    private interface Toggle { boolean on(); }

    private void show(Runnable pickOverlay, Runnable update) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(SHEET);
        sheet.setStroke(dp(1), LINE);
        sheet.setCornerRadii(new float[]{dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0});
        root.setBackground(sheet);
        root.setPadding(dp(18), dp(16), dp(18), dp(12));

        TextView title = text(activity.getString(R.string.ui_ajustes), 26, PAPER);
        title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        root.addView(title);
        TextView subtitle = text(activity.getString(R.string.settings_subtitle), 13, MUTED);
        subtitle.setPadding(0, dp(2), 0, dp(6));
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // Captura
        section(activity.getString(R.string.settings_capture));
        SeekBar quality = jpegSlider();
        IndustrialChoice size = choice(activity.getString(R.string.ui_lado_maior),
                new String[]{activity.getString(R.string.ui_1600_px), activity.getString(R.string.ui_2560_px), activity.getString(R.string.ui_4000_px)},
                prefs.getInt("max_edge_index", 1));
        IndustrialChoice timer = choice(activity.getString(R.string.ui_temporizador),
                new String[]{activity.getString(R.string.ui_desligado), activity.getString(R.string.ui_3_s), activity.getString(R.string.ui_10_s)},
                prefs.getInt("timer_index", 0));
        Toggle sound = toggle(activity.getString(R.string.ui_som_de_obturador), "shutter_sound", false);
        Toggle haptic = toggle(activity.getString(R.string.ui_vibracao_leve), "haptic", true);
        Toggle original = toggle(activity.getString(R.string.ui_guardar_jpeg_original_privado), "keep_original", false);

        // Visor
        section(activity.getString(R.string.settings_viewfinder));
        Toggle grid = toggle(activity.getString(R.string.ui_grade), "grid", true);
        Toggle horizon = toggle(activity.getString(R.string.ui_nivel_do_horizonte), "horizon", false);

        // Foto e moldura
        section(activity.getString(R.string.settings_photo));
        String[] frames = {activity.getString(R.string.ui_da_camera), activity.getString(R.string.ui_sem_moldura), activity.getString(R.string.ui_fina),
                activity.getString(R.string.ui_polaroid), activity.getString(R.string.ui_tira_35_mm), activity.getString(R.string.ui_medio_formato_120),
                activity.getString(R.string.effect_instax)};
        String[] frameKeys = {"", "NONE", "THIN", "POLAROID", "STRIP_35", "MEDIUM", "INSTAX"};
        int currentFrame = java.util.Arrays.asList(frameKeys).indexOf(prefs.getString("print_frame", ""));
        IndustrialChoice frame = choice(activity.getString(R.string.ui_moldura_padrao), frames, Math.max(0, currentFrame));
        IndustrialChoice date = choice(activity.getString(R.string.ui_formato_da_data),
                new String[]{activity.getString(R.string.ui_dd_mm_aa), activity.getString(R.string.ui_aa_mm_dd)},
                prefs.getBoolean("date_year_first", false) ? 1 : 0);
        Toggle info = toggle(activity.getString(R.string.ui_imprimir_iso_e_obturador), "show_info", true);

        // Efeitos de filme
        section(activity.getString(R.string.settings_film));
        Toggle leak = toggle(activity.getString(R.string.ui_vazamento_de_luz), "light_leak", false);
        IndustrialChoice leakColor = choice(activity.getString(R.string.effect_leak_color),
                new String[]{"Laranja", "Âmbar", "Carmim", "Violeta"}, prefs.getInt("effect_leak_color", 0));
        Toggle halo = toggle(activity.getString(R.string.ui_halo_nas_luzes), "halation", false);
        IndustrialChoice halation = choice(activity.getString(R.string.effect_halo),
                new String[]{"Da receita", "0%", "25%", "50%", "75%", "100%"},
                Math.max(0, prefs.getInt("effect_halo", -1) / 25 + (prefs.getInt("effect_halo", -1) < 0 ? 0 : 1)));
        SeekBar dust = slider(R.string.effect_dust, "effect_dust", 0);
        SeekBar scratch = slider(R.string.effect_scratch, "effect_scratch", 0);
        SeekBar edge = slider(R.string.effect_edge, "effect_edge", 0);
        SeekBar vignette = slider(R.string.effect_vignette, "effect_vignette", 100);
        SeekBar overlayMix = slider(R.string.effect_overlay_mix, "effect_overlay_mix", 0);
        TextView overlay = text(activity.getString(R.string.effect_overlay_select), 15, AMBER);
        overlay.setGravity(Gravity.CENTER_VERTICAL);
        overlay.setPadding(dp(16), 0, dp(16), 0);
        overlay.setBackground(ripple(0x00000000, 0));
        overlay.setOnClickListener(v -> pickOverlay.run());
        group.addView(overlay, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout footer = new LinearLayout(activity);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(12), 0, dp(4));
        TextView cancel = actionButton(activity.getString(R.string.ui_cancelar), false);
        TextView apply = actionButton(activity.getString(R.string.ui_aplicar), true);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(50), 1);
        left.rightMargin = dp(10);
        footer.addView(cancel, left);
        footer.addView(apply, new LinearLayout.LayoutParams(0, dp(50), 1));
        root.addView(footer);
        cancel.setOnClickListener(v -> dialog.dismiss());
        apply.setOnClickListener(v -> {
            SharedPreferences.Editor changes = prefs.edit()
                    .putInt("jpeg", 90 + quality.getProgress())
                    .putInt("max_edge_index", size.getSelectedItemPosition())
                    .putInt("timer_index", timer.getSelectedItemPosition())
                    .putBoolean("date_year_first", date.getSelectedItemPosition() == 1)
                    .putBoolean("shutter_sound", sound.on()).putBoolean("haptic", haptic.on())
                    .putBoolean("grid", grid.on()).putBoolean("horizon", horizon.on())
                    .putBoolean("light_leak", leak.on()).putBoolean("halation", halo.on())
                    .putInt("effect_leak_color", leakColor.getSelectedItemPosition())
                    .putInt("effect_halo", halation.getSelectedItemPosition() == 0 ? -1 : (halation.getSelectedItemPosition() - 1) * 25)
                    .putInt("effect_dust", dust.getProgress()).putInt("effect_scratch", scratch.getProgress())
                    .putInt("effect_edge", edge.getProgress()).putInt("effect_vignette", vignette.getProgress())
                    .putInt("effect_overlay_mix", overlayMix.getProgress())
                    .putBoolean("show_info", info.on()).putBoolean("keep_original", original.on());
            int selected = frame.getSelectedItemPosition();
            if (selected == 0) changes.remove("print_frame");
            else changes.putString("print_frame", frameKeys[selected]);
            changes.apply();
            dialog.dismiss();
            update.run();
        });

        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(.65f);
            window.setLayout(-1, Math.round(activity.getResources().getDisplayMetrics().heightPixels * .90f));
        }
    }

    // ---- construção ----

    private void section(String name) {
        TextView label = text(name, 12, AMBER);
        label.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        label.setLetterSpacing(.12f);
        label.setPadding(dp(4), dp(18), 0, dp(8));
        page.addView(label);
        group = new LinearLayout(activity);
        group.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable card = new GradientDrawable();
        card.setColor(CARD);
        card.setCornerRadius(dp(14));
        card.setStroke(dp(1), LINE);
        group.setBackground(card);
        group.setDividerDrawable(divider());
        group.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        page.addView(group, new LinearLayout.LayoutParams(-1, -2));
    }

    private IndustrialChoice choice(String label, String[] values, int current) {
        IndustrialChoice choice = new IndustrialChoice(activity, label, values, current);
        choice.view.setBackground(ripple(0x00000000, 0));
        choice.view.setMinimumHeight(dp(52));
        choice.view.setPadding(dp(16), dp(10), dp(16), dp(10));
        TextView value = choice.view.findViewById(R.id.industrial_value);
        value.setTextColor(AMBER);
        value.setTextSize(14);
        value.setGravity(Gravity.END);
        TextView name = choice.view.findViewById(R.id.industrial_label);
        name.setTextColor(PAPER);
        name.setTextSize(15);
        group.addView(choice.view, new LinearLayout.LayoutParams(-1, -2));
        return choice;
    }

    private Toggle toggle(String label, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(6), dp(12), dp(6));
        row.setMinimumHeight(dp(52));
        row.setBackground(ripple(0x00000000, 0));
        TextView name = text(label, 15, PAPER);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(activity);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states, new int[]{AMBER, 0xff9a9a96}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{AMBER_DIM, 0xff3a3d42}));
        toggle.setContentDescription(label);
        row.addView(toggle);
        row.setOnClickListener(v -> toggle.toggle());
        group.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return toggle::isChecked;
    }

    private SeekBar jpegSlider() {
        int current = prefs.getInt("jpeg", 94);
        LinearLayout box = sliderBox();
        TextView label = text(activity.getString(R.string.ui_qualidade_jpeg), 15, PAPER);
        TextView value = text(activity.getString(R.string.jpeg_quality_number, current), 14, AMBER);
        value.setTypeface(Typeface.MONOSPACE);
        box.addView(header(label, value));
        SeekBar bar = new SeekBar(activity);
        bar.setMax(5);
        bar.setProgress(current - 90);
        bar.setContentDescription(activity.getString(R.string.ui_qualidade_jpeg));
        tint(bar);
        bar.setOnSeekBarChangeListener(new Listener(progress ->
                value.setText(activity.getString(R.string.jpeg_quality_number, 90 + progress))));
        box.addView(bar, new LinearLayout.LayoutParams(-1, dp(40)));
        group.addView(box, new LinearLayout.LayoutParams(-1, -2));
        return bar;
    }

    private SeekBar slider(int title, String key, int defaultValue) {
        String name = activity.getString(title);
        LinearLayout box = sliderBox();
        TextView label = text(name, 15, PAPER);
        TextView value = text(prefs.getInt(key, defaultValue) + "%", 14, AMBER);
        value.setTypeface(Typeface.MONOSPACE);
        box.addView(header(label, value));
        SeekBar bar = new SeekBar(activity);
        bar.setMax(100);
        bar.setProgress(prefs.getInt(key, defaultValue));
        bar.setContentDescription(name);
        tint(bar);
        bar.setOnSeekBarChangeListener(new Listener(progress -> value.setText(progress + "%")));
        box.addView(bar, new LinearLayout.LayoutParams(-1, dp(40)));
        group.addView(box, new LinearLayout.LayoutParams(-1, -2));
        return bar;
    }

    private LinearLayout sliderBox() {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(16), dp(4));
        return box;
    }

    private LinearLayout header(TextView label, TextView value) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(value);
        return row;
    }

    private void tint(SeekBar bar) {
        bar.setProgressTintList(ColorStateList.valueOf(AMBER));
        bar.setThumbTintList(ColorStateList.valueOf(AMBER));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xff4a4d52));
    }

    private TextView actionButton(String label, boolean primary) {
        TextView button = text(label, 16, primary ? 0xff1b1206 : PAPER);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? AMBER : 0x00000000);
        shape.setCornerRadius(dp(14));
        if (!primary) shape.setStroke(dp(1), LINE);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff), shape, null));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private GradientDrawable divider() {
        GradientDrawable line = new GradientDrawable();
        line.setColor(LINE);
        line.setSize(1, dp(1));
        return line;
    }

    private RippleDrawable ripple(int fill, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        return new RippleDrawable(ColorStateList.valueOf(0x22ffffff), shape, null);
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private interface Change { void run(int progress); }

    private static final class Listener implements SeekBar.OnSeekBarChangeListener {
        private final Change change;
        Listener(Change change) { this.change = change; }
        @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { change.run(progress); }
        @Override public void onStartTrackingTouch(SeekBar bar) {}
        @Override public void onStopTrackingTouch(SeekBar bar) {}
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
