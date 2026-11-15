package pet;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * 2008 QQ 风格聊天区：整块自绘。
 * - 左右头像 + 带尖角的气泡（自己右侧浅蓝，猫娘左侧白色）
 * - 超过 4 分钟自动插入居中时间条
 * - 背景图 / 双方头像都可换，路径存 SQLite（重启不丢）
 * - 右键菜单：换背景 / 换头像 / 恢复默认 / 清空记录
 */
public class ChatCanvas extends JComponent implements Scrollable {

    /* ================= 数据 ================= */

    static class Msg {
        final boolean mine;
        final String text;
        final long time;

        Msg(boolean mine, String text, long time) {
            this.mine = mine;
            this.text = text;
            this.time = time;
        }
    }

    static class Row {
        boolean timeRow;
        String ts;
        boolean mine;
        List<String> lines;
        int bubbleW, bubbleH;
        int y, h;
    }

    final List<Msg> msgs = new ArrayList<Msg>();
    boolean typing;              // 猫娘正在输入
    int dotPhase;

    BufferedImage bg;            // 自定义背景，null = 默认皮肤
    BufferedImage bgScaled;      // 按窗口尺寸缓存
    int bgW, bgH;

    Image meAvatar, catAvatar;

    /* ================= 样式常量 ================= */

    static final Font F_MSG  = new Font("微软雅黑", Font.PLAIN, 13);
    static final Font F_TIME = new Font("微软雅黑", Font.PLAIN, 11);
    static final Font F_TIP  = new Font("微软雅黑", Font.PLAIN, 11);

    static final int AV = 46;      // 头像边长
    static final int GAP = 10;     // 头像与气泡间距
    static final int PAD = 14;     // 画布内边距
    static final int TAIL = 8;     // 气泡尖角长度
    static final int ARC = 10;     // 气泡圆角
    static final int TX = 11;      // 气泡内文字左内边距
    static final int TY = 9;       // 气泡内文字上内边距

    static final Color C_MINE_FILL  = new Color(0xB9, 0xE2, 0xFF);
    static final Color C_MINE_LINE  = new Color(0x7F, 0xB9, 0xDD);
    static final Color C_OTHER_FILL = new Color(0xFF, 0xFF, 0xFF);
    static final Color C_OTHER_LINE = new Color(0xBC, 0xCB, 0xDA);
    static final Color C_TEXT       = new Color(0x23, 0x2A, 0x31);
    static final Color C_TIME       = new Color(0x8E, 0x9E, 0xAE);

    static final SimpleDateFormat HHMM = new SimpleDateFormat("HH:mm");

    JScrollPane scroll;

