package pet.game;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import pet.Emotion;
import pet.Growth;
import pet.Ui;

/**
 * 石头剪刀布（阶段3）：赢了 +5 金币，可连续玩。
 * 界面：三个大手势卡片 + 中间对战结果 + 底部比分。
 */
public class RpsGame {

    static final Random RND = new Random();
    static final String[] NAMES = {"石头", "剪刀", "布"};

    public static void show() {
        final Ui.Win w = Ui.win("石头剪刀布", 424, 402);

        final int[] win = {0}, lose = {0}, draw = {0};

        final JLabel result = Ui.label("选一个出手喵！", 16, true, Ui.TEXT);
        result.setHorizontalAlignment(JLabel.CENTER);
        final JLabel detail = Ui.label("石头 赢 剪刀 · 剪刀 赢 布 · 布 赢 石头", 11, false, Ui.TEXT_SUB);
        detail.setHorizontalAlignment(JLabel.CENTER);

        JPanel head = Ui.clear();
        head.setLayout(new BorderLayout(0, 5));
        head.add(result, BorderLayout.NORTH);
        head.add(detail, BorderLayout.CENTER);
        head.setBorder(new EmptyBorder(2, 0, 12, 0));

        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);

        final JLabel score = Ui.label("胜 0   ·   负 0   ·   平 0", 12, true, Ui.PINK_DEEP);
        score.setHorizontalAlignment(JLabel.CENTER);
        final JLabel coins = Ui.label("金币 " + Growth.coins(), 11, false, Ui.TEXT_SUB);
        coins.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < 3; i++) {
            final int mine = i;
            row.add(handCard(mine, new Runnable() {
                public void run() {
                    int cat = RND.nextInt(3);
                    if (mine == cat) {
                        draw[0]++;
                        result.setForeground(Ui.TEXT);
                        result.setText("平局喵！都是" + NAMES[mine]);
                        detail.setText("再来一局分个高下~");
                    } else if ((mine + 1) % 3 == cat) {   // 石头>剪刀>布>石头
                        win[0]++;
                        Growth.addCoins(5);
                        Growth.touch("game");
                        result.setForeground(Ui.OK);
                        result.setText("你出" + NAMES[mine] + "，我出" + NAMES[cat] + "，你赢啦！");
                        detail.setText("+5 金币喵~");
                    } else {
                        lose[0]++;
                        result.setForeground(Ui.BAD);
                        result.setText("你出" + NAMES[mine] + "，我出" + NAMES[cat] + "，我赢喵嘿嘿");
                        detail.setText("再来一局，这次让你赢~");
                    }
                    score.setText("胜 " + win[0] + "   ·   负 " + lose[0] + "   ·   平 " + draw[0]);
                    coins.setText("金币 " + Growth.coins());
                }
            }));
        }

        JPanel foot = Ui.clear();
        foot.setLayout(new BorderLayout(0, 3));
        foot.add(score, BorderLayout.NORTH);
        foot.add(coins, BorderLayout.CENTER);
        foot.setBorder(new EmptyBorder(12, 0, 2, 0));

        w.body.add(head, BorderLayout.NORTH);
        w.body.add(row, BorderLayout.CENTER);
        w.body.add(foot, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /** 一张出拳卡片 */
    static JComponent handCard(final int type, final Runnable onClick) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                int wd = getWidth(), ht = getHeight();
                boolean hov = getModel().isRollover();
                int oy = getModel().isPressed() ? 2 : 0;
                Color tint = type == 0 ? Ui.PINK : (type == 1 ? Ui.PURPLE : new Color(0x6F, 0xA8, 0xFF));

                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, oy, wd, ht - 2, 20, 20));
                if (hov) {
                    g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 26));
                    g2.fill(new RoundRectangle2D.Float(0, oy, wd, ht - 2, 20, 20));
                }
                g2.setColor(hov ? tint : new Color(0xFF, 0xE2, 0xEC));
                g2.setStroke(new BasicStroke(hov ? 1.8f : 1.3f));
                g2.draw(new RoundRectangle2D.Float(0.8f, oy + 0.8f, wd - 1.6f, ht - 3.6f, 20, 20));

                hand(g2, type, wd / 2, ht / 2 - 8, 46, tint);
                g2.setFont(Ui.f(13, true));
                g2.setColor(Ui.TEXT);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(NAMES[type], (wd - fm.stringWidth(NAMES[type])) / 2, ht - 16 + oy);
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 118));
        b.addActionListener(e -> onClick.run());
        return b;
    }

    /** 手势图标：石头=实心圆，剪刀=V 形，布=三根指头 */
    static void hand(Graphics2D g2, int type, int cx, int cy, int size, Color c) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(c);
        int r = size / 2;
        if (type == 0) {
            g.fill(new Ellipse2D.Float(cx - r, cy - r, size, size));
            g.setColor(new Color(255, 255, 255, 70));
            g.fill(new Ellipse2D.Float(cx - r + size * 0.18f, cy - r + size * 0.14f, size * 0.3f, size * 0.26f));
        } else if (type == 1) {
            g.setStroke(new BasicStroke(size * 0.16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - (int) (size * 0.3), cy - r, cx, cy + (int) (size * 0.22));
            g.drawLine(cx + (int) (size * 0.3), cy - r, cx, cy + (int) (size * 0.22));
            g.setStroke(new BasicStroke(size * 0.13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - (int) (size * 0.1), cy + (int) (size * 0.3), cx - (int) (size * 0.1), cy + r);
            g.drawLine(cx + (int) (size * 0.1), cy + (int) (size * 0.3), cx + (int) (size * 0.1), cy + r);
        } else {
            g.setStroke(new BasicStroke(size * 0.14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -1; i <= 1; i++) {
                int x = cx + i * (int) (size * 0.26);
                g.drawLine(x, cy - r + Math.abs(i) * (int) (size * 0.12), x, cy + r - (int) (size * 0.1));
            }
            g.setStroke(new BasicStroke(size * 0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - (int) (size * 0.28), cy + (int) (size * 0.26),
                    cx + (int) (size * 0.28), cy + (int) (size * 0.26));
        }
        g.dispose();
    }
}
