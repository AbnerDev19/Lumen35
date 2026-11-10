import br.com.lume35.camera.imaging.FilmPipeline;
import br.com.lume35.camera.imaging.FilmRecipe;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

class FilmSamples {
    static final int W = 520, H = 450;
    private static final String[] FILMS = {"Original", "Kodak Gold 200", "Kodak Portra 400",
            "Fuji Superia 400", "Fuji Pro 400H", "Kodak Tri-X 400", "Filme vencido", "Kodak Portra 160",
            "Kodak Portra 800", "Kodak Ultramax 400", "Kodak Ektar 100", "Fujifilm Velvia 50",
            "CineStill 800T", "CineStill 50D", "Ilford HP5 Plus 400", "Ilford Delta 3200", "Kodak T-Max 100",
            "Agfa APX 400", "Porcelana 400", "Manteiga 200", "Âmbar 200"};

    static BufferedImage source() {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(27,73,130), 0, 210, new Color(211,228,238)));
        g.fillRect(0, 0, W, H);
        g.setPaint(new GradientPaint(0, 183, new Color(91,129,100), 0, H, new Color(16,36,30)));
        g.fillRect(0, 183, W, H - 183);
        g.setColor(new Color(246,239,221));
        g.fillOval(367, 43, 85, 85);
        g.setColor(new Color(255,244,220));
        g.fillRect(374, 248, 110, 66);
        g.setColor(new Color(235,93,61));
        g.fillRect(32, 242, 61, 44);
        g.setColor(new Color(238,170,58));
        g.fillRect(93, 242, 61, 44);
        g.setColor(new Color(64,148,116));
        g.fillRect(154, 242, 61, 44);
        g.setColor(new Color(72,147,202));
        g.fillRect(215, 242, 61, 44);
        g.setColor(new Color(158,91,156));
        g.fillRect(276, 242, 61, 44);
        Color[] skin = {new Color(81,47,37), new Color(117,75,57), new Color(167,109,79),
                new Color(204,148,117), new Color(227,171,143), new Color(247,202,174)};
        for (int i = 0; i < skin.length; i++) {
            g.setPaint(new GradientPaint(36 + i * 76, 306, skin[i].brighter(), 86 + i * 76, 383, skin[i].darker()));
            g.fillOval(35 + i * 76, 302, 55, 76);
        }
        g.setPaint(new GradientPaint(32, 0, Color.BLACK, 488, 0, Color.WHITE));
        g.fillRect(32, 400, 456, 30);
        g.dispose();
        return image;
    }

    public static void main(String[] args) throws Exception {
        BufferedImage source = source();
        int[] original = source.getRGB(0, 0, W, H, null, 0, W);
        for (FilmRecipe recipe : FilmRecipe.values()) {
            int[] pixels = original.clone();
            new FilmPipeline(recipe, 1, 1, 0, 0, 0, 0, 1, 0, false, false, 80135L)
                    .apply(pixels, W, H);
            BufferedImage pair = new BufferedImage(2 * W + 60, H + 105, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = pair.createGraphics();
            g.setColor(new Color(25,22,18));
            g.fillRect(0, 0, pair.getWidth(), pair.getHeight());
            g.setColor(new Color(240,221,192));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
            g.drawString("LUME 35  /  " + FILMS[recipe.ordinal()], 20, 33);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
            g.drawString("ANTES  /  CENA SINTÉTICA", 20, 62);
            g.drawString("DEPOIS  /  INTENSIDADE 100%", W + 40, 62);
            g.drawImage(source, 20, 75, null);
            pair.setRGB(W + 40, 75, W, H, pixels, 0, W);
            g.dispose();
            ImageIO.write(pair, "png", new File(args[0], String.format("%02d-%s.png", recipe.ordinal(), FILMS[recipe.ordinal()].replace(' ', '-'))));
        }
    }
}
