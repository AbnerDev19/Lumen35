package br.com.lume35.camera.model;

import android.content.Context;
import br.com.lume35.camera.R;

public enum LensStyle {
    NEUTRAL(R.string.lensstyle_neutral,R.string.lensstyle_neutral_description,1.000f,0f,0, 0f, 0f, 1f, 0f),
    CANON(R.string.lensstyle_canon,R.string.lensstyle_canon_description,0.960f,2f,16, 0.16f, 0.09f, 0.95f, 0.04f),
    NIKON(R.string.lensstyle_nikon,R.string.lensstyle_nikon_description,1.100f,-1f,25, 0.065f, 0.25f, 1.03f, 0.012f),
    ZEISS(R.string.lensstyle_zeiss,R.string.lensstyle_zeiss_description,1.140f,0f,30, 0.024f, 0.19f, 1.12f, 0.016f),
    HELIOS(R.string.lensstyle_helios,R.string.lensstyle_helios_description,0.940f,3f,72, 0.42f, 0.48f, 1.25f, 0.065f),
    PLANAR80(R.string.lensstyle_planar80,R.string.lensstyle_planar80_description,0.930f,0f,12, 0.19f, 0.075f, 0.86f, 0.018f),
    MAMIYA(R.string.lensstyle_mamiya,R.string.lensstyle_mamiya_description,0.910f,1f,9, 0.25f, 0.05f, 0.88f, 0.027f),
    NOKTON(R.string.lensstyle_nokton,R.string.lensstyle_nokton_description,1.050f,2f,50, 0.12f, 0.35f, 1.18f, 0.12f),
    TAKUMAR(R.string.lensstyle_takumar,R.string.lensstyle_takumar_description,0.970f,8f,30, 0.17f, 0.17f, 0.96f, 0.052f);

    private final int label, description;
    public final float contrast, warmth, blur, aberration, oval, flare;
    public final int vignette;

    LensStyle(int label, int description, float contrast, float warmth, int vignette,
              float blur, float aberration, float oval, float flare) {
        this.label = label;
        this.description = description;
        this.contrast = contrast;
        this.warmth = warmth;
        this.vignette = vignette;
        this.blur = blur;
        this.aberration = aberration;
        this.oval = oval;
        this.flare = flare;
    }

    public int asset() { return this == NEUTRAL ? 10 : -100 - ordinal(); }
    public String label(Context context) { return context.getString(label); }
    public String description(Context context) { return context.getString(description); }
}
