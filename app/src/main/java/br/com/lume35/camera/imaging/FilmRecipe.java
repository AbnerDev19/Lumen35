package br.com.lume35.camera.imaging;

/** Curves describe the density response before split toning and grain. */
public enum FilmRecipe {
    ORIGINAL(0, "0 32 80 128 192 240 255", 1, 0, 0, 0, 0, 0, 0),
    GOLD(200, "3 27 79 140 211 247 254", 1.22f, -.013f, .057f, .06f, .77f, 0, 0),
    COFFEE(400, "6 34 86 142 202 242 252", .83f, .009f, .027f, .04f, .59f, 0, 0),
    NIGHT(400, "2 25 78 133 195 241 254", 1.33f, -.048f, .009f, .08f, 1.08f, 0, 0),
    ROSE(400, "7 38 90 140 201 241 251", .75f, -.038f, -.018f, .045f, .55f, 0, 0),
    MONO(400, "1 17 67 138 212 248 255", 0, .34f, .57f, .11f, 1.42f, 0, 0),
    EXPIRED(400, "24 51 93 132 186 225 245", .73f, .049f, .084f, .14f, 1.65f, 1, .08f),
    PORTRA160(160, "8 42 91 138 196 236 249", .69f, .004f, .025f, .015f, .43f, 0, 0),
    PORTRA800(800, "6 38 90 146 211 245 254", .94f, -.015f, .049f, .092f, 1.12f, 0, 0),
    ULTRAMAX(400, "2 26 76 132 205 248 255", 1.39f, -.026f, .021f, .07f, 1.23f, 0, 0),
    EKTAR(100, "1 20 69 135 214 250 255", 1.53f, -.012f, .036f, .035f, .51f, 0, 0),
    VELVIA(50, "0 13 60 130 216 250 255", 1.7f, -.033f, .021f, .105f, .56f, 0, 0),
    CINE800(800, "3 34 82 128 187 232 252", .91f, -.077f, -.018f, .085f, .99f, 0, .43f),
    CINE50(50, "5 32 80 137 207 246 254", 1.03f, -.028f, .017f, .04f, .66f, 0, 0),
    HP5(400, "4 27 79 137 203 241 253", 0, .21f, .72f, .065f, 1.21f, 0, 0),
    DELTA(3200, "5 31 78 125 204 245 255", 0, .30f, .64f, .135f, 1.65f, 0, 0),
    TMAX(100, "2 25 79 138 208 246 255", 0, .28f, .54f, .025f, .48f, 0, 0),
    APX(400, "2 22 69 128 198 239 253", 0, .18f, .61f, .085f, 1.02f, 0, 0),
    PORCELANA(400, "15 46 94 144 202 238 251", .68f, -.019f, .023f, .032f, .53f, 0, 0),
    MANTEIGA(200, "7 34 87 148 216 249 255", 1.04f, .028f, .108f, .073f, .79f, 0, 0),
    AMBAR(200, "6 32 80 141 210 245 254", 1.09f, -.066f, .079f, .085f, .63f, 0, 0);

    final float[][] curve = new float[3][];
    final float saturation, shadow, light, vignette, grain, leak, halo;
    final float redMix, greenMix, cross;

    FilmRecipe(int iso, String points, float saturation, float shadow, float light,
               float vignette, float texture, float leak, float halo) {
        this.saturation = saturation;
        this.shadow = saturation == 0 ? 0 : shadow;
        this.light = saturation == 0 ? 0 : light;
        this.vignette = vignette;
        this.leak = leak;
        this.halo = halo;
        this.redMix = saturation == 0 ? shadow : .2126f;
        this.greenMix = saturation == 0 ? light : .7152f;
        this.grain = iso == 0 ? 0 : (float) (.018 * Math.sqrt(iso / 200d)) * texture;
        this.cross = iso == 0 || saturation == 0 ? 0 : .019f + .1f * Math.abs(this.shadow);
        String[] tokens = points.split(" ");
        for (int channel = 0; channel < 3; channel++) {
            int[] y = new int[tokens.length];
            for (int i = 0; i < y.length; i++) {
                int point = Integer.parseInt(tokens[i]);
                float bias = channel == 0 ? this.light : channel == 2 ? -this.light : 0;
                y[i] = Math.max(i == 0 ? 0 : y[i - 1],
                        Math.min(255, point + Math.round(bias * 25 * (1 - Math.abs(point - 145) / 145f))));
            }
            curve[channel] = MonotoneCurve.lookup(y);
        }
    }

    public float blackPoint() {
        return Math.max(curve[0][0], Math.max(curve[1][0], curve[2][0]));
    }
}
