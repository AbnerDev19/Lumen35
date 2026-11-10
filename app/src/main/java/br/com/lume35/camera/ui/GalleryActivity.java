package br.com.lume35.camera.ui;

import br.com.lume35.camera.R;

import android.app.Dialog;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GalleryActivity extends ComponentActivity {
    private final ExecutorService images = Executors.newFixedThreadPool(2);
    private final ExecutorService fullImages = Executors.newSingleThreadExecutor();
    private java.util.concurrent.Future<?> pendingImage;
    private final ArrayList<Uri> photos = new ArrayList<>();
    private final android.util.LruCache<Uri, String> captions = new android.util.LruCache<>(192);
    private final android.util.LruCache<Uri, Bitmap> cache = new android.util.LruCache<Uri, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(Uri key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private Dialog viewer;
    private TextView status;
    private BaseAdapter adapter;
    private Uri deletion;
    private String deletingName;
    private int reloadVersion;
    private final android.database.ContentObserver mediaChanges = new android.database.ContentObserver(
            new android.os.Handler(android.os.Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { reload(); }
    };
    private boolean retryDeletion;
    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>
            deleteConfirmation = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                Uri uri = deletion;
                deletion = null;
                if (result.getResultCode() != RESULT_OK || uri == null) return;
                if (retryDeletion) delete(uri);
                else removed(uri);
            });

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this, SystemBarStyle.dark(0xff171511), SystemBarStyle.dark(0xff171511));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xff202124, 0xff0e0e0f}));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            root.setPadding(bars.left + dp(16), bars.top, bars.right + dp(16), bars.bottom);
            return insets;
        });
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = VisualPicker.text(this, getString(R.string.ui_symbol_199), 30, 0xfff7ebd7);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription(getString(R.string.ui_voltar_a_camera));
        back.setOnClickListener(v -> finish());
        heading.addView(back, new LinearLayout.LayoutParams(dp(48), dp(58)));
        TextView title = VisualPicker.text(this, getString(R.string.ui_rolo_de_fotos), 28, 0xfff7ebd7);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        heading.addView(title);
        root.addView(heading);
        status = VisualPicker.text(this, getString(R.string.ui_carregando_fotos), 13, 0xffc4b49f);
        status.setPadding(dp(6), dp(2), dp(6), dp(16));
        root.addView(status);
        GridView grid = new GridView(this);
        grid.setNumColumns(2);
        grid.setHorizontalSpacing(dp(12));
        grid.setVerticalSpacing(dp(12));
        grid.setPadding(0, 0, 0, dp(12));
        grid.setClipToPadding(false);
        grid.setOnItemClickListener((parent, item, position, id) -> open(photos.get(position)));
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        adapter = new BaseAdapter() {
            @Override public int getCount() { return photos.size(); }
            @Override public Object getItem(int position) { return photos.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
                View cell = recycled != null ? recycled
                        : getLayoutInflater().inflate(R.layout.industrial_contact_cell, parent, false);
                ImageView image = cell.findViewById(R.id.contact_image);
                TextView caption = cell.findViewById(R.id.contact_caption);
                int width = grid.getColumnWidth();
                if (width <= 0) width = (getResources().getDisplayMetrics().widthPixels - dp(44)) / 2;
                image.getLayoutParams().height = Math.max(dp(48), width);
                Uri uri = photos.get(position);
                cell.setTag(uri);
                String open = getString(R.string.ui_abrir_foto) + (position + 1);
                Bitmap thumbnail = cache.get(uri);
                String info = captions.get(uri);
                image.setImageBitmap(thumbnail);
                bindCaption(cell, caption, open, info);
                if (thumbnail == null || info == null) images.execute(() -> {
                    try {
                        Bitmap loaded = cache.get(uri);
                        if (loaded == null) {
                            loaded = decode(uri, 540);
                            cache.put(uri, loaded);
                        }
                        String details = captions.get(uri);
                        if (details == null) {
                            details = contactCaption(uri);
                            captions.put(uri, details);
                        }
                        Bitmap ready = loaded;
                        String readyDetails = details;
                        runOnUiThread(() -> {
                            if (!isDestroyed() && uri.equals(cell.getTag())) {
                                image.setImageBitmap(ready);
                                bindCaption(cell, caption, open, readyDetails);
                            }
                        });
                    } catch (Exception ignored) { /* A deleted photo can disappear while loading. */ }
                });
                return cell;
            }
        };
        grid.setAdapter(adapter);
    }

    @Override protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaChanges);
        reload();
    }

    @Override protected void onPause() {
        getContentResolver().unregisterContentObserver(mediaChanges);
        super.onPause();
    }

    private void reload() {
        int version = ++reloadVersion;
        images.execute(() -> {
            ArrayList<Uri> found = new ArrayList<>();
            boolean failed = false;
            try (Cursor rows = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.VOLUME_NAME},
                    "(" + MediaStore.Images.Media.RELATIVE_PATH + "=? OR " + MediaStore.Images.Media.RELATIVE_PATH
                            + "=?) AND " + MediaStore.Images.Media.IS_PENDING + "=0",
                    new String[]{"Pictures/Lume 35/", "Pictures/Lume 35"}, MediaStore.Images.Media.DATE_TAKEN + " DESC, " + MediaStore.Images.Media.DATE_ADDED + " DESC, " + MediaStore.Images.Media._ID + " DESC")) {
                if (rows != null) {
                    while (rows.moveToNext()) {
                        String volume = rows.getString(1);
                        Uri collection = volume == null ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                : MediaStore.Images.Media.getContentUri(volume);
                        found.add(ContentUris.withAppendedId(collection, rows.getLong(0)));
                    }
                }
            } catch (Exception error) { failed = true; }
            boolean error = failed;
            runOnUiThread(() -> {
                if (isDestroyed() || version != reloadVersion) return;
                photos.clear();
                photos.addAll(found);
                adapter.notifyDataSetChanged();
                status.setText(error ? getString(R.string.ui_nao_foi_possivel_carregar_a_galeria)
                        : found.isEmpty() ? getString(R.string.ui_suas_novas_fotos_aparecerao_aqui)
                        : found.size() == 1 ? getString(R.string.ui_1_foto) : found.size() + getString(R.string.ui_fotos));
            });
        });
    }

    private void bindCaption(View cell, TextView caption, String open, String details) {
        caption.setText(details == null ? getString(R.string.industrial_metadata_pending) : details);
        cell.setContentDescription(open + (details == null ? "" : ", " + details.replace('\n', ' ')));
    }

    private String contactCaption(Uri uri) {
        try (var input = getContentResolver().openInputStream(uri)) {
            if (input == null) return getString(R.string.industrial_metadata_missing);
            var exif = new androidx.exifinterface.media.ExifInterface(input);
            String data = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT);
            String date = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL);
            String[] fields = data == null ? new String[0] : data.split("\n");
            String frame = fields.length > 4 && fields[4].matches("[0-9]+") ? fields[4] : "—";
            String day = date != null && date.length() >= 10 ? date.substring(0, 10).replace(':', '-') : "—";
            String film = fields.length > 0 ? fields[0] : "—";
            return getString(R.string.industrial_frame_metadata, frame, day, film);
        } catch (Exception unavailable) {
            return getString(R.string.industrial_metadata_missing);
        }
    }

    private Bitmap decode(Uri uri, int maxEdge) throws java.io.IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                (decoder, info, source) -> {
                    int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                    float scale = Math.min(1f, maxEdge / (float) Math.max(width, height));
                    decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                            Math.max(1, Math.round(height * scale)));
                });
    }

    private void open(Uri uri) {
        if (pendingImage != null) pendingImage.cancel(true);
        if (viewer != null) viewer.dismiss();
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
        viewer = dialog;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(0xff171511);
        TextView close = VisualPicker.text(this, getString(R.string.ui_rolo_de_fotos_208), 17, 0xfff7ebd7);
        close.setGravity(Gravity.CENTER_VERTICAL);
        close.setOnClickListener(v -> dialog.dismiss());
        content.addView(close, new LinearLayout.LayoutParams(-1, dp(48)));
        ZoomImageView image = new ZoomImageView(this);
        image.setSwipeListener(direction -> {
            int next = photos.indexOf(uri) + direction;
            if (next >= 0 && next < photos.size()) open(photos.get(next));
        });
        Bitmap thumbnail = cache.get(uri);
        if (thumbnail != null) image.setImageBitmap(thumbnail);
        content.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView hint = VisualPicker.text(this, getString(R.string.ui_carregando_foto), 13, 0xffc4b49f);
        hint.setGravity(Gravity.CENTER);
        content.addView(hint, new LinearLayout.LayoutParams(-1, dp(36)));
        LinearLayout actions = new LinearLayout(this);
        addAction(actions, getString(R.string.ui_compartilhar), () -> share(uri));
        addAction(actions, getString(R.string.ui_dados), () -> showData(uri));
        addAction(actions, getString(R.string.ui_excluir), () -> confirmDelete(uri));
        content.addView(actions, new LinearLayout.LayoutParams(-1, dp(52)));
        dialog.setContentView(content);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setLayout(-1, -1);
            ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
                content.setPadding(bars.left + dp(16), bars.top, bars.right + dp(16), bars.bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(content);
        }
        pendingImage = fullImages.submit(() -> {
            try {
                Bitmap bitmap = decode(uri, 3200);
                runOnUiThread(() -> {
                    if (isDestroyed() || !dialog.isShowing()) {
                        bitmap.recycle();
                        return;
                    }
                    image.setImageBitmap(bitmap);
                    hint.setText(getString(R.string.ui_deslize_entre_fotos_pinca_para_ampliar));
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (dialog.isShowing()) hint.setText(getString(R.string.ui_nao_foi_possivel_abrir_a_foto_em_tamanho_comp));
                });
            }
        });
        dialog.setOnDismissListener(ignored -> {
            if (viewer == dialog) {
                viewer = null;
                if (pendingImage != null) pendingImage.cancel(true);
                pendingImage = null;
            }
        });
    }

    private void addAction(LinearLayout bar, String title, Runnable action) {
        TextView button = VisualPicker.text(this, title, 13, 0xffebc89a);
        button.setGravity(Gravity.CENTER);
        button.setBackground(VisualPicker.shape(this, 0xff2d251d, 0xff584530));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, dp(48), 1);
        slot.setMargins(dp(3), 0, dp(3), 0);
        bar.addView(button, slot);
    }

    private void share(Uri uri) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType("image/jpeg").putExtra(android.content.Intent.EXTRA_STREAM, uri)
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(android.content.ClipData.newRawUri(getString(R.string.app_name), uri));
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.ui_compartilhar_foto)));
    }

    private void showData(Uri uri) {
        images.execute(() -> {
            String message = null;
            try (java.io.InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream != null) {
                    androidx.exifinterface.media.ExifInterface exif = new androidx.exifinterface.media.ExifInterface(stream);
                    message = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT);
                }
            } catch (java.io.IOException | SecurityException ignored) { }
            String info = message == null ? getString(R.string.ui_sem_dados_de_revelacao_gravados) : message;
            runOnUiThread(() -> {
                if (!isDestroyed() && !isFinishing()) new android.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.ui_dados_da_foto)).setMessage(info).setPositiveButton(getString(R.string.ui_fechar), null).show();
            });
        });
    }

    private void confirmDelete(Uri uri) {
        new android.app.AlertDialog.Builder(this).setTitle(getString(R.string.ui_excluir_foto))
                .setMessage(getString(R.string.ui_a_foto_sera_removida_tambem_da_galeria_do_cel))
                .setNegativeButton(getString(R.string.ui_cancelar), null).setPositiveButton(getString(R.string.ui_excluir), (dialog, item) -> delete(uri))
                .show();
    }

    private void delete(Uri uri) {
        try (Cursor row = getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DISPLAY_NAME}, null, null, null)) {
            if (row != null && row.moveToFirst()) deletingName = row.getString(0);
        } catch (SecurityException ignored) { }
        try {
            if (getContentResolver().delete(uri, null, null) > 0) removed(uri);
            else reload();
        } catch (SecurityException error) {
            try {
                android.app.PendingIntent request;
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    request = MediaStore.createDeleteRequest(getContentResolver(), java.util.List.of(uri));
                    retryDeletion = false;
                } else if (error instanceof android.app.RecoverableSecurityException recoverable) {
                    request = recoverable.getUserAction().getActionIntent();
                    retryDeletion = true;
                } else {
                    throw error;
                }
                deletion = uri;
                deleteConfirmation.launch(new androidx.activity.result.IntentSenderRequest.Builder(request).build());
            } catch (RuntimeException failure) {
                Toast.makeText(this, getString(R.string.ui_nao_foi_possivel_excluir_a_foto), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void removed(Uri uri) {
        if (deletingName != null && deletingName.matches("LUME_[0-9_]+\\.jpg")) {
            new java.io.File(new java.io.File(getFilesDir(), "originals"), deletingName).delete();
        }
        deletingName = null;
        cache.remove(uri);
        if (viewer != null) viewer.dismiss();
        reload();
    }

    private int dp(int value) { return VisualPicker.dp(this, value); }

    @Override protected void onDestroy() {
        if (viewer != null) viewer.dismiss();
        images.shutdownNow();
        fullImages.shutdownNow();
        cache.evictAll();
        super.onDestroy();
    }
}
