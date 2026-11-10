package br.com.lume35.camera.imaging;

public final class ReviewSmoke {
    public static void main(String[] args) {
        int midpoint = LinearRgb.mix(0xff000000, 0xffffffff, .5f);
        require((midpoint & 255) == 188, "50% em luz linear");
        require(LinearRgb.mix(0xffff0000, 0xff0000ff, 0) == 0xffff0000, "mistura 0%");
        int w = 101, h = 67;
        int[] scene = new int[w * h];
        float[] matte = new float[scene.length];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = y * w + x;
            boolean subject = x >= 37 && x <= 63;
            scene[p] = subject ? 0xffff0000 : 0xff0d1a38;
            matte[p] = subject ? 1 : 0;
        }
        int[] rendered = PortraitBokeh.render(scene, matte, w, h, 1);
        require(rendered[33*w+36] == scene[33*w+36], "cor do primeiro plano excluída da amostra");
        require(rendered[33*w+30] == scene[33*w+30], "fundo constante preservado");
        float[] fakeMask = {0, 1, 0, 1};
        require(PortraitBokeh.sample(fakeMask, 2, 2, .5f, .5f) == .5f, "reamostragem bilinear");
        FilmPipeline original = new FilmPipeline(FilmRecipe.GOLD, 0, 1, 0, 0, 0, 0, 1, 0, true, true, 1,
                0, 0, 3, 0, 0);
        int[] test = {0xffa75527, 0xff434343};
        original.apply(test, 2, 1);
        require(test[0] == 0xffa75527 && test[1] == 0xff434343, "intensidade zero");
        FilmPipeline tint = new FilmPipeline(FilmRecipe.GOLD, 1, 1, 0, .3f, 0, 0, 1, 0, true, true, 1,
                0, 0, 3, 0, 0);
        require(tint.halo == 0 && tint.vignette == 0 && tint.leakB == 1, "preferências de acabamento");
        System.out.println("7 verificações de imagem aprovadas.");
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