    public ChatCanvas() {
        setOpaque(true);
        loadSkin();

        JPopupMenu menu = new JPopupMenu();
        JMenuItem bgItem = new JMenuItem("设置聊天背景…");
        bgItem.addActionListener(e -> chooseBg());
        JMenuItem meItem = new JMenuItem("更换我的头像…");
        meItem.addActionListener(e -> chooseAvatar(true));
        JMenuItem catItem = new JMenuItem("更换猫娘头像…");
        catItem.addActionListener(e -> chooseAvatar(false));
        JMenuItem resetItem = new JMenuItem("恢复默认外观");
        resetItem.addActionListener(e -> resetSkin());
        JMenuItem clearItem = new JMenuItem("清空聊天记录");
        clearItem.addActionListener(e -> clearChat());
        menu.add(bgItem);
        menu.add(meItem);
        menu.add(catItem);
        menu.addSeparator();
        menu.add(resetItem);
        menu.add(clearItem);
        setComponentPopupMenu(menu);

        // 双击聊天区也能换背景，方便发现
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) chooseBg();
            }
        });
    }

    public void attach(JScrollPane sp) { this.scroll = sp; }

    /* ================= 对外操作 ================= */

    public void add(boolean mine, String text) {
        if (text == null || text.isEmpty()) return;
        msgs.add(new Msg(mine, clean(text), System.currentTimeMillis()));
        afterChange();
    }

    public void addAt(boolean mine, String text, long time) {
        if (text == null || text.isEmpty()) return;
        msgs.add(new Msg(mine, clean(text), time));
        invalidate();
        repaint();
    }

    /** 往最前面插更早的消息（历史分页加载用，插完记得调 refresh） */
    public void insertFront(boolean mine, String text, long time) {
        if (text == null || text.isEmpty()) return;
        msgs.add(0, new Msg(mine, clean(text), time));
    }

    public void refresh() {
        revalidate();
        repaint();
    }

    public void addSystem(String text) {
        msgs.add(new Msg(false, "〔" + text + "〕", System.currentTimeMillis()));
        afterChange();
    }

    public void clearAll() {
        msgs.clear();
        typing = false;
        revalidate();
        repaint();
    }

    public void setTyping(boolean on) {
        typing = on;
        afterChange();
    }

    void afterChange() {
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            if (scroll != null) {
                JScrollPane sp = scroll;
                sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());
            }
        });
    }

    void clearChat() {
        int r = javax.swing.JOptionPane.showConfirmDialog(this,
                "清空后猫娘就不记得这些对话了，确定吗？", "清空聊天记录",
                javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
        if (r != javax.swing.JOptionPane.OK_OPTION) return;
        try {
            java.sql.Statement st = Db.conn.createStatement();
            st.executeUpdate("DELETE FROM chat_memory");
            st.close();
        } catch (Exception ignored) { }
        clearAll();
        ChatPanel.resetLoaded();
        addSystem("聊天记录已清空");
    }

    /* ================= 换肤 ================= */

    void loadSkin() {
        bg = readImage(Db.get("chat_bg", ""));
        meAvatar = readImage(Db.get("avatar_me", ""));
        catAvatar = readImage(Db.get("avatar_cat", ""));
        if (meAvatar == null) meAvatar = defaultMeAvatar();
        if (catAvatar == null) catAvatar = catHeadAvatar();
        bgScaled = null;
    }

    void chooseBg() {
        File f = pickImage("选一张聊天背景图（建议 600×800 左右的图）");
        if (f == null) return;
        Db.set("chat_bg", f.getAbsolutePath());
        bg = readImage(f.getAbsolutePath());
        bgScaled = null;
        repaint();
    }

    void chooseAvatar(boolean mine) {
        File f = pickImage(mine ? "选一张图片作为「我」的头像" : "选一张图片作为猫娘的头像");
        if (f == null) return;
        Db.set(mine ? "avatar_me" : "avatar_cat", f.getAbsolutePath());
        if (mine) meAvatar = readImage(f.getAbsolutePath());
        else      catAvatar = readImage(f.getAbsolutePath());
        if (mine && meAvatar == null) meAvatar = defaultMeAvatar();
        if (!mine && catAvatar == null) catAvatar = catHeadAvatar();
        repaint();
    }

    void resetSkin() {
        Db.set("chat_bg", "");
        Db.set("avatar_me", "");
        Db.set("avatar_cat", "");
        bg = null;
        bgScaled = null;
        meAvatar = defaultMeAvatar();
        catAvatar = catHeadAvatar();
        repaint();
    }

    File pickImage(String title) {
        JFileChooser fc = new JFileChooser(new File("assets"));
        fc.setDialogTitle(title);
        fc.setFileFilter(new FileNameExtensionFilter("图片 (png/jpg/gif/bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    static BufferedImage readImage(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        File f = new File(path.trim());
        if (!f.isFile()) return null;
        try {
            return ImageIO.read(f);
        } catch (Exception e) {
            return null;
        }
    }

    /** 默认「我」的头像：蓝色渐变圆角方块 + 白色「我」 */
    static BufferedImage defaultMeAvatar() {
        int s = 128;
        BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x7F, 0xC4, 0xF2), 0, s, new Color(0x3E, 0x87, 0xC8)));
        g.fillRoundRect(0, 0, s, s, 24, 24);
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 0xE8));
        g.setFont(new Font("微软雅黑", Font.BOLD, 62));
        FontMetrics fm = g.getFontMetrics();
        String t = "我";
        g.drawString(t, (s - fm.stringWidth(t)) / 2, (s - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return im;
    }

    /** 默认猫娘头像：从待机立绘里裁头，裁不到就画一块粉色方块 */
    static BufferedImage catHeadAvatar() {
        try {
            BufferedImage src = Pet.idleImg;
            if (src != null) {
                int w = src.getWidth(), h = src.getHeight();
                int side = (int) (Math.min(w, h) * 0.72);
                int x = (w - side) / 2;
                int y = (int) (h * 0.04);
                if (y + side > h) side = h - y;
                if (side > 20) {
                    return src.getSubimage(x, y, side, side);
                }
            }
        } catch (Exception ignored) { }
        int s = 128;
        BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0xFF, 0xD9, 0xE8), 0, s, new Color(0xF3, 0xA6, 0xC8)));
        g.fillRoundRect(0, 0, s, s, 24, 24);
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 0xF0));
        g.setFont(new Font("微软雅黑", Font.BOLD, 56));
        FontMetrics fm = g.getFontMetrics();
        String t = "喵";
        g.drawString(t, (s - fm.stringWidth(t)) / 2, (s - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        return im;
    }

    /* ================= 排版 ================= */

    List<Row> layout(int width) {
        FontMetrics fm = getFontMetrics(F_MSG);
        int maxBubble = Math.max(140, (int) (width * 0.60));
        List<Row> rows = new ArrayList<Row>();
        int y = PAD;
        long lastTime = -1;
        for (int i = 0; i < msgs.size(); i++) {
            Msg m = msgs.get(i);
            if (lastTime < 0 || m.time - lastTime > 4 * 60 * 1000L) {
                Row t = new Row();
                t.timeRow = true;
                t.ts = HHMM.format(new Date(m.time));
                t.y = y;
                t.h = 24;
                rows.add(t);
                y += t.h;
            }
            lastTime = m.time;

            Row r = new Row();
            r.mine = m.mine;
            r.lines = wrap(m.text, fm, maxBubble - TX * 2);
            int textW = 0;
            for (String line : r.lines) textW = Math.max(textW, fm.stringWidth(line));
            r.bubbleW = Math.max(48, textW + TX * 2);
            r.bubbleH = r.lines.size() * fm.getHeight() + TY * 2;
            r.h = Math.max(AV, r.bubbleH) + 16;
            r.y = y;
            rows.add(r);
            y += r.h;
        }
        if (typing) {
            Row r = new Row();
            r.mine = false;
            r.lines = new ArrayList<String>();
            r.bubbleW = 62;
            r.bubbleH = 30;
            r.h = 46;
            r.y = y;
            rows.add(r);
            y += r.h;
        }
        return rows;
    }

    static List<String> wrap(String text, FontMetrics fm, int maxW) {
        List<String> out = new ArrayList<String>();
        String[] paras = text.split("\n", -1);
        for (int p = 0; p < paras.length; p++) {
            String para = paras[p];
            if (para.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i < para.length(); i++) {
                char c = para.charAt(i);
                if (cur.length() > 0 && fm.stringWidth(cur.toString() + c) > maxW) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                cur.append(c);
            }
            if (cur.length() > 0) out.add(cur.toString());
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    /** 把大模型爱打的 markdown 符号清掉，QQ 气泡里只留纯文本 */
    static String clean(String s) {
        String t = s.replace("\r", "");
        t = t.replace("**", "").replace("```", "").replace("###", "· ").replace("##", "· ");
        while (t.contains("\n\n\n")) t = t.replace("\n\n\n", "\n\n");
        return t.trim();
    }

    /* ================= 绘制 ================= */

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth(), h = getHeight();
        paintBackground(g, w, h);

        List<Row> rows = layout(w);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r.y > getVisibleRect().getMaxY() + 200) break;   // 屏幕外的跳过
            if (r.timeRow) {
                g.setFont(F_TIME);
                g.setColor(C_TIME);
                int tw = g.getFontMetrics().stringWidth(r.ts);
                g.drawString(r.ts, (w - tw) / 2, r.y + 15);
            } else if (r.lines != null && r.lines.isEmpty()) {
                paintTyping(g, r, w);
            } else {
                paintBubble(g, r, w);
            }
        }
        g.dispose();
    }

    void paintBackground(Graphics2D g, int w, int h) {
        if (bg != null) {
            if (bgScaled == null || bgW != w || bgH != h) {
                BufferedImage tmp = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
                Graphics2D tg = tmp.createGraphics();
                tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                tg.drawImage(bg, 0, 0, w, h, null);
                tg.dispose();
                bgScaled = tmp;
                bgW = w;
                bgH = h;
            }
            g.drawImage(bgScaled, 0, 0, null);
            // 蒙一层浅白，保证文字可读
            g.setColor(new Color(0xFF, 0xFF, 0xFF, 0x66));
            g.fillRect(0, 0, w, h);
            return;
        }
        // 默认皮肤：QQ 经典浅蓝渐变 + 斜纹
        g.setPaint(new GradientPaint(0, 0, new Color(0xEE, 0xF6, 0xFD), 0, h, new Color(0xD5, 0xE8, 0xF8)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0xFF, 0xFF, 0xFF, 0x3C));
        for (int x = -h; x < w; x += 16) {
            g.drawLine(x, h, x + h, 0);
        }
    }

    void paintBubble(Graphics2D g, Row r, int w) {
        int by = r.y + (AV > r.bubbleH ? (AV - r.bubbleH) / 2 : 0);
        int ax = r.mine ? w - PAD - AV : PAD;
        int ay = r.y + (r.bubbleH > AV ? 0 : (AV - r.bubbleH) / 2);

        // 头像
        drawAvatar(g, r.mine ? meAvatar : catAvatar, ax, Math.max(r.y, ay), AV, r.mine);

        int bx, tailX, tailDir;
        if (r.mine) {
            bx = w - PAD - AV - GAP - TAIL - r.bubbleW;
            tailX = bx + r.bubbleW;
            tailDir = 1;
        } else {
            bx = PAD + AV + GAP + TAIL;
            tailX = bx;
            tailDir = -1;
        }

        Color fill = r.mine ? C_MINE_FILL : C_OTHER_FILL;
        Color line = r.mine ? C_MINE_LINE : C_OTHER_LINE;

        // 尖角
        int ty = by + 15;
        int[] xs = {tailX, tailX + tailDir * TAIL, tailX};
        int[] ys = {ty - 6, ty, ty + 6};
        g.setColor(fill);
        g.fillPolygon(xs, ys, 3);

        // 气泡本体
        g.setColor(fill);
        g.fillRoundRect(bx, by, r.bubbleW, r.bubbleH, ARC, ARC);
        // 去掉尖角与本体交界处的边线
        g.setColor(fill);
        g.fillRect(tailX - (tailDir > 0 ? 3 : 0), ty - 6, 3, 12);

        g.setColor(line);
        g.setStroke(new BasicStroke(1f));
        RoundRectangle2D rr = new RoundRectangle2D.Float(bx + 0.5f, by + 0.5f, r.bubbleW - 1, r.bubbleH - 1, ARC, ARC);
        g.draw(rr);
        g.drawLine(tailX, ty - 6, tailX + tailDir * TAIL, ty);
        g.drawLine(tailX + tailDir * TAIL, ty, tailX, ty + 6);

        // 文字
        g.setFont(F_MSG);
        g.setColor(C_TEXT);
        FontMetrics fm = g.getFontMetrics();
        int tx = bx + TX;
        int base = by + TY + fm.getAscent();
        for (int i = 0; i < r.lines.size(); i++) {
            g.drawString(r.lines.get(i), tx, base + i * fm.getHeight());
        }
    }

    void paintTyping(Graphics2D g, Row r, int w) {
        int ax = PAD, ay = r.y;
        drawAvatar(g, catAvatar, ax, ay, AV, false);
        int bx = PAD + AV + GAP + TAIL;
        Color fill = C_OTHER_FILL, line = C_OTHER_LINE;
        int ty = ay + 15;
        g.setColor(fill);
        g.fillPolygon(new int[]{bx, bx - TAIL, bx}, new int[]{ty - 6, ty, ty + 6}, 3);
        g.fillRoundRect(bx, ay, r.bubbleW, r.bubbleH, ARC, ARC);
        g.setColor(line);
        g.drawRoundRect(bx, ay, r.bubbleW - 1, r.bubbleH - 1, ARC, ARC);
        g.drawLine(bx, ty - 6, bx - TAIL, ty);
        g.drawLine(bx - TAIL, ty, bx, ty + 6);
        // 三个点，相位动画
        g.setColor(new Color(0x6E, 0x86, 0x9A));
        for (int i = 0; i < 3; i++) {
            int rr = 3 + ((dotPhase + i) % 3);
            g.fillOval(bx + 18 + i * 12 - rr / 2, ay + 15 - rr / 2 + 2, rr, rr);
        }
    }

    void drawAvatar(Graphics2D g, Image img, int x, int y, int size, boolean mine) {
        Shape old = g.getClip();
        g.setClip(new RoundRectangle2D.Float(x, y, size, size, 8, 8));
        if (img != null) {
            g.drawImage(img, x, y, size, size, null);
        } else {
            g.setColor(mine ? new Color(0xDD, 0xEA, 0xF6) : new Color(0xF7, 0xE2, 0xEA));
            g.fillRect(x, y, size, size);
        }
        g.setClip(old);
        g.setColor(new Color(0x9F, 0xB4, 0xC6));
        g.drawRoundRect(x, y, size - 1, size - 1, 8, 8);
    }

    /* ================= Scrollable ================= */

    @Override public Dimension getPreferredSize() {
        int w = getWidth();
        if (w <= 0) w = 420;
        List<Row> rows = layout(w);
        int h = PAD + 6;
        if (!rows.isEmpty()) {
            Row last = rows.get(rows.size() - 1);
            h = last.y + last.h + PAD;
        }
        return new Dimension(w, Math.max(h, 10));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }

    @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(20, visibleRect.height - 40);
    }

    @Override public boolean getScrollableTracksViewportWidth() { return true; }

    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
