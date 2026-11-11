package br.com.lume35.camera.ui;

import android.app.Activity;
import br.com.lume35.camera.R;
import br.com.lume35.camera.model.CameraStyle;
import br.com.lume35.camera.model.Film;
import java.util.function.Consumer;

public final class CatalogSelectors {
    private CatalogSelectors() {}

    public static void films(Activity activity, Film selected, Consumer<Film> apply) {
        java.util.ArrayList<Film> films = new java.util.ArrayList<>();
        films.add(Film.PORCELANA);
        films.add(Film.MANTEIGA);
        films.add(Film.AMBAR);
        for (Film film : Film.values()) {
            if (!films.contains(film)) films.add(film);
        }
        java.util.ArrayList<VisualPicker.Item> items = new java.util.ArrayList<>();
        for (Film film : films) {
            String details = film.description(activity);
            if (film.nominalIso() > 0) details = activity.getString(R.string.industrial_stock_iso, film.nominalIso()) + " · " + details;
            items.add(new VisualPicker.Item(film.label(activity), details, film.asset(), film.swatch));
        }
        VisualPicker.show(activity, activity.getString(R.string.ui_escolha_seu_filme), activity.getString(R.string.ui_ajuste_a_intensidade_no_visor),
                items, films.indexOf(selected), index -> apply.accept(films.get(index)));
    }

    public static void bodies(Activity activity, CameraStyle cameraStyle, Consumer<CameraStyle> apply) {
        java.util.ArrayList<VisualPicker.Item> items = new java.util.ArrayList<>();
        CameraStyle[] bodies = CameraStyle.values();
        for (CameraStyle body : bodies) {
            items.add(new VisualPicker.Item(body.label(activity), body.description(activity), body.asset, body.accent));
        }
        VisualPicker.show(activity, activity.getString(R.string.ui_sua_camera), activity.getString(R.string.ui_cor_formato_e_acabamento),
                items, cameraStyle.ordinal(), index -> apply.accept(bodies[index]));
    }

}
