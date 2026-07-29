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

    // Caches images loaded from disk, so the same file isn't re-read on every render.
    // The key includes the target size, so different scalings of one file don't overwrite each other.
    private static final Map<String, BufferedImage> IMAGE_CACHE = new ConcurrentHashMap<>();

    private BufferedImage img;
    private static JFrame frame;
    static JLabel label;
    // Pending window title: show() creates the frame lazily, so setTitle may be called before one exists.
    private static String pendingTitle;

    /* ----------- load & optional resize ----------- */
    /** Loads an image, optionally scaled to a target size, reusing the cache when possible. */
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

    /** Loads an image as a private, drawable canvas - a shared cached copy would accumulate paint across frames. */
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

    /** Same as readAsFreshCanvas, scaled to a target size - lets the board background follow the window size. */
    public Img readAsFreshCanvas(String path, int targetWidth, int targetHeight) {
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
        BufferedImage copy = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        img = copy;
        return this;
    }

    /** Creates an empty canvas of the given size, filled with one color - the drawing surface for the whole scene. */
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

    /** Draws text with an exact pixel font size and optional bold. */
    public void drawText(String text, int x, int y, int fontSize, Color color, boolean bold) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, fontSize));
        g.drawString(text, x, y);
        g.dispose();
    }

    /** Measures how wide text would be, without drawing it - used to center text instead of guessing its width. */
    public int textWidth(String text, int fontSize, boolean bold) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, fontSize));
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(text);
        g.dispose();
        return width;
    }

    /* ----------- draw this image onto another ----------- */
    /** Composites this image onto another at the given position, respecting alpha. */
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
    /** Fills a rectangle, blending with whatever is underneath if the color has alpha. */
    public void fillRect(int x, int y, int w, int h, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRect(x, y, w, h);
        g.dispose();
    }

    /** Like fillRect but with rounded corners; arcWidth/arcHeight are corner diameters, as in Graphics2D. */
    public void fillRoundRect(int x, int y, int w, int h, int arcWidth, int arcHeight, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRoundRect(x, y, w, h, arcWidth, arcHeight);
        g.dispose();
    }

    /* ----------- draw a rectangle outline (e.g. selection border) ----------- */
    /** Strokes a rectangle outline, inset by half the stroke so it stays inside the given bounds. */
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
    /** Fills an oval inscribed in the given rectangle. */
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
    /** Shows this image in the shared window, creating it on first call and swapping the image afterwards. */
    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

         if (frame == null) {
        // First call - create the window. pack() here only sets the initial size;
        // after that the user is free to resize it by dragging.
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame(pendingTitle != null ? pendingTitle : "Image");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            label = new JLabel(new ImageIcon(img));
            // JLabel centers its icon by default, and the label stretches to fill the window -
            // which is usually slightly larger than the drawn image. That gap would offset every
            // click relative to the image. Anchoring top-left makes image (0,0) == mouse (0,0).
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setVerticalAlignment(SwingConstants.TOP);
            frame.add(label);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    } else {
        // Window already exists - just swap the image. Deliberately no pack() here:
        // it would snap the window back to the image size on every frame, undoing any manual resize.
        SwingUtilities.invokeLater(() -> {
            label.setIcon(new ImageIcon(img));
            frame.repaint();
        });
        }
    }

    /* ----------- access (optional) ----------- */
    public BufferedImage get() { return img; }

    /** Whether show() has actually created the window yet - check before calling contentSize(). */
    public boolean isReady() {
        return frame != null;
    }

    /**
     * The drawable area size right now (never a value cached from an earlier resize), so rendering
     * and click handling always read the same live source and can't drift out of sync.
     */
    public Dimension contentSize() {
        if (frame == null) {
            throw new IllegalStateException("Call show() before contentSize().");
        }
        return frame.getContentPane().getSize();
    }

/**
 * Sets the game window title. Static like frame/label, because every temporary Img canvas shares the one window.
 * Applied immediately if the frame exists, otherwise stored and applied when show() creates it.
 */
public static void setTitle(String title) {
    pendingTitle = title;
    if (frame != null) {
        SwingUtilities.invokeLater(() -> frame.setTitle(title));
    }
}

/** Registers a handler for left/any mouse click, receiving pixel coordinates inside the image. */
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
/** Registers a handler for right-clicks only - used to trigger a jump. */
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

/**
 * Registers a handler fired whenever the user resizes the window.
 * Reports the inner content size, not the raw frame size - the latter includes the border and title bar.
 */
public void onResize(java.util.function.BiConsumer<Integer, Integer> handler) {
    if (label == null) {
        throw new IllegalStateException("Call show() before onResize().");
    }
    frame.addComponentListener(new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            handler.accept(frame.getContentPane().getWidth(), frame.getContentPane().getHeight());
        }
    });
}
}
