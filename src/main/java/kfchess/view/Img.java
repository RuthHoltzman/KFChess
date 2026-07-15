package kfchess.view;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight image‑utility class using only standard JDK APIs.
 */
public class Img {

    // קאש לתמונות שנטענו מהדיסק - מונע ImageIO.read חוזר על אותו קובץ
    // בכל render tick (60 פעמים בשנייה). המפתח כולל את מידות היעד כדי
    // שגדלים שונים של אותה תמונה לא ידרסו זה את זה.
    private static final Map<String, BufferedImage> IMAGE_CACHE = new ConcurrentHashMap<>();

    private BufferedImage img;
    private static JFrame frame;
    static JLabel label;

    /* ----------- load & optional resize ----------- */
    public Img read(String path,
                    Dimension targetSize,
                    boolean keepAspect,
                    Object interpolation /*ignored*/) {

        String cacheKey = path + "|" + (targetSize == null ? "orig" : targetSize.width + "x" + targetSize.height) + "|" + keepAspect;
        BufferedImage cached = IMAGE_CACHE.get(cacheKey);
        if (cached != null) {
            img = cached;
            return this;
        }

        BufferedImage source = IMAGE_CACHE.get(path);
        if (source == null) {
            try {
                source = ImageIO.read(new File(path));
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot load image: " + path);
            }
            if (source == null) throw new IllegalArgumentException("Unsupported image: " + path);
            IMAGE_CACHE.put(path, source);
        }

        if (targetSize == null) {
            img = source;
            return this;
        }

        int tw = targetSize.width, th = targetSize.height;
        int w = source.getWidth(), h = source.getHeight();

        int nw, nh;
        if (keepAspect) {
            double s = Math.min(tw / (double) w, th / (double) h);
            nw = (int) Math.round(w * s);
            nh = (int) Math.round(h * s);
        } else { nw = tw; nh = th; }

        BufferedImage dst = new BufferedImage(
                nw, nh,
                source.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB);

        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, nw, nh, null);
        g.dispose();

        IMAGE_CACHE.put(cacheKey, dst);
        img = dst;
        return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    /**
     * טוענת תמונה כ"קנבס" נקי לציור - עותק פרטי (לא משותף עם הקאש),
     * כי קנבס עומד להיות מצויר עליו (drawOn/fillRect וכו') בכל פריים,
     * ועותק משותף היה נצבע-על מפריים לפריים. עדיין נמנעת מקריאה מהדיסק
     * בזכות הקאש הפנימי.
     */
    public Img readAsFreshCanvas(String path) {
        BufferedImage source = IMAGE_CACHE.get(path);
        if (source == null) {
            try {
                source = ImageIO.read(new File(path));
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot load image: " + path);
            }
            if (source == null) throw new IllegalArgumentException("Unsupported image: " + path);
            IMAGE_CACHE.put(path, source);
        }
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        img = copy;
        return this;
    }

    /**
     * יוצרת קנבס ריק (לא נטען מקובץ) בגודל נתון, מלא בצבע רקע אחיד.
     * זו הדרך היחידה ליצור "משטח ציור" גדול יותר מתמונת הלוח עצמה -
     * למשל כדי להרכיב עליו את הלוח ולצדדיו את פאנלי הניקוד/המהלכים -
     * בלי לצאת מגבולות ה-API של Img ובלי להיעזר בשום ספריית גרפיקה אחרת.
     */
    public Img newCanvas(int width, int height, Color backgroundColor) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width, height);
        g.dispose();
        img = canvas;
        return this;
    }

    public int width() {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        return img.getWidth();
    }

    public int height() {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        return img.getHeight();
    }

    /**
     * מציירת טקסט עם שליטה אמיתית בגודל הפונט (בפיקסלים) ואפשרות הדגשה (bold).
     * putText הקיימת נשארת ללא שינוי (כדי לא לשבור קוד קיים שמשתמש בה) -
     * זו תוספת ל-API הפנימי של Img לצורך פאנלי הניקוד/המהלכים החדשים.
     */
    public void drawText(String text, int x, int y, int fontSize, Color color, boolean bold) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, fontSize));
        g.drawString(text, x, y);
        g.dispose();
    }

    /* ----------- draw this image onto another ----------- */
    public void drawOn(Img other, int x, int y) {
        if (img == null || other.img == null)
            throw new IllegalStateException("Both images must be loaded.");

        if (x + img.getWidth()  > other.img.getWidth()
         || y + img.getHeight() > other.img.getHeight())
            throw new IllegalArgumentException("Patch exceeds destination bounds.");

        Graphics2D g = other.img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);                               // handles alpha channel
        g.drawImage(img, x, y, null);
        g.dispose();
    }

    /* ----------- draw a filled, alpha-blended rectangle (highlights / sandglass) ----------- */
    public void fillRect(int x, int y, int w, int h, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRect(x, y, w, h);
        g.dispose();
    }

    /* ----------- draw a rectangle outline (e.g. selection border) ----------- */
    public void drawRect(int x, int y, int w, int h, Color color, int thickness) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawRect(x + thickness / 2, y + thickness / 2, w - thickness, h - thickness);
        g.dispose();
    }

    /* ----------- draw a filled oval (e.g. legal-move dot marker) ----------- */
    public void fillOval(int x, int y, int w, int h, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(x, y, w, h);
        g.dispose();
    }

    /* ----------- annotate with text ----------- */
    public void putText(String txt, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(img.getGraphics().getFont().deriveFont(fontSize * 12));     // simple scale
        g.drawString(txt, x, y);
        g.dispose();
    }

    /* ----------- display in a Swing window ----------- */
    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

         if (frame == null) {
        // פעם ראשונה - יוצרים את החלון
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Image");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            label = new JLabel(new ImageIcon(img));
            frame.add(label);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    } else {
        // החלון כבר קיים - רק מעדכנים את התמונה בתוכו
        SwingUtilities.invokeLater(() -> {
            label.setIcon(new ImageIcon(img));
            frame.pack();
            frame.repaint();
        });
        }
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }

public void onClick(java.util.function.BiConsumer<Integer, Integer> handler) {
    if (label == null) {
        throw new IllegalStateException("Call show() before onClick().");
    }

    
    label.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            handler.accept(e.getX(), e.getY());
        }
    });
}
// ב-Img.java, ליד onClick הקיימת
public void onRightClick(java.util.function.BiConsumer<Integer, Integer> handler) {
    if (label == null) {
        throw new IllegalStateException("Call show() before onRightClick().");
    }
    label.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                handler.accept(e.getX(), e.getY());
            }
        }
    });
}
}
