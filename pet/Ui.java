package pet;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * 二次元主题层（樱花粉）。
 *
 * 所有弹窗（小游戏 / 状态 / 番茄钟 / 待办 / 面试题）统一走这里：
 * 无系统标题栏 + 圆角卡片 + 粉色渐变按钮 + 猫娘头像 + 柔和阴影。
 * 换肤只要改这个文件顶部的几个颜色常量。
 */
public final class Ui {

    /* ==================== 配色 ==================== */

    /** 主粉（按钮/强调） */
    public static final Color PINK        = new Color(0xFF, 0x7B, 0xA9);
    /** 深粉（渐变深端/描边） */
    public static final Color PINK_DEEP   = new Color(0xF2, 0x58, 0x9B);
    /** 浅粉（边框/分隔） */
    public static final Color PINK_LIGHT  = new Color(0xFF, 0xD3, 0xE2);
    /** 更浅的粉（hover 底） */
    public static final Color PINK_PALE   = new Color(0xFF, 0xF0, 0xF6);
    /** 淡紫（点缀） */
    public static final Color PURPLE      = new Color(0xB7, 0x8C, 0xFF);
    /** 卡片底：上白下虾粉 */
    public static final Color CARD_TOP    = new Color(0xFF, 0xFC, 0xFD);
    public static final Color CARD_BOTTOM = new Color(0xFF, 0xF4, 0xF9);
    /** 正文色（暖深棕，比纯黑柔和） */
    public static final Color TEXT        = new Color(0x5A, 0x45, 0x53);
    /** 次要文字 */
    public static final Color TEXT_SUB    = new Color(0x9C, 0x8A, 0x96);
    /** 金币金 */
    public static final Color GOLD        = new Color(0xF5, 0xB9, 0x42);
    /** 成功绿 / 失败红 */
    public static final Color OK          = new Color(0x3C, 0xB6, 0x84);
    public static final Color BAD         = new Color(0xEE, 0x5D, 0x7B);

    private static final String FONT = "微软雅黑";
    /** 圆角卡片外留白，用来画阴影 */
    private static final int PAD = 8;
    private static final int ARC = 24;

    private Ui() {}

