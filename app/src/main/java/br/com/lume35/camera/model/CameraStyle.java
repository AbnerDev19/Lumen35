package br.com.lume35.camera.model;

import android.content.Context;
import br.com.lume35.camera.R;

import br.com.lume35.camera.imaging.PrintFrame;

public enum CameraStyle {
    CLASSIC(R.string.camerastyle_classic,R.string.camerastyle_classic_description,26,0.750000f,1.000f,0f,0xffd5b78d, PrintFrame.POLAROID, 0, 0f, 0f, 0f, 0f),
    KODAK(R.string.camerastyle_kodak,R.string.camerastyle_kodak_description,27,0.750000f,0.980f,5f,0xffe9b641, PrintFrame.STRIP_35, 3, 0.09f, 0.05f, 0.12f, 0.1f),
    FUJI(R.string.camerastyle_fuji,R.string.camerastyle_fuji_description,28,0.750000f,0.960f,-4f,0xff81b394, PrintFrame.THIN, 2, 0.05f, 0.02f, 0.04f, 0.02f),
    CANON(R.string.camerastyle_canon,R.string.camerastyle_canon_description,29,0.750000f,0.970f,3f,0xffddb494, PrintFrame.POLAROID, 2, 0.045f, 0.05f, 0.04f, 0.03f),
    PENTAX(R.string.camerastyle_pentax,R.string.camerastyle_pentax_description,14,0.750000f,1.090f,0f,0xffc5b8a5, PrintFrame.STRIP_35, -4, 0.05f, 0.1f, 0.025f, 0.02f),
    OLYMPUS(R.string.camerastyle_olympus,R.string.camerastyle_olympus_description,30,0.750000f,1.020f,0f,0xffc4c5bb, PrintFrame.NONE, -1, 0.027f, 0.055f, 0.012f, 0f),
    NIKON(R.string.camerastyle_nikon,R.string.camerastyle_nikon_description,18,0.750000f,1.070f,-1f,0xffbbbec1, PrintFrame.STRIP_35, -3, 0.071f, 0.095f, 0.018f, 0.01f),
    MINOLTA(R.string.camerastyle_minolta,R.string.camerastyle_minolta_description,31,0.750000f,0.940f,1f,0xffc5a89a, PrintFrame.THIN, 5, 0.025f, 0.045f, 0.065f, 0.04f),
    LEICA(R.string.camerastyle_leica,R.string.camerastyle_leica_description,17,0.750000f,1.110f,0f,0xffd08c78, PrintFrame.STRIP_35, -3, 0.12f, 0.13f, 0.024f, 0.02f),
    CANONET(R.string.camerastyle_canonet,R.string.camerastyle_canonet_description,21,0.750000f,0.930f,3f,0xffc4ad88, PrintFrame.POLAROID, 4, 0.09f, 0.065f, 0.094f, 0.06f),
    ELECTRO(R.string.camerastyle_electro,R.string.camerastyle_electro_description,20,0.750000f,0.950f,5f,0xffc4aa77, PrintFrame.THIN, 2, 0.085f, 0.055f, 0.15f, 0.09f),
    BESSA(R.string.camerastyle_bessa,R.string.camerastyle_bessa_description,32,0.750000f,1.080f,-2f,0xffb3b9aa, PrintFrame.NONE, -2, 0.059f, 0.1f, 0.036f, 0.01f),
    CONTAX(R.string.camerastyle_contax,R.string.camerastyle_contax_description,15,0.750000f,1.060f,1f,0xffcfbc99, PrintFrame.THIN, -1, 0.084f, 0.07f, 0.03f, 0.05f),
    MJU(R.string.camerastyle_mju,R.string.camerastyle_mju_description,13,0.750000f,1.130f,1f,0xffd5c3a2, PrintFrame.STRIP_35, -4, 0.15f, 0.14f, 0.115f, 0.04f),
    T4(R.string.camerastyle_t4,R.string.camerastyle_t4_description,19,0.750000f,1.100f,-1f,0xffa5b3a7, PrintFrame.STRIP_35, -2, 0.11f, 0.09f, 0.018f, 0.015f),
    GR(R.string.camerastyle_gr,R.string.camerastyle_gr_description,33,0.750000f,1.150f,0f,0xffb3b8ad, PrintFrame.NONE, -5, 0.17f, 0.16f, 0.027f, 0.01f),
    HASSEL(R.string.camerastyle_hassel,R.string.camerastyle_hassel_description,16,1.000000f,0.960f,0f,0xffb6b6a9, PrintFrame.MEDIUM, 2, 0.055f, 0.025f, 0.044f, 0f),
    MAMIYA(R.string.camerastyle_mamiya,R.string.camerastyle_mamiya_description,34,0.857143f,0.920f,1f,0xffbdada0, PrintFrame.MEDIUM, 4, 0.039f, 0.045f, 0.11f, 0f),
    P67(R.string.camerastyle_p67,R.string.camerastyle_p67_description,12,0.857143f,0.970f,2f,0xffb5a68b, PrintFrame.MEDIUM, 1, 0.08f, 0.065f, 0.055f, 0.01f),
    ROLLEI(R.string.camerastyle_rollei,R.string.camerastyle_rollei_description,35,1.000000f,0.940f,2f,0xffb6a78b, PrintFrame.MEDIUM, 5, 0.081f, 0.045f, 0.15f, 0.035f);

    private final int label, description;
    public final int asset, accent, toe;
    public final float ratio, contrast, warmth, vignette, curve, softness, leakChance;
    public final PrintFrame defaultFrame;

    CameraStyle(int label, int description, int asset, float ratio, float contrast,
                float warmth, int accent, PrintFrame defaultFrame, int toe, float vignette,
                float curve, float softness, float leakChance) {
        this.label = label;
        this.description = description;
        this.asset = asset;
        this.ratio = ratio;
        this.contrast = contrast;
        this.warmth = warmth;
        this.accent = accent;
        this.defaultFrame = defaultFrame;
        this.toe = toe;
        this.vignette = vignette;
        this.curve = curve;
        this.softness = softness;
        this.leakChance = leakChance;
    }
    public String label(Context context) { return context.getString(label); }
    public String description(Context context) { return context.getString(description); }
}
