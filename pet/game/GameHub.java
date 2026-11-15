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
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pet.Growth;
import pet.Ui;

/**
 * 游戏中心（阶段3）：四个小游戏的入口。
 * 二次元卡片式：一卡一游戏，粉色描边 + 圆角图标，鼠标悬停整张卡亮起来。
 */
public class GameHub {

    public static void show() {
        final Ui.Win w = Ui.win("游戏中心", 456, 400);

        // 顶部：金币 + 小提示
        JPanel head = Ui.clear();
        head.setLayout(new BorderLayout());
        JLabel coins = Ui.chip("金币 " + Growth.coins(), new Color(0xB5, 0x7C, 0x14),
                new Color(0xFF, 0xF3, 0xD6));
        head.add(coins, BorderLayout.WEST);
        JLabel lv = Ui.label("Lv." + Growth.level() + " · 亲密度 " + Growth.intimacy(),
                11, false, Ui.TEXT_SUB);
        head.add(lv, BorderLayout.EAST);
        head.setBorder(new javax.swing.border.EmptyBorder(0, 2, 8, 2));

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.add(card("猜数字", "1~100 · 7 次机会", Ui.GLYPH_ASK, Ui.PINK, new Runnable() {
            public void run() { GuessNumber.show(); }
        }));
        grid.add(card("石头剪刀布", "赢一局 +5 金币", Ui.GLYPH_RPS, Ui.PURPLE, new Runnable() {
            public void run() { RpsGame.show(); }
        }));
        grid.add(card("每日抽奖", "每天一次免费", Ui.GLYPH_GIFT, Ui.GOLD, new Runnable() {
            public void run() { Lottery.show(); }
        }));
        grid.add(card("反应力测试", "300ms 内 +10 金币", Ui.GLYPH_BOLT, new Color(0x6F, 0xA8, 0xFF), new Runnable() {
            public void run() { ReactionTest.show(); }
        }));

        JPanel tipRow = Ui.clear();
        tipRow.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel tip = Ui.label("赢了涨金币和亲密度喵~", 11, false, Ui.TEXT_SUB);
        tipRow.add(tip);

        w.body.add(head, BorderLayout.NORTH);
        w.body.add(grid, BorderLayout.CENTER);
        w.body.add(tipRow, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /** 一张游戏卡片：圆角白卡 + 左侧圆形图标 + 标题/副标题 */
    static JComponent card(final String title, final String sub, final int glyph,
                           final Color tint, final Runnable onClick) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                int wd = getWidth(), ht = getHeight();
                boolean hov = getModel().isRollover();
                int oy = getModel().isPressed() ? 1 : 0;

                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, oy, wd, ht - 1, 20, 20));
                if (hov) {
                    g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 26));
                    g2.fill(new RoundRectangle2D.Float(0, oy, wd, ht - 1, 20, 20));
                }
                g2.setColor(hov ? Ui.PINK : new Color(0xFF, 0xE2, 0xEC));
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(new RoundRectangle2D.Float(0.7f, oy + 0.7f, wd - 1.4f, ht - 2.4f, 20, 20));

                int cx = 38, cy = ht / 2;
                g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 34));
                g2.fillOval(cx - 23, cy - 23, 46, 46);
                Ui.glyph(g2, glyph, cx, cy, 24, tint);

                g2.setFont(Ui.f(14, true));
                g2.setColor(Ui.TEXT);
                g2.drawString(title, 70, cy - 1);
                g2.setFont(Ui.f(11, false));
                g2.setColor(Ui.TEXT_SUB);
                g2.drawString(sub, 70, cy + 17);
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(180, 110));
        b.addActionListener(e -> onClick.run());
        return b;
    }
}
