package br.com.lume35.camera;

import android.Manifest;
import br.com.lume35.camera.imaging.PrintFrame;
import br.com.lume35.camera.imaging.FilmPipeline;
import br.com.lume35.camera.imaging.FilmPreview;
import br.com.lume35.camera.imaging.DateStamp;
import br.com.lume35.camera.camera.CameraControls;
import br.com.lume35.camera.ui.SettingsPanel;
import br.com.lume35.camera.ui.GalleryActivity;
import br.com.lume35.camera.ui.FilmStrip;
import br.com.lume35.camera.ui.CatalogSelectors;
import br.com.lume35.camera.camera.PhoneLenses;
import br.com.lume35.camera.model.Film;
import br.com.lume35.camera.model.CameraStyle;
import br.com.lume35.camera.model.LensStyle;
import br.com.lume35.camera.data.PhotoStore;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends ComponentActivity {
    private static final int INK = 0xff141414;
    private static final int PANEL = 0xff25211b;
    private static final int LINE = 0xff494033;
    private static final int PAPER = 0xfff7ebd7;
    private static final int MUTED = 0xffc4b49f;
    private static final int AMBER = 0xffe1aa71;
    private static final Typeface MONO = Typeface.create("monospace", Typeface.BOLD);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yy", Locale.ROOT);
    private static final DateTimeFormatter YEAR_FIRST = DateTimeFormatter.ofPattern("yy MM dd", Locale.ROOT);
    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.ROOT);

    private final ExecutorService photos = Executors.newSingleThreadExecutor();

    private final FilmPreview previewEffect =
            new FilmPreview();
    private long filmSeed = java.util.concurrent.ThreadLocalRandom.current().nextLong();
    private boolean compareOriginal;
    private PrintFrame printFrame = PrintFrame.POLAROID;
    private boolean dateYearFirst;
    private int formatOverride;

    private int stampColor() {
        return Color.rgb(255, 125 + Color.green(cameraStyle.accent) / 4,
                51 + Color.blue(cameraStyle.accent) / 3);
    }

    private String formatStamp(LocalDateTime value) {
        return (dateYearFirst ? YEAR_FIRST : DATE).format(value);
    }

    private void choosePrintSettings() {
        String[] choices = {getString(R.string.ui_sem_moldura), getString(R.string.ui_fina),
                getString(R.string.ui_polaroid), getString(R.string.ui_tira_35_mm),
                getString(R.string.ui_medio_formato_120), getString(R.string.effect_instax)};
        new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.ui_moldura))
                .setSingleChoiceItems(choices, printFrame.ordinal(), (dialog, index) -> {
                    printFrame = PrintFrame.values()[index];
                    preferences.edit().putString("print_frame", printFrame.name()).apply();
                    dialog.dismiss();
                })
                .setNeutralButton(getString(R.string.capture_format), (dialog, index) -> chooseFormat())
                .setPositiveButton(getString(R.string.ui_fechar), null).show();
    }

    private float aspect() {
        return switch (formatOverride) {
            case 1 -> .75f;
            case 2 -> 1f;
            case 3 -> 6f / 7;
            default -> cameraStyle.ratio;
        };
    }

    private void chooseFormat() {
        String[] formats = {getString(R.string.ui_da_camera), getString(R.string.ui_3_4),
                getString(R.string.ui_1_1), getString(R.string.ui_6_7)};
        new android.app.AlertDialog.Builder(this).setTitle(R.string.capture_format)
                .setSingleChoiceItems(formats, formatOverride, (dialog, choice) -> {
                    formatOverride = choice;
                    preferences.edit().putInt("format_override", choice).apply();
                    updateProfiles();
                    dialog.dismiss();
                }).setPositiveButton(R.string.ui_fechar, null).show();
    }

    private FilmPipeline development() {
        return new FilmPipeline(selected.recipe(), intensity / 100f,
                cameraStyle.contrast * lensStyle.contrast + cameraStyle.curve,
                (cameraStyle.warmth + lensStyle.warmth) / 255f,
                lensStyle.vignette / 255f + cameraStyle.vignette,
                Math.min(.6f, cameraStyle.softness + lensStyle.blur), lensStyle.aberration,
                lensStyle.oval, cameraStyle.leakChance, preferences.getBoolean("light_leak", false),
                preferences.getBoolean("halation", false), filmSeed, cameraStyle.toe, lensStyle.flare,
                preferences.getInt("effect_leak_color", 0), preferences.getInt("effect_halo", -1),
                preferences.getInt("effect_vignette", 100));
    }

    private int nextFrameNumber;
    private SharedPreferences preferences;
    private PreviewView preview;
    private FrameLayout imageLayer;
    private FinishView finish;
    private View shutterBlink;
    private TextView shutter;
    private TextView flip;
    private TextView flash;
    private TextView date;
    private String lastExposure;
    private TextView roll;
    private TextView strengthLabel;
    private TextView status;
    private TextView permissionButton;
    private FilmStrip filmStrip;
    private LinearLayout intensityRow;
    private int pendingPhotos;
    private boolean countingDown;
    private final android.os.Handler timer = new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.media.MediaActionSound cameraSound = new android.media.MediaActionSound();
    private android.hardware.SensorManager sensors;
    private float horizonAngle;
    private final android.hardware.SensorEventListener horizonSensor = new android.hardware.SensorEventListener() {
        @Override public void onSensorChanged(android.hardware.SensorEvent event) {
            float angle = (float) Math.toDegrees(Math.atan2(event.values[0], event.values[1]));
            horizonAngle += .16f * (angle - horizonAngle);
            finish.invalidate();
        }
        @Override public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
    };
    private TextView galleryButton;
    private FrameLayout finderView;

    private LifecycleCameraController controller;
    private Film selected = Film.GOLD;
    private int intensity = 80;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;
    private int frameNumber;
    private boolean front;
    private boolean hasBothLenses;
    private boolean cameraReady;
    private boolean working;
    private boolean stamp;
    private TextView lensButton;
    private boolean showInfo;
    private CameraStyle cameraStyle=CameraStyle.CLASSIC;
    private LensStyle lensStyle=LensStyle.NEUTRAL;
    private int portraitStrength;
    private TextView bodyButton,modeButton,formatLabel;
    private LinearLayout cameraRoot;
    private FrameLayout cameraStage;
    private Uri lastPhoto;

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera();
                else showPermission();
            });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this, SystemBarStyle.dark(INK), SystemBarStyle.dark(INK));
        preferences = getSharedPreferences("camera", MODE_PRIVATE);
        nextFrameNumber = preferences.getInt("effect_next_frame", preferences.getInt("frame", 0) + 1);
        selected = br.com.lume35.camera.data.CameraPreferences.readFilm(preferences);
        
        dateYearFirst = preferences.getBoolean("date_year_first", false);
        formatOverride = preferences.getInt("format_override", 0);
        intensity = preferences.getInt("intensity", 80);
        stamp = preferences.getBoolean("date", true);
        showInfo = preferences.getBoolean("show_info", true);
        frameNumber = preferences.getInt("frame", 0);
        try{cameraStyle=CameraStyle.valueOf(preferences.getString("body","CLASSIC"));}catch(IllegalArgumentException ignored){}
                printFrame = PrintFrame.valueOf(preferences.getString("print_frame", cameraStyle.defaultFrame.name()));
        portraitStrength = Math.max(0, Math.min(100, preferences.getInt("portrait_strength",
                preferences.getBoolean("portrait", false) ? 30 : 0)));
        String previousPhoto = preferences.getString("photo", null);
        if (previousPhoto != null) lastPhoto = Uri.parse(previousPhoto);

        buildInterface();
        updateFilm();
        updateRoll();
        updateProfiles();
        preview.getPreviewStreamState().observe(this, streamState -> {
            if (streamState == PreviewView.StreamState.STREAMING) {
                status.setVisibility(View.GONE);
                refreshButtons();
            } else if (cameraReady) {
                status.setText(getString(R.string.ui_abrindo_camera));
                status.setVisibility(View.VISIBLE);
                refreshButtons();
            }
        });
        refreshButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            showPermission();
        }
        refreshHorizon();
        if (finish != null) finish.refresh();
    }

    private void refreshHorizon() {
        if (sensors == null) sensors = getSystemService(android.hardware.SensorManager.class);
        if (sensors == null) return;
        sensors.unregisterListener(horizonSensor);
        if (preferences.getBoolean("horizon", false)) {
            var gravity = sensors.getDefaultSensor(android.hardware.Sensor.TYPE_GRAVITY);
            if (gravity != null) sensors.registerListener(horizonSensor, gravity,
                    android.hardware.SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        if (sensors != null) sensors.unregisterListener(horizonSensor);
        timer.removeCallbacksAndMessages(null);
        countingDown = false;
        preferences.edit().putInt("intensity", intensity).apply();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.unbind();
            preview.setController(null);
        }
        cameraSound.release();
        photos.shutdown();
        super.onDestroy();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        cameraRoot=root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(INK);
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            root.setPadding(safe.left + dp(16), safe.top + dp(3), safe.right + dp(16),
                    safe.bottom + dp(6));
            return insets;
        });

        LinearLayout header = row();
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(51)));
        TextView brand = text(getString(R.string.app_name), 23, PAPER, Typeface.create("sans-serif-medium", Typeface.NORMAL));
        brand.setMaxLines(1);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        flash = topButton(header, getString(R.string.ui_flash), getString(R.string.ui_alterar_flash), this::cycleFlash);
        formatLabel = topButton(header, getString(R.string.ui_3_4), getString(R.string.ui_escolher_formato_e_moldura), this::choosePrintSettings);
        date = topButton(header, getString(R.string.ui_data), getString(R.string.ui_ativar_carimbo_de_data), () -> {
            stamp = !stamp;
            preferences.edit().putBoolean("date", stamp).apply();
            finish.refresh();
            refreshButtons();
        });
        topButton(header, getString(R.string.ui_symbol), getString(R.string.ui_abrir_ajustes), this::openSettings);

        FrameLayout finder = new FrameLayout(this);
        finderView = finder;
        finder.setBackground(shape(0xff0d0c0a, 19, LINE));
        finder.setClipToOutline(true);
        FrameLayout stage = new FrameLayout(this) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                int width = Math.min(MeasureSpec.getSize(widthSpec),
                        Math.round(MeasureSpec.getSize(heightSpec) * aspect()));
                FrameLayout.LayoutParams bounds = (FrameLayout.LayoutParams) finder.getLayoutParams();
                bounds.width = width;
                bounds.height = Math.round(width / aspect());
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        cameraStage=stage;
        stage.addView(finder, new FrameLayout.LayoutParams(0, 0, Gravity.CENTER));
        root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1f));

        imageLayer = new FrameLayout(this);
        finder.addView(imageLayer, new FrameLayout.LayoutParams(-1, -1));
        preview = new PreviewView(this);
        preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        imageLayer.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        CameraControls.installGestures(preview, finder, () -> setComparison(true),
                () -> setComparison(false));
        finish = new FinishView();
        finder.addView(finish, new FrameLayout.LayoutParams(-1, -1));

        shutterBlink = new View(this);
        shutterBlink.setBackgroundColor(PAPER);
        shutterBlink.setAlpha(0f);
        finder.addView(shutterBlink, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout message = new LinearLayout(this);
        message.setOrientation(LinearLayout.VERTICAL);
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(12), 0, dp(12), 0);
        finder.addView(message, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        status = text(getString(R.string.ui_abrindo_camera), 13, PAPER, MONO);
        status.setGravity(Gravity.CENTER);
        message.addView(status, new LinearLayout.LayoutParams(-1, -2));
        permissionButton = button(getString(R.string.ui_liberar_camera), getString(R.string.ui_permitir_acesso_a_camera));
        permissionButton.setOnClickListener(view -> askPermission());
        LinearLayout.LayoutParams permissionParams = new LinearLayout.LayoutParams(dp(166), dp(48));
        permissionParams.topMargin = dp(10);
        message.addView(permissionButton, permissionParams);
        permissionButton.setVisibility(View.GONE);

        LinearLayout detail = row();
        root.addView(detail, new LinearLayout.LayoutParams(-1, dp(28)));
        roll = text("", 13, AMBER, MONO);
        roll.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        detail.addView(roll, new LinearLayout.LayoutParams(0, -2, 1));
        TextView exposure = text(getString(R.string.ui_ev), 11, AMBER, MONO);
        exposure.setGravity(Gravity.CENTER);
        exposure.setOnClickListener(v -> chooseExposure());
        exposure.setContentDescription(getString(R.string.ui_compensacao_de_exposicao));
        finder.addView(exposure, new FrameLayout.LayoutParams(dp(60), dp(48), Gravity.TOP | Gravity.END));

        filmStrip = new FilmStrip(this, this::chooseFilm, film -> {
            if (film == selected) {
                intensityRow.setVisibility(intensityRow.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                filmStrip.select(film);
            } else {
                selectFilm(film);
            }
        }, film -> {
            if (film == selected) filmStrip.select(film);
            else selectFilm(film);
        });
        root.addView(filmStrip, new LinearLayout.LayoutParams(-1, dp(68)));

        intensityRow = row();
        root.addView(intensityRow, new LinearLayout.LayoutParams(-1, dp(48)));
        intensityRow.setVisibility(View.GONE);
        strengthLabel = text("", 11, MUTED, MONO);
        intensityRow.addView(strengthLabel, new LinearLayout.LayoutParams(dp(126), -2));
        SeekBar seek = new SeekBar(this);
        seek.setMax(100);
        seek.setProgress(intensity);
        seek.setContentDescription(getString(R.string.ui_intensidade_do_filtro));
        seek.setProgressTintList(ColorStateList.valueOf(AMBER));
        seek.setThumbTintList(ColorStateList.valueOf(AMBER));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                intensity = value;
                updateLook();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        intensityRow.addView(seek, new LinearLayout.LayoutParams(0, -1, 1f));

        LinearLayout chips = row();
        root.addView(chips, new LinearLayout.LayoutParams(-1, dp(60)));
        bodyButton = chip(chips, getString(R.string.ui_camera), getString(R.string.ui_escolher_estilo_de_camera), this::chooseBody);
        modeButton = chip(chips, getString(R.string.ui_retrato), getString(R.string.ui_ajustar_desfoque_do_retrato), this::choosePortrait);
        lensButton = chip(chips, getString(R.string.ui_celular), getString(R.string.ui_escolher_sensor_fisico_e_zoom), this::chooseLens);

        LinearLayout controls = row();
        controls.setGravity(Gravity.CENTER);
        root.addView(controls, new LinearLayout.LayoutParams(-1, dp(75)));
        TextView gallery = galleryButton = button(getString(R.string.ui_galeria), getString(R.string.ui_abrir_galeria_do_aplicativo));
        gallery.setOnClickListener(view -> openPhoto());
        controls.addView(gallery, new LinearLayout.LayoutParams(dp(65), dp(48)));
        shutter = new ShutterView();
        shutter.setContentDescription(getString(R.string.ui_tirar_foto));
        shutter.setOnClickListener(view -> requestPhoto());
        LinearLayout.LayoutParams shutterParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        shutterParams.leftMargin = dp(22);
        shutterParams.rightMargin = dp(22);
        controls.addView(shutter, shutterParams);
        flip = button(getString(R.string.ui_selfie), getString(R.string.ui_alternar_camera_frontal_e_traseira));
        flip.setOnClickListener(view -> switchLens());
        controls.addView(flip, new LinearLayout.LayoutParams(dp(65), dp(48)));
    }

    private TextView topButton(LinearLayout parent, String label, String description, Runnable action) {
        TextView control = button(label, description);
        int width = parent.getChildCount() == 1 ? 65 : 48;
        parent.addView(control, new LinearLayout.LayoutParams(dp(width), dp(48)));
        control.setOnClickListener(v -> action.run());
        return control;
    }

    private TextView chip(LinearLayout parent, String label, String description, Runnable action) {
        TextView control = button(label, description);
        LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, dp(50), 1);
        slot.rightMargin = dp(3);
        parent.addView(control, slot);
        control.setOnClickListener(v -> action.run());
        return control;
    }

    private final ActivityResultLauncher<String[]> overlayPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    preferences.edit().putString("effect_overlay", uri.toString()).apply();
                    notifyUser(getString(R.string.effect_overlay_ready));
                } catch (SecurityException error) {
                    notifyUser(getString(R.string.effect_overlay_error));
                }
            });

    private void openSettings() {
        SettingsPanel.show(this, preferences, () -> overlayPicker.launch(new String[]{"image/*"}), () -> {
            showInfo = preferences.getBoolean("show_info", true);
            dateYearFirst = preferences.getBoolean("date_year_first", false);
            printFrame = PrintFrame.valueOf(preferences.getString("print_frame", cameraStyle.defaultFrame.name()));
            refreshHorizon();
            finish.refresh();
        });
    }

    private void setComparison(boolean original) {
        if (compareOriginal != original) {
            compareOriginal = original;
            finish.refresh();
        }
    }

    private void chooseExposure() {
        if (!cameraReady || controller.getCameraInfo() == null || controller.getCameraControl() == null) return;
        var state = controller.getCameraInfo().getExposureState();
        if (!state.isExposureCompensationSupported()) {
            notifyUser(getString(R.string.ui_compensacao_de_exposicao_indisponivel_nesta_c));
            return;
        }
        int low = state.getExposureCompensationRange().getLower();
        int high = state.getExposureCompensationRange().getUpper();
        double step = state.getExposureCompensationStep().doubleValue();
        String[] values = new String[high - low + 1];
        for (int i = low; i <= high; i++) values[i - low] = String.format(Locale.getDefault(), getString(R.string.ui_1f_ev), i * step);
        new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.ui_compensacao_de_exposicao))
                .setSingleChoiceItems(values, state.getExposureCompensationIndex() - low, (dialog, choice) -> {
                    var control = controller.getCameraControl();
                    if (control != null) control.setExposureCompensationIndex(choice + low);
                    dialog.dismiss();
                }).setPositiveButton(getString(R.string.ui_fechar), null).show();
    }

    private void choosePortrait() {
        if (working) return;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(8), dp(22), 0);
        TextView value = text(portraitLabel(portraitStrength), 14, PAPER, Typeface.DEFAULT);
        panel.addView(value);
        SeekBar slider = new SeekBar(this);
        slider.setMax(100);
        slider.setProgress(portraitStrength);
        slider.setContentDescription(getString(R.string.ui_intensidade_do_desfoque));
        panel.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText(portraitLabel(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.ui_desfoque_de_retrato))
                .setMessage(getString(R.string.ui_aplicado_a_foto_salva_quando_uma_pessoa_e_ide))
                .setView(panel)
                .setNegativeButton(getString(R.string.ui_cancelar), null)
                .setNeutralButton(getString(R.string.ui_desligar), (dialog, which) -> setPortraitStrength(0))
                .setPositiveButton(getString(R.string.ui_aplicar), (dialog, which) -> setPortraitStrength(slider.getProgress()))
                .show();
    }

    private String portraitLabel(int value) {
        return value == 0 ? getString(R.string.ui_desligado) : value + getString(R.string.ui_symbol_150);
    }

    private void setPortraitStrength(int value) {
        portraitStrength = value;
        preferences.edit().putInt("portrait_strength", value).apply();
        updateProfiles();
    }

    private void chooseFilm() {
        CatalogSelectors.films(this, selected, this::selectFilm);
    }

    private void chooseBody() {
        CatalogSelectors.bodies(this, cameraStyle, next -> {
            cameraStyle = next;
            if (!preferences.contains("print_frame")) printFrame = next.defaultFrame;
            preferences.edit().putString("body", next.name()).apply();
            updateProfiles();
        });
    }

    private void updateProfiles() {
        bodyButton.setText(getString(R.string.ui_camera));
        bodyButton.setContentDescription(getString(R.string.ui_camera_157) + cameraStyle.label(this));
        bodyButton.setTextColor(cameraStyle.accent);
        bodyButton.setBackground(touchBackground(PANEL, cameraStyle.accent, 10));
        int tint = Color.rgb(Color.red(cameraStyle.accent) / 5, Color.green(cameraStyle.accent) / 5,
                Color.blue(cameraStyle.accent) / 5);
        cameraRoot.setBackground(new br.com.lume35.camera.ui.CameraBodyBackground(tint, INK));
        modeButton.setText(portraitStrength > 0 ? getString(R.string.portrait_percent, portraitStrength) : getString(R.string.ui_retrato));
        modeButton.setTextColor(portraitStrength > 0 ? cameraStyle.accent : PAPER);
        formatLabel.setText(aspect() == 1 ? R.string.ui_1_1 : aspect() > .8f ? R.string.ui_6_7 : R.string.ui_3_4);
        cameraStage.requestLayout();
        updateLook();
    }

    private void startCamera() {
        if (controller != null) return;
        permissionButton.setVisibility(View.GONE);
        status.setText(getString(R.string.ui_abrindo_camera));
        status.setVisibility(View.VISIBLE);

        LifecycleCameraController session = new LifecycleCameraController(this);
        controller = session;
        session.setEnabledUseCases(CameraController.IMAGE_CAPTURE);
        CameraControls.configure(session);
        preview.setController(session);
        session.getInitializationFuture().addListener(() -> {
            if (isDestroyed() || controller != session) return;
            try {
                session.getInitializationFuture().get();
                boolean back = session.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                boolean selfie = session.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                hasBothLenses = back && selfie;
                if (!back && !selfie) {
                    cameraError(getString(R.string.ui_nenhuma_camera_disponivel));
                    return;
                }
                front = !back;
                session.setCameraSelector(front ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA);
                session.bindToLifecycle(this);
                cameraReady = true;
                refreshButtons();
            } catch (Exception error) {
                Log.w("Lume35", "Camera initialization failed", error);
                cameraError(getString(R.string.ui_nao_foi_possivel_abrir_a_camera));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void cameraError(String message) {
        cameraReady = false;
        if (controller != null) {
            controller.unbind();
            preview.setController(null);
            controller = null;
        }
        status.setText(message);
        status.setVisibility(View.VISIBLE);
        refreshButtons();
    }

    private void showPermission() {
        status.setText(getString(R.string.ui_permita_o_acesso_a_camera));
        status.setVisibility(View.VISIBLE);
        permissionButton.setVisibility(View.VISIBLE);
    }

    private void askPermission() {
        boolean asked = preferences.getBoolean("asked_permission", false);
        if (asked && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        preferences.edit().putBoolean("asked_permission", true).apply();
        cameraPermission.launch(Manifest.permission.CAMERA);
    }

    private void chooseLens() {
        if (!cameraReady || working) return;
        PhoneLenses.show(this, controller,
                (selector, label) -> { if (!working && cameraReady) selectRear(selector, label); },
                ratio -> lensButton.setText(String.format(Locale.ROOT, getString(R.string.ui_celular_1f), ratio)),
                this::notifyUser);
    }

    private void selectRear(CameraSelector selector, String name) {
        CameraSelector previous = controller.getCameraSelector();
        try {
            controller.setCameraSelector(selector);
            front = false;
            controller.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_OFF);
            flashMode = ImageCapture.FLASH_MODE_OFF;
            lensButton.setText(getString(R.string.ui_celular));
            lensButton.setContentDescription(getString(R.string.ui_celular_177) + name);
            refreshButtons();
        } catch (RuntimeException error) {
            try { controller.setCameraSelector(previous); }
            catch (RuntimeException recovery) { cameraError(getString(R.string.ui_reabra_o_app_para_reconectar_a_camera)); }
            notifyUser(getString(R.string.ui_esta_lente_nao_suporta_a_captura_atual));
        }
    }

    private void switchLens() {
        if (!cameraReady || !hasBothLenses || working) return;
        try {
            controller.setCameraSelector(front ? CameraSelector.DEFAULT_BACK_CAMERA
                    : CameraSelector.DEFAULT_FRONT_CAMERA);
            front = !front;
            lensButton.setText(getString(R.string.ui_celular));
            lensButton.setContentDescription(front ? getString(R.string.ui_celular_frontal) : getString(R.string.ui_celular_traseira));
            flashMode = ImageCapture.FLASH_MODE_OFF;
            controller.setImageCaptureFlashMode(flashMode);
            refreshButtons();
        } catch (RuntimeException error) {
            Log.w("Lume35", "Lens switch failed", error);
            notifyUser(getString(R.string.ui_nao_foi_possivel_trocar_de_lente));
        }
    }

    private boolean supportsFlash() {
        return cameraReady && controller.getCameraInfo() != null
                && controller.getCameraInfo().hasFlashUnit();
    }

    private void cycleFlash() {
        if (!supportsFlash() || working) return;
        switch (flashMode) {
            case ImageCapture.FLASH_MODE_OFF:
                flashMode = ImageCapture.FLASH_MODE_AUTO;
                break;
            case ImageCapture.FLASH_MODE_AUTO:
                flashMode = ImageCapture.FLASH_MODE_ON;
                break;
            default:
                flashMode = ImageCapture.FLASH_MODE_OFF;
        }
        controller.setImageCaptureFlashMode(flashMode);
        refreshButtons();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.getRepeatCount() == 0) requestPhoto();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void requestPhoto() {
        if (!shutter.isEnabled() || countingDown) return;
        int choice = preferences.getInt("timer_index", 0);
        int seconds = choice == 2 ? 10 : choice == 1 ? 3 : 0;
        if (seconds == 0) takePhoto();
        else countdown(seconds);
    }

    private void countdown(int remaining) {
        countingDown = remaining > 0;
        if (remaining == 0) {
            updateRoll();
            takePhoto();
            return;
        }
        roll.setText(String.format(Locale.ROOT, getString(R.string.ui_temporizador_d_s), remaining));
        timer.postDelayed(() -> countdown(remaining - 1), 1000);
    }

    private void takePhoto() {
        if (!cameraReady || working || !shutter.isEnabled() || pendingPhotos >= 4) return;
        File temporary;
        try {
            temporary = File.createTempFile("lume-", ".jpg", getCacheDir());
        } catch (IOException error) {
            notifyUser(getString(R.string.ui_sem_espaco_para_capturar_a_foto));
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int captureNumber = nextFrameNumber++;
        PhotoStore.Request capture = new PhotoStore.Request(selected, cameraStyle, lensStyle, aspect(),
                portraitStrength, development(), printFrame, stampColor(), stamp ? formatStamp(now) : null,
                "LUME_" + FILE_DATE.format(now), showInfo, preferences.getInt("jpeg", 94),
                new int[]{1600, 2560, 4000}[preferences.getInt("max_edge_index", 1)],
                System.currentTimeMillis(),
                preferences.getBoolean("keep_original", false) ? new File(getFilesDir(), "originals") : null,
                new PhotoStore.Effects(preferences.getInt("effect_dust", 0), preferences.getInt("effect_scratch", 0),
                        preferences.getInt("effect_edge", 0), filmSeed,
                        preferences.getString("effect_overlay", null), preferences.getInt("effect_overlay_mix", 0)),
                captureNumber);
        preferences.edit().putInt("effect_next_frame", nextFrameNumber).apply();

        working = true;
        pendingPhotos++;
        filmSeed = java.util.concurrent.ThreadLocalRandom.current().nextLong();
        refreshButtons();
        finish.refresh();
        shutterBlink.setAlpha(.24f);
        shutterBlink.animate().alpha(0).setDuration(160).start();
        shutter.animate().scaleX(.89f).scaleY(.89f).setDuration(80).withEndAction(
                () -> shutter.animate().scaleX(1).scaleY(1).setDuration(160).start()).start();
        if (preferences.getBoolean("haptic", true)) shutter.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        if (preferences.getBoolean("shutter_sound", false)) cameraSound.play(android.media.MediaActionSound.SHUTTER_CLICK);

        ImageCapture.Metadata metadata = new ImageCapture.Metadata();
        metadata.setReversedHorizontal(front);
        ImageCapture.OutputFileOptions output = new ImageCapture.OutputFileOptions.Builder(temporary)
                .setMetadata(metadata).build();
        try {
            controller.takePicture(output, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override public void onImageSaved(ImageCapture.OutputFileResults result) {
                            if (isDestroyed()) {
                                temporary.delete();
                                return;
                            }
                            working = false;
                            refreshButtons();
                            photos.execute(() -> developPhoto(temporary, capture));
                        }

                        @Override public void onError(ImageCaptureException error) {
                            captureFailed(temporary, error);
                        }
                    });
        } catch (RuntimeException error) {
            captureFailed(temporary, error);
        }
    }

    private void captureFailed(File temporary, Exception error) {
        Log.w("Lume35", "Capture failed", error);
        temporary.delete();
        working = false;
        finishPhoto(null, getString(R.string.ui_falha_ao_fotografar_tente_novamente), null, false);
    }

    private void developPhoto(File temporary, PhotoStore.Request capture) {
        Uri saved = null;
        android.graphics.Bitmap thumbnail = null;
        boolean portraitMissing = false;
        try {
            PhotoStore.Result result = PhotoStore.save(getApplicationContext(), temporary, capture);
            saved = result.uri;
            String exposure = result.lastExposure;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                lastExposure = exposure;
                    });
            if (result.overlayMissing) runOnUiThread(() -> notifyUser(getString(R.string.effect_overlay_error)));
            portraitMissing = capture.portraitStrength() > 0 && !result.portraitApplied;
            int next = preferences.getInt("frame", 0) + 1;
            preferences.edit().putString("photo", saved.toString()).putInt("frame", next).apply();
            try {
                thumbnail = getContentResolver().loadThumbnail(saved, new android.util.Size(112, 112), null);
            } catch (IOException ignored) { }
        } catch (IOException | RuntimeException error) {
            Log.w("Lume35", "Photo could not be saved", error);
        } finally {
            temporary.delete();
        }
        Uri uri = saved;
        android.graphics.Bitmap image = thumbnail;
        boolean noPortrait = portraitMissing;
        runOnUiThread(() -> finishPhoto(uri, getString(R.string.ui_nao_foi_possivel_salvar_a_foto), image, noPortrait));
    }

    private void finishPhoto(Uri saved, String errorMessage, android.graphics.Bitmap image, boolean noPortrait) {
        if (isDestroyed()) {
            if (image != null) image.recycle();
            return;
        }
        pendingPhotos = Math.max(0, pendingPhotos - 1);
        refreshButtons();
        if (saved == null) {
            notifyUser(errorMessage);
            return;
        }
        lastPhoto = saved;
        frameNumber = preferences.getInt("frame", 0);
        updateRoll();
        if (image != null) animateThumbnail(image);
        if (noPortrait) notifyUser(getString(R.string.ui_foto_salva_o_retrato_precisa_de_uma_pessoa_id));
    }

    private void animateThumbnail(android.graphics.Bitmap bitmap) {
        if (!cameraRoot.isShown() || !hasWindowFocus()) {
            bitmap.recycle();
            return;
        }
        android.widget.ImageView thumbnail = new android.widget.ImageView(this);
        thumbnail.setImageBitmap(bitmap);
        thumbnail.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        int size = dp(46);
        android.view.ViewGroupOverlay overlay = cameraRoot.getOverlay();
        int[] origin = new int[2], target = new int[2], root = new int[2];
        finderView.getLocationInWindow(origin);
        galleryButton.getLocationInWindow(target);
        cameraRoot.getLocationInWindow(root);
        int x = origin[0] - root[0] + finderView.getWidth() / 2;
        int y = origin[1] - root[1] + finderView.getHeight() / 2;
        thumbnail.layout(x, y, x + size, y + size);
        overlay.add(thumbnail);
        thumbnail.animate().translationX(target[0] - root[0] - x)
                .translationY(target[1] - root[1] - y).scaleX(.68f).scaleY(.68f)
                .alpha(.25f).setDuration(360).withEndAction(() -> {
                    overlay.remove(thumbnail);
                    bitmap.recycle();
                }).start();
    }

    private void openPhoto() {
        startActivity(new Intent(this, GalleryActivity.class));
    }

    private void updateRoll() {
        roll.setText(pendingPhotos > 0 ? getString(R.string.ui_revelando) + pendingPhotos
                : String.format(Locale.ROOT, getString(R.string.ui_foto_02d), frameNumber));
    }

    private void selectFilm(Film film) {
        selected = film;
        preferences.edit().putString("film_name", selected.name()).apply();
        updateFilm();
        imageLayer.setAlpha(.90f);
        imageLayer.animate().alpha(1).setDuration(155).start();
    }

    private void updateFilm() {
        filmStrip.select(selected);
        updateLook();
    }

    private void updateLook() {
        strengthLabel.setText(String.format(Locale.ROOT, getString(R.string.ui_intensidade_d), intensity));
        finish.refresh();
    }

    private void refreshButtons() {
        boolean streaming = preview.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING;
        shutter.setEnabled(cameraReady && streaming && !working && pendingPhotos < 4);
        shutter.setContentDescription(pendingPhotos == 0 ? getString(R.string.ui_tirar_foto) : getString(R.string.ui_tirar_foto_revelando) + pendingPhotos);
        if (!countingDown) updateRoll();
        shutter.invalidate();
        bodyButton.setEnabled(!working);modeButton.setEnabled(!working);lensButton.setEnabled(cameraReady&&!working);
        flip.setEnabled(cameraReady && hasBothLenses && !working);
        flip.setAlpha(flip.isEnabled() ? 1f : .42f);
        flash.setEnabled(supportsFlash() && !working);
        flash.setAlpha(flash.isEnabled() ? 1f : .42f);
        String mode = flashMode == ImageCapture.FLASH_MODE_AUTO ? getString(R.string.ui_auto)
                : flashMode == ImageCapture.FLASH_MODE_ON ? getString(R.string.ui_on) : getString(R.string.ui_off);
        flash.setText(getString(R.string.ui_flash_195) + mode);
        flash.setContentDescription(getString(R.string.ui_flash_196) + mode);
        date.setText(stamp ? getString(R.string.ui_data_on) : getString(R.string.ui_data));
        date.setContentDescription(stamp ? getString(R.string.ui_desativar_carimbo_de_data) : getString(R.string.ui_ativar_carimbo_de_data));
        date.setTextColor(stamp ? AMBER : PAPER);
    }

    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, int color, Typeface face) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(face);
        return view;
    }

    private TextView button(String label, String description) {
        TextView view = text(label, 11, getColor(R.color.lume_paper), MONO);
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.lume_control_text));
        view.setMinHeight(getResources().getDimensionPixelSize(R.dimen.lume_touch_target));
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setFocusable(true);
        view.setBackgroundResource(R.drawable.industrial_control);
        return view;
    }

    private GradientDrawable shape(int fill, int radius, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (border != Color.TRANSPARENT) shape.setStroke(dp(1), border);
        return shape;
    }

    private RippleDrawable touchBackground(int fill, int border, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x30ffffff),
                shape(fill, radius, border), shape(Color.WHITE, radius, Color.TRANSPARENT));
    }

    private final class FinishView extends View {
        private final Paint lines = new Paint(Paint.ANTI_ALIAS_FLAG);
        private DateStamp dateStamp =
                new DateStamp(0xffffad66);
        private String dateValue;
        private boolean gridEnabled, horizonEnabled;

        FinishView() {
            super(MainActivity.this);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            refresh();
        }

        void refresh() {
            if (getWidth() > 0 && getHeight() > 0) {
                previewEffect.install(imageLayer, development(), getWidth(), getHeight(), compareOriginal);
                dateValue = formatStamp(LocalDateTime.now());
                gridEnabled = preferences.getBoolean("grid", true);
                horizonEnabled = preferences.getBoolean("horizon", false);
                dateStamp = new DateStamp(stampColor());
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width = getWidth();
            int height = getHeight();
            previewEffect.draw(canvas, width, height);
            if (stamp && !compareOriginal) dateStamp.draw(canvas, width, height, dateValue);
            lines.setStrokeWidth(dp(1));
            lines.setColor(0x25ffffff);
            if (gridEnabled) for (int i = 1; i <= 2; i++) {
                canvas.drawLine(width * i / 3f, 0, width * i / 3f, height, lines);
                canvas.drawLine(0, height * i / 3f, width, height * i / 3f, lines);
            }
            if (horizonEnabled) {
                canvas.save();
                canvas.rotate(-horizonAngle, width / 2f, height / 2f);
                lines.setColor(Math.abs(horizonAngle) < 2 ? 0xffa5be8e : 0xaff6debb);
                canvas.drawLine(width * .38f, height * .5f, width * .62f, height * .5f, lines);
                canvas.restore();
            }
            lines.setColor(0x99fff1dc);
            int margin = dp(14);
            int edge = dp(11);
            drawCorner(canvas, margin, margin, edge, edge);
            drawCorner(canvas, width - margin, margin, -edge, edge);
            drawCorner(canvas, margin, height - margin, edge, -edge);
            drawCorner(canvas, width - margin, height - margin, -edge, -edge);
        }

        private void drawCorner(Canvas canvas, int x, int y, int dx, int dy) {
            canvas.drawLine(x, y, x + dx, y, lines);
            canvas.drawLine(x, y, x, y + dy, lines);
        }
    }

    @android.annotation.SuppressLint("AppCompatCustomView")
    private final class ShutterView extends TextView {
        private final Paint rings = new Paint(Paint.ANTI_ALIAS_FLAG);

        ShutterView() {
            super(MainActivity.this);
            setFocusable(true);
            setBackground(touchBackground(Color.TRANSPARENT, Color.TRANSPARENT, 48));
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float center = getWidth() * .5f;
            rings.setStyle(Paint.Style.STROKE);
            rings.setStrokeWidth(dp(2));
            rings.setColor(isEnabled() ? AMBER : LINE);
            canvas.drawCircle(center, center, dp(35), rings);
            rings.setStyle(Paint.Style.FILL);
            rings.setColor(isEnabled() ? PAPER : MUTED);
            canvas.drawCircle(center, center, dp(isPressed() ? 24 : 28), rings);
            if (pendingPhotos > 0) {
                rings.setColor(INK);
                rings.setTextSize(dp(14));
                rings.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(Integer.toString(pendingPhotos), center, center + dp(5), rings);
            }
        }
    }
}
