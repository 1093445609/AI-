package pet.game;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import pet.Emotion;
import pet.Growth;
import pet.Ui;

/**
 * 反应力测试（阶段3）：点「开始」后随机 1~3 秒色块变红，
 * 变红之前就点 = 抢跑；变红之后点 = 计时。
 * 300ms 内 +10 金币，600ms 内 +5 金币。
 * 界面：一整块可以点的色块（比两个小按钮更有"拍下去"的实感）。
 */
public class ReactionTest {

    public static void show() {
        final Ui.Win w = Ui.win("反应力测试", 430, 412);

        JLabel tip = Ui.label("色块变红的一瞬间点它，动作要快喵", 12, false, Ui.TEXT_SUB);
        tip.setHorizontalAlignment(JLabel.CENTER);
        tip.setBorder(new EmptyBorder(0, 0, 8, 0));

        final int[] phase = {0};               // 0 待机 / 1 等待 / 2 已出信号 / 3 结果
        final long[] signalAt = {0};
        final Timer[] wait = new Timer[1];
        final Pad pad = new Pad();
        final Ui.PetButton start = Ui.btn("开 始", Ui.PRIMARY);

        final Runnable doClick = new Runnable() {
            public void run() {
                if (phase[0] == 1) {                       // 信号没出就点 = 抢跑
                    if (wait[0] != null) wait[0].stop();
                    phase[0] = 0;
                    pad.state(3, "抢跑啦！", "面壁 3 秒喵 · 点「开始」重来", Ui.BAD);
                    Emotion.trigger(Emotion.ANGRY, "都说了不要抢跑喵！");
                    start.setEnabled(true);
                    return;
                }
                if (phase[0] != 2) return;                 // 还没开始 / 已经出结果，忽略

                long ms = (System.nanoTime() - signalAt[0]) / 1000000;
                phase[0] = 3;
                String sub;
                Color tint;
                if (ms < 300) {
                    Growth.addCoins(10);
                    Growth.touch("game");
                    sub = "神仙反应！+10 金币喵！";
                    tint = Ui.OK;
                } else if (ms < 600) {
                    Growth.addCoins(5);
                    Growth.touch("game");
                    sub = "还不错喵！+5 金币~";
                    tint = Ui.PINK_DEEP;
                } else {
                    sub = "手速堪比 JVM 全量 GC（无奖励）";
                    tint = Ui.TEXT_SUB;
                }
                pad.state(3, ms + " ms", sub, tint);
                start.setEnabled(true);
                start.setText("再 来 一 次");
            }
        };

        pad.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                doClick.run();
            }
        });

        start.addActionListener(ev -> {
            start.setEnabled(false);
            phase[0] = 1;
            pad.state(1, "等…", "别抢跑喵！", Ui.PINK_DEEP);
            int delay = 1000 + new java.util.Random().nextInt(2000);
            wait[0] = new Timer(delay, e2 -> {
                signalAt[0] = System.nanoTime();
                phase[0] = 2;
                pad.state(2, "拍！", "现在！！", Color.WHITE);
            });
            wait[0].setRepeats(false);
            wait[0].start();
        });

        JPanel btnRow = Ui.clear();
        btnRow.setLayout(new FlowLayout(FlowLayout.CENTER));
        start.setPreferredSize(new java.awt.Dimension(150, 40));
        btnRow.add(start);
        btnRow.setBorder(new EmptyBorder(12, 0, 2, 0));

        w.body.add(tip, BorderLayout.NORTH);
        w.body.add(pad, BorderLayout.CENTER);
        w.body.add(btnRow, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /** 可点击的大色块 */
    static class Pad extends JComponent {
        int phase = 0;
        String big = "准备好了吗";
        String sub = "点下面「开始」，色块变红就点我";
        Color tint = Ui.PINK;

        Pad() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void state(int ph, String bigText, String subText, Color t) {
            phase = ph;
            big = bigText;
            sub = subText;
            tint = t;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.aa(g);
            int w = getWidth(), h = getHeight();

            if (phase == 2) {
                // 出信号：整块变成醒目的品红，视觉冲击最强
                g2.setPaint(new GradientPaint(0, 0, new Color(0xFF, 0x5C, 0x8A), 0, h, new Color(0xE0, 0x2E, 0x6B)));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 22, 22));
            } else {
                g2.setColor(phase == 1 ? new Color(0xFF, 0xF2, 0xF7) : Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 22, 22));
                g2.setPaint(new GradientPaint(0, 0,
                        new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), phase == 3 ? 26 : 0),
                        0, h, new Color(255, 255, 255, 0)));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 22, 22));
                g2.setColor(phase == 1 ? Ui.PINK_LIGHT : new Color(0xFF, 0xE2, 0xEC));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 1.6f, h - 1.6f, 22, 22));
            }

            g2.setFont(Ui.f(phase == 2 ? 46 : 30, true));
            g2.setColor(phase == 2 ? Color.WHITE : tint);
            FontMetrics fm = g2.getFontMetrics();
            int ty = h / 2 + (fm.getAscent() - fm.getDescent()) / 2 - 10;
            g2.drawString(big, (w - fm.stringWidth(big)) / 2, ty);

            if (sub != null && !sub.isEmpty()) {
                g2.setFont(Ui.f(12, false));
                g2.setColor(phase == 2 ? new Color(255, 255, 255, 220) : Ui.TEXT_SUB);
                fm = g2.getFontMetrics();
                g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, ty + 30);
            }
            g2.dispose();
        }
    }
}
