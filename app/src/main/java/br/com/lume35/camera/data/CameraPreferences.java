package br.com.lume35.camera.data;

import android.content.SharedPreferences;
import br.com.lume35.camera.model.Film;

public final class CameraPreferences {
    private static final String[] OLD_ORDER = {"ORIGINAL", "GOLD", "COFFEE", "NIGHT", "ROSE", "MONO",
            "EXPIRED", "PORTRA160", "PORTRA800", "ULTRAMAX", "EKTAR", "VELVIA", "CINE800",
            "CINE50", "HP5", "DELTA", "TMAX", "APX", "PORCELANA", "MANTEIGA", "AMBAR"};

    private CameraPreferences() {}

    public static Film readFilm(SharedPreferences preferences) {
        String name = preferences.getString("film_name", null);
        if (name == null) {
            int index = preferences.getInt("film", 1);
            name = index >= 0 && index < OLD_ORDER.length ? OLD_ORDER[index] : Film.GOLD.name();
            preferences.edit().putString("film_name", name).remove("film").apply();
        }
        try {
            return Film.valueOf(name);
        } catch (IllegalArgumentException invalid) {
            return Film.GOLD;
        }
    }
}
