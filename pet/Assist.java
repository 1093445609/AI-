package pet;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * 智能助手（阶段4）：番茄钟 / 待办事项 / 休息提醒。
 * 计时全部用 Swing Timer（EDT 上跑，不卡界面）。
 * 界面：番茄钟 = 粉色圆环进度 + 大号倒计时；待办 = 自绘卡片列表。
 */
public final class Assist {

    static Ui.Win pomodoroWin;
    static Timer pomodoroTimer;
    static int remainSeconds;          // 剩余秒数
    static Ring pomodoroRing;

    private Assist() {}

    /** 久坐提醒：每 45 分钟一轮，猫娘会真的露出疲倦相并催你起来动动（Pet.main 调用一次） */
    public static void startRestReminder() {
        Timer t = new Timer(45 * 60 * 1000, ev -> {
            boolean working = pomodoroTimer != null && pomodoroTimer.isRunning();
            if (working) {
                // 番茄钟还在跑，别打断专注，只提醒一句
                Pet.say("已经专注 45 分钟了喵，这轮结束记得起来动动~");
            } else {
                Act.play(Act.A.TIRED, 9000, false);
                Pet.say("坐太久啦喵！站起来走两步、扭扭脖子，别把腰坐坏了~");
            }
        });
        t.setRepeats(true);
        t.start();
    }

    // ==================== 番茄钟 ====================

    public static void showPomodoro() {
        if (pomodoroWin != null && pomodoroWin.isVisible()) {
            pomodoroWin.toFront();
            return;
        }
        remainSeconds = 25 * 60;
        pomodoroWin = Ui.win("番茄钟", 330, 420);

        JLabel tip = Ui.label("专注写代码喵！完成奖励 +15 亲密度 +5 金币", 11, false, Ui.TEXT_SUB);
        tip.setHorizontalAlignment(JLabel.CENTER);
        tip.setBorder(new EmptyBorder(0, 0, 4, 0));

        pomodoroRing = new Ring();
        pomodoroRing.setText(String.format("%02d:%02d", remainSeconds / 60, remainSeconds % 60));
        pomodoroRing.setSub("保持专注哦");

        Ui.PetButton cancel = Ui.btn("放 弃", Ui.GHOST);
        cancel.setPreferredSize(new Dimension(140, 40));
        cancel.addActionListener(e -> {
            if (pomodoroTimer != null) pomodoroTimer.stop();
            pomodoroWin.dispose();
            Act.reset();                      // 放弃番茄钟 → 收起键盘回待机
            Emotion.trigger(Emotion.SHY, "番茄钟被放弃了喵……下次坚持住哦");
        });

        JPanel foot = Ui.clear();
        foot.setLayout(new FlowLayout(FlowLayout.CENTER));
        foot.add(cancel);
        foot.setBorder(new EmptyBorder(8, 0, 2, 0));

        pomodoroWin.body.add(tip, BorderLayout.NORTH);
        pomodoroWin.body.add(pomodoroRing, BorderLayout.CENTER);
        pomodoroWin.body.add(foot, BorderLayout.SOUTH);

        pomodoroTimer = new Timer(1000, ev -> {
            remainSeconds--;
            if (remainSeconds <= 0) {
                pomodoroTimer.stop();
                pomodoroWin.dispose();
                Act.reset();                  // 番茄钟结束 → 收起键盘
                Growth.touch("pomodoro");
                Growth.addCoins(5);
                Emotion.trigger(Emotion.ACT_CUTE, "番茄钟完成喵！！+15 亲密度 +5 金币，休息 5 分钟~");
            } else {
                pomodoroRing.setText(String.format("%02d:%02d", remainSeconds / 60, remainSeconds % 60));
                pomodoroRing.setFrac(remainSeconds * 1.0 / (25 * 60));
                pomodoroRing.repaint();
            }
        });
        pomodoroTimer.setRepeats(true);
        pomodoroTimer.start();
        Act.play(Act.A.WORKING, 25 * 60000, false);   // 番茄钟开始 → 猫娘也打开键盘陪你写
        pomodoroWin.setVisible(true);
    }

    /** 圆环倒计时 */
    static class Ring extends JComponent {
        double frac = 1.0;         // 剩余比例
        String text = "25:00";
        String sub = "";

        Ring() { setOpaque(false); }

