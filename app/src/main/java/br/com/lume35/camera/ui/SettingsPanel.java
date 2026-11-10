package br.com.lume35.camera.ui;

import br.com.lume35.camera.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public final class SettingsPanel {
    private final Activity activity;
    private final SharedPreferences prefs;
    private final LinearLayout controls;

    private SettingsPanel(Activity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
        controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.VERTICAL);
        int spacing = Math.round(activity.getResources().getDisplayMetrics().density * 20);
        controls.setPadding(spacing, 0, spacing, spacing);
    }

    public static void show(Activity activity, SharedPreferences prefs, Runnable pickOverlay, Runnable update) {
        SettingsPanel panel = new SettingsPanel(activity, prefs);
        panel.show(pickOverlay, update);
    }

    private void show(Runnable pickOverlay, Runnable update) {
        SeekBar quality = new SeekBar(activity);
        TextView jpeg = heading(activity.getString(R.string.jpeg_quality_number, prefs.getInt("jpeg", 94)));
        quality.setMax(5);
        quality.setProgress(prefs.getInt("jpeg", 94) - 90);
        quality.setContentDescription(activity.getString(R.string.ui_qualidade_jpeg));
        controls.addView(quality, new LinearLayout.LayoutParams(-1, dp(48)));
        quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                jpeg.setText(activity.getString(R.string.jpeg_quality_number, 90 + value));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        IndustrialChoice size = select(activity.getString(R.string.ui_lado_maior), new String[]{activity.getString(R.string.ui_1600_px), activity.getString(R.string.ui_2560_px), activity.getString(R.string.ui_4000_px)}, prefs.getInt("max_edge_index", 1));
        IndustrialChoice timer = select(activity.getString(R.string.ui_temporizador), new String[]{activity.getString(R.string.ui_desligado), activity.getString(R.string.ui_3_s), activity.getString(R.string.ui_10_s)}, prefs.getInt("timer_index", 0));
        IndustrialChoice date = select(activity.getString(R.string.ui_formato_da_data), new String[]{activity.getString(R.string.ui_dd_mm_aa), activity.getString(R.string.ui_aa_mm_dd)}, prefs.getBoolean("date_year_first", false) ? 1 : 0);
        String[] frames = {activity.getString(R.string.ui_da_camera), activity.getString(R.string.ui_sem_moldura), activity.getString(R.string.ui_fina), activity.getString(R.string.ui_polaroid), activity.getString(R.string.ui_tira_35_mm), activity.getString(R.string.ui_medio_formato_120), activity.getString(R.string.effect_instax)};
        String[] frameKeys = {"", "NONE", "THIN", "POLAROID", "STRIP_35", "MEDIUM", "INSTAX"};
        int currentFrame = java.util.Arrays.asList(frameKeys).indexOf(prefs.getString("print_frame", ""));
        IndustrialChoice frame = select(activity.getString(R.string.ui_moldura_padrao), frames, Math.max(0, currentFrame));
        IndustrialChoice sound = check(activity.getString(R.string.ui_som_de_obturador), "shutter_sound", false);
        IndustrialChoice haptic = check(activity.getString(R.string.ui_vibracao_leve), "haptic", true);
        IndustrialChoice grid = check(activity.getString(R.string.ui_grade), "grid", true);
        IndustrialChoice horizon = check(activity.getString(R.string.ui_nivel_do_horizonte), "horizon", false);
        IndustrialChoice leak = check(activity.getString(R.string.ui_vazamento_de_luz), "light_leak", false);
        IndustrialChoice halo = check(activity.getString(R.string.ui_halo_nas_luzes), "halation", false);
        IndustrialChoice original = check(activity.getString(R.string.ui_guardar_jpeg_original_privado), "keep_original", false);
        IndustrialChoice info = check(activity.getString(R.string.ui_imprimir_iso_e_obturador), "show_info", true);
        IndustrialChoice leakColor = select(activity.getString(R.string.effect_leak_color),
                new String[]{"Laranja", "Âmbar", "Carmim", "Violeta"}, prefs.getInt("effect_leak_color", 0));
        IndustrialChoice halation = select(activity.getString(R.string.effect_halo),
                new String[]{"Da receita", "0%", "25%", "50%", "75%", "100%"},
                Math.max(0, prefs.getInt("effect_halo", -1) / 25 + (prefs.getInt("effect_halo", -1) < 0 ? 0 : 1)));
        SeekBar dust = effect(R.string.effect_dust, "effect_dust", 0);
        SeekBar scratch = effect(R.string.effect_scratch, "effect_scratch", 0);
        SeekBar edge = effect(R.string.effect_edge, "effect_edge", 0);
        SeekBar vignette = effect(R.string.effect_vignette, "effect_vignette", 100);
        SeekBar overlayMix = effect(R.string.effect_overlay_mix, "effect_overlay_mix", 0);
        android.widget.Button overlay = new android.widget.Button(activity);
        overlay.setText(R.string.effect_overlay_select);
        overlay.setMinHeight(dp(48));
        overlay.setOnClickListener(v -> pickOverlay.run());
        controls.addView(overlay);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(controls);
        new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.ui_ajustes))
                .setView(scroll).setNegativeButton(activity.getString(R.string.ui_cancelar), null)
                .setPositiveButton(activity.getString(R.string.ui_aplicar), (dialog, item) -> {
                    SharedPreferences.Editor changes = prefs.edit()
                            .putInt("jpeg", 90 + quality.getProgress())
                            .putInt("max_edge_index", size.getSelectedItemPosition())
                            .putInt("timer_index", timer.getSelectedItemPosition())
                            .putBoolean("date_year_first", date.getSelectedItemPosition() == 1)
                            .putBoolean("shutter_sound", sound.isChecked()).putBoolean("haptic", haptic.isChecked())
                            .putBoolean("grid", grid.isChecked()).putBoolean("horizon", horizon.isChecked())
                            .putBoolean("light_leak", leak.isChecked()).putBoolean("halation", halo.isChecked())
                            .putInt("effect_leak_color", leakColor.getSelectedItemPosition())
                            .putInt("effect_halo", halation.getSelectedItemPosition() == 0 ? -1 : (halation.getSelectedItemPosition() - 1) * 25)
                            .putInt("effect_dust", dust.getProgress()).putInt("effect_scratch", scratch.getProgress())
                            .putInt("effect_edge", edge.getProgress()).putInt("effect_vignette", vignette.getProgress())
                            .putInt("effect_overlay_mix", overlayMix.getProgress())
                            .putBoolean("show_info", info.isChecked()).putBoolean("keep_original", original.isChecked());
                    int selected = frame.getSelectedItemPosition();
                    if (selected == 0) changes.remove("print_frame");
                    else changes.putString("print_frame", frameKeys[selected]);
                    changes.apply();
                    update.run();
                }).show();
    }

    private SeekBar effect(int title, String key, int defaultValue) {
        String label = activity.getString(title);
        SeekBar slider = new SeekBar(activity);
        slider.setMax(100);
        slider.setProgress(prefs.getInt(key, defaultValue));
        TextView value = heading(label + "  " + slider.getProgress() + "%");
        slider.setContentDescription(label);
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(0xffffa454));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(0xffffa454));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText(label + "  " + progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        controls.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
        return slider;
    }

    private TextView heading(String title) {
        TextView text = new TextView(activity);
        text.setText(title);
        text.setTextSize(13);
        text.setPadding(0, dp(9), 0, 0);
        controls.addView(text);
        return text;
    }

    private IndustrialChoice select(String title, String[] values, int current) {
        IndustrialChoice choice = new IndustrialChoice(activity, title, values, current);
        LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(-1, -2);
        slot.bottomMargin = dp(8);
        controls.addView(choice.view, slot);
        return choice;
    }

    private IndustrialChoice check(String title, String key, boolean defaultValue) {
        return select(title, new String[]{activity.getString(R.string.ui_desligado), activity.getString(R.string.industrial_on)},
                prefs.getBoolean(key, defaultValue) ? 1 : 0);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
