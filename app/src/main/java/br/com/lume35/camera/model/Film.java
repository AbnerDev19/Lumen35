package br.com.lume35.camera.model;

import android.content.Context;
import br.com.lume35.camera.R;

import br.com.lume35.camera.imaging.FilmRecipe;

public enum Film {
    ORIGINAL("00", R.string.film_original, 0xffc2ae91, -200, R.string.film_original_description),
    GOLD("01", R.string.film_gold, 0xffe3aa61, 4, R.string.film_gold_description),
    COFFEE("02", R.string.film_coffee, 0xffbb8d73, 7, R.string.film_coffee_description),
    NIGHT("03", R.string.film_night, 0xff879dc2, 1, R.string.film_night_description),
    ROSE("04", R.string.film_rose, 0xffe4a1a4, 0, R.string.film_rose_description),
    MONO("05", R.string.film_mono, 0xffd0cbc0, 23, R.string.film_mono_description),
    EXPIRED("06", R.string.film_expired, 0xffd98454, -201, R.string.film_expired_description),
    PORTRA160("07", R.string.film_portra160, 0xffe7c997, 6, R.string.film_portra160_description),
    PORTRA800("08", R.string.film_portra800, 0xffdcb477, 5, R.string.film_portra800_description),
    ULTRAMAX("09", R.string.film_ultramax, 0xffd7b24b, 3, R.string.film_ultramax_description),
    EKTAR("10", R.string.film_ektar, 0xffba785a, 2, R.string.film_ektar_description),
    VELVIA("11", R.string.film_velvia, 0xffcf7cb3, 25, R.string.film_velvia_description),
    CINE800("12", R.string.film_cine800, 0xffa4b9c8, -202, R.string.film_cine800_description),
    CINE50("13", R.string.film_cine50, 0xffdba58c, 24, R.string.film_cine50_description),
    HP5("14", R.string.film_hp5, 0xffb4bcb1, -203, R.string.film_hp5_description),
    DELTA("15", R.string.film_delta, 0xffb6a3a1, 22, R.string.film_delta_description),
    TMAX("16", R.string.film_tmax, 0xffc6b77b, 11, R.string.film_tmax_description),
    APX("17", R.string.film_apx, 0xffb8b4a8, -204, R.string.film_apx_description),
    PORCELANA("18", R.string.film_porcelana, 0xffdcd0b3, -205, R.string.film_porcelana_description),
    MANTEIGA("19", R.string.film_manteiga, 0xffe7c46b, -206, R.string.film_manteiga_description),
    AMBAR("20", R.string.film_ambar, 0xffdfaa64, -207, R.string.film_ambar_description);

    public final String number;
    private final int label;
    private final int description;
    public final int swatch;
    public final int asset;

    Film(String number, int label, int swatch, int asset, int description) {
        this.number = number;
        this.label = label;
        this.swatch = swatch;
        this.asset = asset;
        this.description = description;
    }

    /** Nominal stock rating for the catalog, independent of the captured ISO. */
    public int nominalIso() {
        return switch (this) {
            case ORIGINAL, EXPIRED -> 0;
            case VELVIA, CINE50 -> 50;
            case TMAX, EKTAR -> 100;
            case PORTRA160 -> 160;
            case GOLD, MANTEIGA, AMBAR -> 200;
            case CINE800, PORTRA800 -> 800;
            case DELTA -> 3200;
            case COFFEE, NIGHT, ROSE, MONO, ULTRAMAX, HP5, APX, PORCELANA -> 400;
        };
    }

    public FilmRecipe recipe() { return FilmRecipe.valueOf(name()); }
    public int asset() { return asset; }
    public String label(Context context) { return context.getString(label); }
    public String description(Context context) { return context.getString(description); }
}
