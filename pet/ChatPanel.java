package pet;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * 和猫娘聊天（2008 QQ 风格窗口）。
 * 改版要点：无系统标题栏 + 自绘蓝色标题条、工具栏、自绘气泡聊天区、可换背景与头像。
 * 逻辑不变：回复统一走 Persona.chatAsync（后台线程 + 回调在 EDT），界面不卡。
 */
public class ChatPanel {

    static JDialog win;
    static ChatCanvas canvas;
    static JTextArea input;
    static JButton sendBtn;
    static JButton earlierBtn;
    static JScrollPane scroll;
    static Timer typingTimer;
    static int loaded;          // 已经载入界面的历史条数（分页游标）
    static final int PAGE = 100;

    static final Color TITLE_1 = new Color(0x2E, 0x7C, 0xC3);
    static final Color TITLE_2 = new Color(0x63, 0xAF, 0xE8);
    static final Color BAR_BG  = new Color(0xF1, 0xF6, 0xFB);
    static final Color LINE    = new Color(0xC8, 0xD8, 0xE8);

    public static void show() {
        if (win != null && win.isVisible()) {
            win.toFront();
            input.requestFocusInWindow();
            return;
        }

        canvas = new ChatCanvas();

        win = new JDialog((JFrame) null, "和猫娘聊天", false);
        win.setUndecorated(true);
        win.setAlwaysOnTop(true);
        win.setLayout(new BorderLayout());
        win.setSize(500, 640);
        win.setLocationRelativeTo(null);
        ((JComponent) win.getContentPane()).setBorder(BorderFactory.createLineBorder(new Color(0x4E, 0x86, 0xB8)));

        win.add(titleBar(), BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(canvas);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        canvas.attach(sp);
        scroll = sp;

        JPanel center = new JPanel(new BorderLayout());
        center.add(toolBar(), BorderLayout.NORTH);
        center.add(sp, BorderLayout.CENTER);
        center.add(inputArea(), BorderLayout.SOUTH);
        win.add(center, BorderLayout.CENTER);

        loadHistory();

        win.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { stopTyping(); }
        });
        win.setVisible(true);
        input.requestFocusInWindow();
    }

    /* ================= 标题条 ================= */

    static JComponent titleBar() {
        final JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, TITLE_1, 0, getHeight(), TITLE_2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0xFF, 0xFF, 0xFF, 0x40));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 36));
        bar.setBorder(new EmptyBorder(0, 10, 0, 6));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(new JLabel(roundIcon(canvas.catAvatar, 24)));
        JLabel title = new JLabel("和  喵酱  聊天中");
        title.setFont(new Font("微软雅黑", Font.BOLD, 13));
        title.setForeground(Color.WHITE);
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 4));
        right.setOpaque(false);
        JButton min = chromeBtn("—");
        min.addActionListener(e -> win.setVisible(false));
        JButton close = chromeBtn("×");
        close.addActionListener(e -> { stopTyping(); win.dispose(); });
        right.add(min);
        right.add(close);
        bar.add(right, BorderLayout.EAST);

        // 拖动标题条移动窗口
        final Point[] press = new Point[1];
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                press[0] = e.getPoint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (press[0] == null) return;
                Point p = win.getLocation();
                win.setLocation(p.x + e.getX() - press[0].x, p.y + e.getY() - press[0].y);
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        return bar;
    }

    static JButton chromeBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        b.setForeground(Color.WHITE);
        b.setPreferredSize(new Dimension(26, 22));
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** 把头像缩成小图标（标题栏用） */
    static javax.swing.ImageIcon roundIcon(java.awt.Image img, int size) {
        javax.swing.ImageIcon ic = new javax.swing.ImageIcon(img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH));
        return ic;
    }

    /* ================= 工具栏 ================= */

    static JComponent toolBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BAR_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(LINE);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 34));

        JButton emoji = toolBtn("表情");
        emoji.addActionListener(e -> showEmoji(emoji));
        JButton shake = toolBtn("抖一抖");
        shake.addActionListener(e -> shake());
        JButton skin = toolBtn("换背景");
        skin.addActionListener(e -> canvas.chooseBg());
        JButton avatar = toolBtn("换头像");
        avatar.addActionListener(e -> showAvatarMenu(avatar));
        JButton clear = toolBtn("清空记录");
        clear.addActionListener(e -> canvas.clearChat());
        earlierBtn = toolBtn("更早");
        earlierBtn.setEnabled(false);
        earlierBtn.addActionListener(e -> loadEarlier());

        bar.add(emoji);
        bar.add(shake);
        bar.add(skin);
        bar.add(avatar);
        bar.add(earlierBtn);
        bar.add(clear);

        JLabel hint = new JLabel("右键聊天区也能换皮肤");
        hint.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        hint.setForeground(new Color(0x94, 0xA7, 0xB8));
        bar.add(Box.createHorizontalStrut(6));
        bar.add(hint);
        return bar;
    }

    static JButton toolBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        b.setForeground(new Color(0x35, 0x4A, 0x5C));
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static void showAvatarMenu(Component at) {
        JPopupMenu m = new JPopupMenu();
        javax.swing.JMenuItem me = new javax.swing.JMenuItem("更换「我」的头像…");
        me.addActionListener(e -> canvas.chooseAvatar(true));
        javax.swing.JMenuItem cat = new javax.swing.JMenuItem("更换猫娘头像…");
        cat.addActionListener(e -> canvas.chooseAvatar(false));
        javax.swing.JMenuItem reset = new javax.swing.JMenuItem("恢复默认头像和背景");
        reset.addActionListener(e -> canvas.resetSkin());
        m.add(me);
        m.add(cat);
        m.addSeparator();
        m.add(reset);
        m.show(at, 0, at.getHeight());
    }

    static void showEmoji(Component at) {
        final String[] faces = {
            "^_^", "T_T", ">_<", "QAQ", "233", "orz",
            "(=￣ω￣=)", "(*/ω＼*)", "(๑•̀ㅂ•́)و", "ヾ(≧▽≦*)o", "(￣▽￣)~*", "（＾ω＾）"
        };
        JPopupMenu m = new JPopupMenu();
        JPanel grid = new JPanel(new GridLayout(2, 6, 2, 2));
        grid.setBorder(new EmptyBorder(4, 4, 4, 4));
        for (final String f : faces) {
            JButton b = new JButton(f);
            b.setFont(new Font("微软雅黑", Font.PLAIN, 11));
            b.setMargin(new java.awt.Insets(2, 2, 2, 2));
            b.setBorder(new EmptyBorder(4, 6, 4, 6));
            b.setFocusPainted(false);
            b.addActionListener(e -> {
                input.insert(f, input.getCaretPosition());
                input.requestFocusInWindow();
                m.setVisible(false);
            });
            grid.add(b);
        }
        m.add(grid);
        m.show(at, 0, at.getHeight());
    }

    /** 经典 QQ 抖动窗口 */
    static void shake() {
        if (win == null) return;
        final Point origin = win.getLocation();
        final int[] step = {0};
        final Timer t = new Timer(25, null);
        t.addActionListener(e -> {
            int d = (step[0] % 2 == 0) ? 8 : -8;
            win.setLocation(origin.x + d, origin.y + (step[0] % 4 < 2 ? 3 : -3));
            step[0]++;
            if (step[0] > 12) {
                t.stop();
                win.setLocation(origin);
            }
        });
        t.start();
        canvas.addSystem("【窗口抖动】");
    }

    /* ================= 输入区 ================= */

    static JComponent inputArea() {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xF7, 0xFA, 0xFD));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(LINE);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        wrap.setOpaque(false);

        input = new JTextArea(3, 20);
        input.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(new EmptyBorder(6, 8, 6, 8));
        input.setBackground(Color.WHITE);

        JScrollPane isp = new JScrollPane(input);
        isp.setBorder(BorderFactory.createLineBorder(new Color(0xB8, 0xCB, 0xDE)));
        isp.setPreferredSize(new Dimension(10, 78));
        JPanel pad = new JPanel(new BorderLayout());
        pad.setOpaque(false);
        pad.setBorder(new EmptyBorder(6, 8, 4, 8));
        pad.add(isp, BorderLayout.CENTER);
        wrap.add(pad, BorderLayout.NORTH);

        // Enter 发送 / Shift+Enter 换行
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMsg");
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        input.getActionMap().put("sendMsg", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { send(); }
        });

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(0, 8, 8, 8));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        JButton minus = toolBtn("A-");
        minus.addActionListener(e -> zoomFont(-1));
        JButton plus = toolBtn("A+");
        plus.addActionListener(e -> zoomFont(1));
        JLabel tip = new JLabel("Enter 发送，Shift+Enter 换行");
        tip.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        tip.setForeground(new Color(0x94, 0xA7, 0xB8));
        tip.setBorder(new EmptyBorder(0, 8, 0, 0));
        left.add(minus);
        left.add(plus);
        left.add(tip);
        actions.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        sendBtn = sendButton("发送(S)");
        sendBtn.addActionListener(e -> send());
        JButton close = toolBtn("关闭");
        close.addActionListener(e -> { stopTyping(); win.dispose(); });
        right.add(sendBtn);
        right.add(close);
        actions.add(right, BorderLayout.EAST);

        wrap.add(actions, BorderLayout.SOUTH);
        return wrap;
    }

    static void zoomFont(int d) {
        int size = input.getFont().getSize() + d;
        size = Math.max(11, Math.min(22, size));
        input.setFont(new Font("微软雅黑", Font.PLAIN, size));
    }

    /** 蓝底白字圆角按钮（系统 LAF 会吃掉背景色，所以自绘） */
    static JButton sendButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean pressed = getModel().isPressed();
                Color c1 = pressed ? new Color(0x2C, 0x7C, 0xBE) : new Color(0x63, 0xB5, 0xEF);
                Color c2 = pressed ? new Color(0x1E, 0x64, 0xA6) : new Color(0x33, 0x8D, 0xD6);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(0xFF, 0xFF, 0xFF, 0x60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("微软雅黑", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 18, 6, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        return b;
    }

    /* ================= 逻辑 ================= */

    /* ================= 历史：全量保存，分页上翻 ================= */

    static void loadHistory() {
        loaded = 0;
        int total = Db.chatCount();
        if (total == 0) {
            canvas.add(false, "主人好喵！我是你的 Java 猫娘喵酱～\n今天想聊点什么？(＾▽＾)");
            updateEarlier();
            return;
        }
        List<String[]> page = Db.chatPage(0, PAGE);
        loaded = page.size();
        canvas.addSystem("共 " + total + " 句聊天记录，已载入最近 " + loaded + " 句");
        for (String[] m : page) {
            if (m[1].startsWith("让我想想喵")) continue;   // 旧版本占位消息，不回放
            canvas.addAt("owner".equals(m[0]), m[1], ts(m[2]));
        }
        canvas.addSystem("新对话从这里开始");
        updateEarlier();
    }

    /** 往上翻一页更早的历史（全部记录都在库里，不丢） */
    static void loadEarlier() {
        if (scroll == null) return;
        int oldH = canvas.getPreferredSize().height;
        int oldV = scroll.getVerticalScrollBar().getValue();

        List<String[]> page = Db.chatPage(loaded, PAGE);
        if (page.isEmpty()) { updateEarlier(); return; }
        for (int i = page.size() - 1; i >= 0; i--) {
            String[] m = page.get(i);
            if (m[1].startsWith("让我想想喵")) continue;
            canvas.insertFront("owner".equals(m[0]), m[1], ts(m[2]));
        }
        loaded += page.size();
        canvas.refresh();

        // 补完历史后保持视觉位置不跳动
        final int delta = canvas.getPreferredSize().height - oldH;
        javax.swing.SwingUtilities.invokeLater(
                () -> scroll.getVerticalScrollBar().setValue(oldV + delta));
        updateEarlier();
    }

    static void updateEarlier() {
        if (earlierBtn == null) return;
        int left = Math.max(0, Db.chatCount() - loaded);
        earlierBtn.setText(left > 0 ? "更早(" + left + ")" : "更早");
        earlierBtn.setEnabled(left > 0);
    }

    /** 清空记录后由 ChatCanvas 回调，重置分页游标 */
    public static void resetLoaded() {
        loaded = 0;
        updateEarlier();
    }

    static long ts(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return System.currentTimeMillis(); }
    }

    static void send() {
        if (input == null || sendBtn == null || !sendBtn.isEnabled()) return;
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        canvas.add(true, text);
        loaded++;
        updateEarlier();
        input.setText("");
        input.requestFocusInWindow();
        sendBtn.setEnabled(false);
        startTyping();
        Persona.chatAsync(text, reply -> {
            stopTyping();
            canvas.setTyping(false);
            canvas.add(false, reply == null || reply.isEmpty() ? "……喵？（没听清，再说一次好吗）" : reply);
            loaded++;
            updateEarlier();
            sendBtn.setEnabled(true);
        });
    }

    static void startTyping() {
        canvas.setTyping(true);
        canvas.dotPhase = 0;
        if (typingTimer != null) typingTimer.stop();
        typingTimer = new Timer(380, e -> {
            canvas.dotPhase++;
            canvas.repaint();
        });
        typingTimer.start();
    }

    static void stopTyping() {
        if (typingTimer != null) {
            typingTimer.stop();
            typingTimer = null;
        }
        if (canvas != null && canvas.typing) {
            canvas.setTyping(false);
        }
    }
}