    /** 字体速取 */
    public static Font f(int size, boolean bold) {
        return new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size);
    }

    /** 开抗锯齿 */
    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * 把 Swing 默认的"灰盒子"外观换成粉色：菜单、工具提示、滚动条等。
     * Pet.main 里调一次即可，对已有的聊天窗也生效。
     */
    public static void installGlobal() {
        UIManager.put("Menu.font", f(13, false));
        UIManager.put("Menu.background", Color.WHITE);
        UIManager.put("Menu.foreground", TEXT);
        UIManager.put("Menu.selectionBackground", PINK_PALE);
        UIManager.put("Menu.selectionForeground", PINK_DEEP);
        UIManager.put("Menu.borderPainted", Boolean.FALSE);
        UIManager.put("Menu.arrowIcon", null);
        UIManager.put("MenuItem.font", f(13, false));
        UIManager.put("CheckBoxMenuItem.font", f(13, false));
        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("MenuItem.selectionBackground", PINK_PALE);
        UIManager.put("MenuItem.selectionForeground", PINK_DEEP);
        UIManager.put("MenuItem.acceleratorForeground", TEXT_SUB);
        UIManager.put("MenuItem.borderPainted", Boolean.FALSE);
        UIManager.put("CheckBoxMenuItem.background", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.foreground", TEXT);
        UIManager.put("CheckBoxMenuItem.selectionBackground", PINK_PALE);
        UIManager.put("CheckBoxMenuItem.selectionForeground", PINK_DEEP);
        UIManager.put("PopupMenu.background", Color.WHITE);
        UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PINK_LIGHT, 1),
                new EmptyBorder(4, 4, 4, 4)));
        UIManager.put("Separator.foreground", PINK_LIGHT);
        UIManager.put("ToolTip.font", f(12, false));
        UIManager.put("ToolTip.background", new Color(0xFF, 0xFA, 0xFC));
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PINK_LIGHT, 1),
                new EmptyBorder(5, 9, 5, 9)));
        UIManager.put("OptionPane.messageFont", f(13, false));
        UIManager.put("OptionPane.buttonFont", f(13, false));
    }

    /* ==================== 窗口 ==================== */

    /** 一个粉色圆角卡片窗口：自带可拖动标题条 + 猫娘头像 + 关闭按钮，内容放 body */
    public static final class Win extends JDialog {
        public final JPanel body = new JPanel(new BorderLayout(0, 8));

        Win(String title) {
            super((Frame) null, title, false);
        }
    }

    /** 造窗口。w/h 含阴影留白，真正内容比它小一圈 */
    public static Win win(String title, int w, int h) {
        Win d = new Win(title);
        d.setUndecorated(true);
        d.setAlwaysOnTop(true);
        try {
            d.setBackground(new Color(0, 0, 0, 0));    // 圆角外透明，露出桌面
        } catch (Throwable ignored) {
            // 个别环境不支持逐像素透明，退化成浅粉实心窗，不影响使用
        }
        d.getRootPane().setOpaque(false);

        CardPanel root = new CardPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(PAD, PAD + 2, PAD + 4, PAD + 2));
        d.setContentPane(root);

        root.add(new TitleBar(d, title), BorderLayout.NORTH);
        d.body.setOpaque(false);
        d.body.setBorder(new EmptyBorder(2, 14, 6, 14));
        root.add(d.body, BorderLayout.CENTER);

        d.setSize(w, h);
        d.setLocationRelativeTo(null);
        escToClose(d);
        return d;
    }

    /** Esc 关窗 */
    static void escToClose(final JDialog d) {
        d.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        d.getRootPane().getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { d.dispose(); }
        });
    }

    /** 卡片主体：阴影 + 上下渐变 + 粉描边 */
    static class CardPanel extends JPanel {
        CardPanel() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth() - PAD * 2;
            int h = getHeight() - PAD * 2 - 2;
            if (w <= 0 || h <= 0) { g2.dispose(); return; }

            // 柔和外阴影：多层低透明度圆角叠出来
            for (int i = 7; i >= 1; i--) {
                g2.setColor(new Color(0xD9, 0x8C, 0xAA, 4));
                g2.fillRoundRect(PAD - i, PAD + i + 1, w + i * 2, h + i, ARC + i * 2, ARC + i * 2);
            }
            // 主体
            g2.setPaint(new GradientPaint(0, PAD, CARD_TOP, 0, PAD + h, CARD_BOTTOM));
            g2.fill(new RoundRectangle2D.Float(PAD, PAD, w, h, ARC, ARC));
            g2.setColor(PINK_LIGHT);
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new RoundRectangle2D.Float(PAD + 0.5f, PAD + 0.5f, w - 1, h - 1, ARC, ARC));
            g2.dispose();
        }
    }

    /** 自绘标题条：左边猫娘头像 + 标题，右边 ×，整条可拖动窗口 */
    static class TitleBar extends JPanel {
        TitleBar(final JDialog win, String text) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(10, 36));
            setBorder(new EmptyBorder(6, 4, 6, 2));

            JPanel left = new JPanel(new BorderLayout(8, 0));
            left.setOpaque(false);
            JLabel icon = new JLabel(catIcon(22));
            left.add(icon, BorderLayout.WEST);

            JLabel t = new JLabel(text);
            t.setFont(f(14, true));
            t.setForeground(TEXT);
            left.add(t, BorderLayout.CENTER);
            add(left, BorderLayout.WEST);

            JButton close = chrome("×", 24);
            close.addActionListener(e -> win.dispose());
            JPanel right = new JPanel(new BorderLayout());
            right.setOpaque(false);
            right.add(close, BorderLayout.EAST);
            add(right, BorderLayout.EAST);

            final Point[] press = new Point[1];
            MouseAdapter drag = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { press[0] = e.getPoint(); }
                @Override public void mouseDragged(MouseEvent e) {
                    if (press[0] == null) return;
                    Point p = win.getLocation();
                    win.setLocation(p.x + e.getX() - press[0].x, p.y + e.getY() - press[0].y);
                }
            };
            addMouseListener(drag);
            addMouseMotionListener(drag);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(new Color(0xF2, 0x58, 0x9B, 28));
            g2.fillRoundRect(0, 0, getWidth(), 6, 6, 6);
            g2.dispose();
        }
    }

    /** 标题条上的小圆点按钮（— × 之类） */
    static JButton chrome(final String text, int size) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                boolean hov = getModel().isRollover();
                g2.setColor(hov ? new Color(0xF2, 0x58, 0x9B, 36) : new Color(0, 0, 0, 0));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setFont(f(15, false));
                g2.setColor(hov ? PINK_DEEP : TEXT_SUB);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(size, size));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /* ==================== 按钮 ==================== */

    public static final int PRIMARY = 0;   // 粉渐变白字
    public static final int GHOST   = 1;   // 白底粉边粉字
    public static final int SOFT    = 2;   // 奶白底深字

    /** 圆角胶囊按钮 */
    public static class PetButton extends JButton {
        private final int style;
        private final int arc;

        public PetButton(String text, int style) {
            this(text, style, 34);
        }

        public PetButton(String text, int style, int height) {
            super(text);
            this.style = style;
            this.arc = height;
            setFont(f(13, true));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(0, 16, 0, 16));
            setPreferredSize(new Dimension(prefW(text), height));
        }

        int prefW(String text) {
            return getFontMetrics(getFont() == null ? f(13, true) : getFont()).stringWidth(text) + 40;
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, prefW(getText())), d.height);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            boolean hov = getModel().isRollover() && isEnabled();
            boolean down = getModel().isPressed();
            int y = down ? 1 : 0;

            if (!isEnabled()) {
                g2.setColor(new Color(0xEE, 0xE4, 0xEA));
            } else if (style == PRIMARY) {
                Color a = down ? PINK_DEEP : (hov ? new Color(0xFF, 0x8F, 0xB7) : PINK);
                Color b = down ? new Color(0xD9, 0x42, 0x84) : PINK_DEEP;
                g2.setPaint(new GradientPaint(0, 0, a, 0, h, b));
            } else if (style == GHOST) {
                g2.setColor(hov ? PINK_PALE : Color.WHITE);
            } else {
                g2.setColor(hov ? new Color(0xFF, 0xF7, 0xFB) : CARD_TOP);
            }
            g2.fill(new RoundRectangle2D.Float(0, y, w, h - 1, arc, arc));

            if (style != PRIMARY) {
                g2.setColor(isEnabled() ? (hov ? PINK : PINK_LIGHT) : new Color(0xE2, 0xD8, 0xDE));
                g2.setStroke(new BasicStroke(1.3f));
                g2.draw(new RoundRectangle2D.Float(0.7f, y + 0.7f, w - 1.4f, h - 2.4f, arc, arc));
            }

            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            String s = getText();
            int tx = (w - fm.stringWidth(s)) / 2;
            int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
            if (!isEnabled()) {
                g2.setColor(new Color(0xB3, 0xA6, 0xAE));
            } else if (style == PRIMARY) {
                g2.setColor(Color.WHITE);
            } else if (style == GHOST) {
                g2.setColor(PINK_DEEP);
            } else {
                g2.setColor(TEXT);
            }
            g2.drawString(s, tx, ty);
            g2.dispose();
        }
    }

    /** 快捷造按钮 */
    public static PetButton btn(String text, int style) {
        return new PetButton(text, style);
    }

    /* ==================== 文本 / 卡片 / 输入框 ==================== */

    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(f(size, bold));
        l.setForeground(color);
        return l;
    }

    /** 居中标题（浅粉底小标签） */
    public static JLabel chip(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(f(12, false));
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(4, 12, 4, 12));
        l.setOpaque(false);
        return l;
    }

    /** 白色圆角卡片容器 */
    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(0xFF, 0xFF, 0xFF, 220));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.setColor(new Color(0xFF, 0xE1, 0xEC));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 1.2f, getHeight() - 1.2f, 18, 18));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));
        return p;
    }

    /** 透明容器（只用来做布局） */
    public static JPanel clear() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    /** 圆角输入框 */
    public static JTextField input(int cols) {
        JTextField t = new JTextField(cols) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(hasFocus() ? PINK : PINK_LIGHT);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.8f : 1.2f));
                g2.draw(new RoundRectangle2D.Float(0.7f, 0.7f, getWidth() - 1.4f, getHeight() - 1.4f, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        t.setOpaque(false);
        t.setFont(f(13, false));
        t.setForeground(TEXT);
        t.setCaretColor(PINK_DEEP);
        t.setSelectionColor(new Color(0xFF, 0xD3, 0xE2));
        t.setSelectedTextColor(TEXT);
        t.setBorder(new EmptyBorder(7, 10, 7, 10));
        return t;
    }

    /** 进度条（粉色圆角） */
    public static JComponent progress(final int value, final int max, final int height) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int w = getWidth(), h = height;
                g2.setColor(new Color(0xFF, 0xE7, 0xEF));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                int vw = max <= 0 ? 0 : (int) (w * Math.min(1.0, value * 1.0 / max));
                if (vw > 0) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0xFF, 0xA8, 0xC6), w, 0, PINK_DEEP));
                    g2.fill(new RoundRectangle2D.Float(0, 0, Math.max(vw, h), h, h, h));
                }
                g2.dispose();
            }
        };
        c.setOpaque(false);
        c.setPreferredSize(new Dimension(10, height));
        return c;
    }

    /** 美化滚动条：细、粉、无箭头 */
    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(new EmptyBorder(0, 0, 0, 0));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        styleBar(sp.getVerticalScrollBar());
        styleBar(sp.getHorizontalScrollBar());
        return sp;
    }

    static void styleBar(JScrollBar bar) {
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(8, 8));
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(0xF7, 0xB9, 0xD0);
                trackColor = new Color(0, 0, 0, 0);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
                if (r.width <= 0 || r.height <= 0) return;
                Graphics2D g2 = aa(g);
                g2.setColor(thumbColor);
                g2.fill(new RoundRectangle2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 8, 8));
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
                // 轨道不画，保持干净
            }
        });
    }

    /** 弹一个粉色提示（替代 JOptionPane 的灰框） */
    public static void alert(String title, String msg) {
        final Win w = win(title, 420, 300);
        JLabel content = label("<html><body style='width:300px;line-height:1.7'>"
                + msg.replace("\n", "<br>") + "</body></html>", 13, false, TEXT);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(6, 4, 6, 4));
        wrap.add(content, BorderLayout.NORTH);
        w.body.add(wrap, BorderLayout.CENTER);
        JButton ok = btn("知道啦", PRIMARY);
        ok.addActionListener(e -> w.dispose());
        JPanel bottom = clear();
        bottom.add(ok);
        w.body.add(bottom, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /* ==================== 图形 ==================== */

    public static final int GLYPH_ASK  = 0;   // 问号
    public static final int GLYPH_RPS  = 1;   // 三个圈
    public static final int GLYPH_GIFT = 2;   // 礼物盒
    public static final int GLYPH_BOLT = 3;   // 闪电
    public static final int GLYPH_HEART = 4;  // 爱心
    public static final int GLYPH_CLOCK = 5;  // 时钟
    public static final int GLYPH_LIST  = 6;  // 清单

    /** 在 (cx,cy) 处画一个 size 大小的图标，用纯几何图形，不依赖 emoji 字体 */
    public static void glyph(Graphics2D g2, int type, int cx, int cy, int size, Color color) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        int r = size / 2;
        switch (type) {
            case GLYPH_ASK: {
                g.setFont(f(size, true));
                java.awt.FontMetrics fm = g.getFontMetrics();
                String s = "?";
                g.drawString(s, cx - fm.stringWidth(s) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
                break;
            }
            case GLYPH_RPS: {
                g.setStroke(new BasicStroke(size * 0.14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Float(cx - r, cy - r, size * 0.62f, size * 0.62f));
                g.draw(new Ellipse2D.Float(cx - r + size * 0.34f, cy - r, size * 0.62f, size * 0.62f));
                g.draw(new Ellipse2D.Float(cx - r + size * 0.17f, cy - r + size * 0.34f, size * 0.62f, size * 0.62f));
                break;
            }
            case GLYPH_GIFT: {
                g.setStroke(new BasicStroke(size * 0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new RoundRectangle2D.Float(cx - r, cy - r + size * 0.16f, size, size * 0.8f, size * 0.14f, size * 0.14f));
                g.drawLine(cx, (int) (cy - r + size * 0.16f), cx, cy + r);
                g.draw(new java.awt.geom.Arc2D.Float(cx - size * 0.34f, cy - r - size * 0.1f, size * 0.34f, size * 0.3f, 0, 180, java.awt.geom.Arc2D.OPEN));
                g.draw(new java.awt.geom.Arc2D.Float(cx, cy - r - size * 0.1f, size * 0.34f, size * 0.3f, 0, 180, java.awt.geom.Arc2D.OPEN));
                break;
            }
            case GLYPH_BOLT: {
                Path2D p = new Path2D.Float();
                p.moveTo(cx + size * 0.12f, cy - r);
                p.lineTo(cx - size * 0.28f, cy + size * 0.06f);
                p.lineTo(cx - size * 0.02f, cy + size * 0.06f);
                p.lineTo(cx - size * 0.14f, cy + r);
                p.lineTo(cx + size * 0.3f, cy - size * 0.1f);
                p.lineTo(cx + size * 0.02f, cy - size * 0.1f);
                p.closePath();
                g.fill(p);
                break;
            }
            case GLYPH_HEART: {
                Path2D p = new Path2D.Float();
                p.moveTo(cx, cy + size * 0.38f);
                p.curveTo(cx - size * 0.62f, cy - size * 0.06f, cx - size * 0.3f, cy - size * 0.56f, cx, cy - size * 0.18f);
                p.curveTo(cx + size * 0.3f, cy - size * 0.56f, cx + size * 0.62f, cy - size * 0.06f, cx, cy + size * 0.38f);
                p.closePath();
                g.fill(p);
                break;
            }
            case GLYPH_CLOCK: {
                g.setStroke(new BasicStroke(size * 0.12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Float(cx - r, cy - r, size, size));
                g.drawLine(cx, cy, cx, (int) (cy - size * 0.26f));
                g.drawLine(cx, cy, cx + (int) (size * 0.22f), cy + (int) (size * 0.1f));
                break;
            }
            default: {
                g.setStroke(new BasicStroke(size * 0.11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 3; i++) {
                    int y = cy - (int) (size * 0.3) + i * (int) (size * 0.3);
                    g.drawLine(cx - r, y, cx + r, y);
                }
                g.draw(new Ellipse2D.Float(cx - r - size * 0.06f, cy - r - size * 0.02f, size * 0.1f, size * 0.1f));
                break;
            }
        }
        g.dispose();
    }

    /* ==================== 猫娘头像 ==================== */

    private static Image catCache;
    private static boolean catTried;

    /** 圆形猫娘头像（读 assets/cat_idle.png 的头部；读不到就画一个） */
    public static javax.swing.ImageIcon catIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 粉色圆底
        g2.setColor(new Color(0xFF, 0xE3, 0xEE));
        g2.fillOval(0, 0, size, size);

        Image src = catImage();
        if (src != null) {
            int sw = src.getWidth(null), sh = src.getHeight(null);
            int cropH = (int) (sh * 0.55);              // 只要上半身：头
            int cropW = Math.min(sw, cropH);
            int sx = (sw - cropW) / 2;
            int dh = (int) ((double) cropH * size / cropW);
            g2.setClip(new Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(src, 0, 0, size, dh, sx, 0, sx + cropW, cropH, null);
        } else {
            // 兜底：画个猫脸
            g2.setColor(PINK);
            Path2D ear1 = new Path2D.Float();
            ear1.moveTo(size * 0.18f, size * 0.36f);
            ear1.lineTo(size * 0.28f, size * 0.06f);
            ear1.lineTo(size * 0.46f, size * 0.3f);
            ear1.closePath();
            g2.fill(ear1);
            Path2D ear2 = new Path2D.Float();
            ear2.moveTo(size * 0.82f, size * 0.36f);
            ear2.lineTo(size * 0.72f, size * 0.06f);
            ear2.lineTo(size * 0.54f, size * 0.3f);
            ear2.closePath();
            g2.fill(ear2);
            g2.setColor(new Color(0xFF, 0xF6, 0xF9));
            g2.fillOval((int) (size * 0.14), (int) (size * 0.22), (int) (size * 0.72), (int) (size * 0.68));
            g2.setColor(TEXT);
            g2.fillOval((int) (size * 0.34), (int) (size * 0.46), (int) (size * 0.09), (int) (size * 0.12));
            g2.fillOval((int) (size * 0.57), (int) (size * 0.46), (int) (size * 0.09), (int) (size * 0.12));
            g2.setColor(PINK);
            g2.setStroke(new BasicStroke(size * 0.05f));
            g2.drawArc((int) (size * 0.42), (int) (size * 0.56), (int) (size * 0.16), (int) (size * 0.12), 200, 140);
        }
        g2.dispose();
        return new javax.swing.ImageIcon(img);
    }

    static Image catImage() {
        if (!catTried) {
            catTried = true;
            File f = new File("assets/cat_idle.png");
            if (!f.isFile()) f = new File("assets/cat_happy.png");
            try {
                if (f.isFile()) catCache = ImageIO.read(f);
            } catch (Exception ignored) {
                catCache = null;
            }
        }
        return catCache;
    }

    /** 给容器加一张半透明水印（大猫头，放右下角） */
    public static JComponent watermark(final JComponent target, final int size) {
        target.setOpaque(false);
        return target;
    }

    /** 画爱心点缀背景的小工具（各窗口可选调用） */
    public static void sprinkle(Graphics2D g2, int w, int h) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        int[][] pts = {{w - 60, 40}, {40, h - 50}, {w - 100, h - 70}, {70, 60}};
        for (int i = 0; i < pts.length; i++) {
            glyph(g, GLYPH_HEART, pts[i][0], pts[i][1], 14 + (i % 3) * 6,
                    i % 2 == 0 ? new Color(0xFF, 0xC8, 0xDB) : new Color(0xE6, 0xD6, 0xFF));
        }
        g.dispose();
    }

    /** 菜单（统一样式） */
    public static JPopupMenu menu() {
        JPopupMenu m = new JPopupMenu();
        m.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PINK_LIGHT, 1),
                new EmptyBorder(5, 5, 5, 5)));
        return m;
    }

    public static JMenuItem item(String text, ActionListener al) {
        JMenuItem it = new JMenuItem(text);
        it.setFont(f(13, false));
        if (al != null) it.addActionListener(al);
        return it;
    }
}