        void setFrac(double f) { frac = f; }
        void setText(String t) { text = t; repaint(); }
        void setSub(String s) { sub = s; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.aa(g);
            int w = getWidth(), h = getHeight();
            int s = Math.min(w, h) - 16;
            int x = (w - s) / 2, y = (h - s) / 2;
            int th = 13;

            g2.setStroke(new BasicStroke(th, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0xFF, 0xE7, 0xEF));
            g2.draw(new Arc2D.Float(x, y, s, s, 0, 360, Arc2D.OPEN));

            g2.setPaint(new GradientPaint(x, y, Ui.PINK, x + s, y + s, Ui.PURPLE));
            int sweep = (int) Math.round(360 * frac);
            if (sweep > 0) {
                g2.draw(new Arc2D.Float(x, y, s, s, 90, -sweep, Arc2D.OPEN));
            }

            g2.setFont(Ui.f(40, true));
            g2.setColor(Ui.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2,
                    h / 2 + (fm.getAscent() - fm.getDescent()) / 2);

            if (!sub.isEmpty()) {
                g2.setFont(Ui.f(12, false));
                g2.setColor(Ui.TEXT_SUB);
                fm = g2.getFontMetrics();
                g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 + 42);
            }
            g2.dispose();
        }
    }

    // ==================== 待办事项 ====================

    public static void showTodos() {
        final Ui.Win w = Ui.win("待办事项", 436, 470);

        final JPanel list = Ui.clear();
        list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));

        final List<String[]> ids = new ArrayList<String[]>();   // 行 → todo id

        final Runnable[] refreshRef = new Runnable[1];
        Runnable refresh = new Runnable() {
            public void run() {
                list.removeAll();
                ids.clear();
                List<String[]> todos = Db.todos();
                if (todos.isEmpty()) {
                    JLabel empty = Ui.label("还没有待办喵，加一条吧~", 12, false, Ui.TEXT_SUB);
                    empty.setAlignmentX(0.5f);
                    JPanel pad = Ui.clear();
                    pad.setLayout(new BorderLayout());
                    pad.setBorder(new EmptyBorder(30, 0, 0, 0));
                    pad.add(empty, BorderLayout.NORTH);
                    list.add(pad);
                }
                for (final String[] td : todos) {
                    ids.add(td);
                    final boolean done = "1".equals(td[2]);
                    list.add(todoRow(td[1], done,
                            new Runnable() { public void run() {
                                Db.toggleTodo(Integer.parseInt(td[0]));
                                refreshRef[0].run();
                            }},
                            new Runnable() { public void run() {
                                Db.removeTodo(Integer.parseInt(td[0]));
                                refreshRef[0].run();
                            }}));
                    list.add(Box.createVerticalStrut(6));
                }
                list.revalidate();
                list.repaint();
            }
        };
        refreshRef[0] = refresh;

        final JTextField input = Ui.input(16);
        input.getPreferredSize().height = 38;
        Ui.PetButton add = Ui.btn("添加", Ui.PRIMARY);
        add.setPreferredSize(new Dimension(90, 38));

        JPanel inputRow = Ui.clear();
        inputRow.setLayout(new BorderLayout(8, 0));
        inputRow.setBorder(new EmptyBorder(0, 0, 10, 0));
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(add, BorderLayout.EAST);

        Runnable addTodo = new Runnable() {
            public void run() {
                String text = input.getText().trim();
                if (!text.isEmpty()) {
                    Db.addTodo(text);
                    input.setText("");
                    input.requestFocusInWindow();
                    refresh.run();
                }
            }
        };
        add.addActionListener(e -> addTodo.run());
        input.addActionListener(e -> addTodo.run());

        JLabel hint = Ui.label("点圆圈 = 完成 / 取消完成 · 点 × = 删除", 10, false, Ui.TEXT_SUB);
        hint.setHorizontalAlignment(JLabel.CENTER);
        hint.setBorder(new EmptyBorder(6, 0, 2, 0));

        refresh.run();
        w.body.add(inputRow, BorderLayout.NORTH);
        w.body.add(Ui.scroll(list), BorderLayout.CENTER);
        w.body.add(hint, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /** 一行待办：圆圈勾选 + 文字 + 删除 */
    static JComponent todoRow(final String text, final boolean done,
                              final Runnable toggle, final Runnable del) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                g2.setColor(done ? new Color(0xFF, 0xF9, 0xFB) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(done ? new Color(0xFF, 0xEA, 0xF2) : new Color(0xFF, 0xE2, 0xEC));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 1.2f, getHeight() - 1.2f, 16, 16));
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 12, 9, 8));

        JButton check = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                int s = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - s) / 2, y = (getHeight() - s) / 2;
                if (done) {
                    g2.setPaint(new GradientPaint(0, 0, Ui.PINK, s, s, Ui.PINK_DEEP));
                    g2.fill(new Ellipse2D.Float(x, y, s, s));
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + s / 4, y + s / 2, x + s * 2 / 5, y + s * 3 / 4);
                    g2.drawLine(x + s * 2 / 5, y + s * 3 / 4, x + s * 3 / 4, y + s / 4);
                } else {
                    g2.setColor(new Color(0xFF, 0xD3, 0xE2));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.draw(new Ellipse2D.Float(x, y, s, s));
                }
                g2.dispose();
            }
        };
        check.setPreferredSize(new Dimension(24, 24));
        check.setContentAreaFilled(false);
        check.setBorderPainted(false);
        check.setFocusPainted(false);
        check.setOpaque(false);
        check.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        check.setToolTipText(done ? "取消完成" : "标记完成");
        check.addActionListener(e -> toggle.run());
        row.add(check, BorderLayout.WEST);

        JLabel label = Ui.label(done ? "<html><s>" + text + "</s></html>" : text, 13, false,
                done ? Ui.TEXT_SUB : Ui.TEXT);
        label.setBorder(new EmptyBorder(0, 2, 0, 0));
        row.add(label, BorderLayout.CENTER);

        JButton x = new JButton("×") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                g2.setColor(getModel().isRollover() ? Ui.BAD : new Color(0xD5, 0xC8, 0xCE));
                g2.setFont(Ui.f(14, false));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("×", (getWidth() - fm.stringWidth("×")) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        x.setPreferredSize(new Dimension(26, 24));
        x.setContentAreaFilled(false);
        x.setBorderPainted(false);
        x.setFocusPainted(false);
        x.setOpaque(false);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.setToolTipText("删除这条");
        x.addActionListener(e -> del.run());
        row.add(x, BorderLayout.EAST);
        return row;
    }
}
