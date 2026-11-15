package pet.game;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import pet.Db;
import pet.Emotion;
import pet.Growth;
import pet.Ui;

/**
 * 每日抽奖（阶段3）：每天第一次免费，之后 8 金币一次。
 * 奖池：5% 50金币 / 15% 20金币 / 30% 10金币 / 30% 5金币 / 20% 谢谢参与。
 * 界面：中间一块奖盘卡片，抽完直接在那个位置显示结果。
 */
public class Lottery {

    static final Random RND = new Random();

    public static void show() {
        final Ui.Win w = Ui.win("每日抽奖", 412, 386);
        final String today = java.time.LocalDate.now().toString();

        final JLabel head = Ui.label("今天的运气如何喵？", 15, true, Ui.TEXT);
        head.setHorizontalAlignment(JLabel.CENTER);
        final JLabel sub = Ui.label("每天第一次免费，之后 8 金币一次", 11, false, Ui.TEXT_SUB);
        sub.setHorizontalAlignment(JLabel.CENTER);

        JPanel headBox = Ui.clear();
        headBox.setLayout(new BorderLayout(0, 4));
        headBox.add(head, BorderLayout.NORTH);
        headBox.add(sub, BorderLayout.CENTER);
        headBox.setBorder(new EmptyBorder(2, 0, 10, 0));

        final PrizePanel prize = new PrizePanel();
        prize.big = "?";
        prize.subText = "点下面的按钮开始";

        final Ui.PetButton go = Ui.btn("抽 一 次", Ui.PRIMARY);
        go.setPreferredSize(new java.awt.Dimension(150, 42));

        JLabel pool = Ui.label("奖池   50 · 20 · 10 · 5 金币 · 谢谢参与", 11, false, Ui.TEXT_SUB);
        pool.setHorizontalAlignment(JLabel.CENTER);

        JPanel foot = Ui.clear();
        foot.setLayout(new BorderLayout(0, 6));
        JPanel btnRow = Ui.clear();
        btnRow.setLayout(new FlowLayout(FlowLayout.CENTER));
        btnRow.add(go);
        foot.add(btnRow, BorderLayout.NORTH);
        foot.add(pool, BorderLayout.CENTER);
        foot.setBorder(new EmptyBorder(10, 0, 2, 0));

        go.addActionListener(ev -> {
            boolean free = !today.equals(Db.get("lotto_date", ""));
            if (!free && !Growth.spend(8)) {
                head.setText("金币不够了喵……");
                sub.setText("先去签到或玩游戏赚一点吧");
                prize.big = "0";
                prize.subText = "余额不足";
                prize.tint = Ui.TEXT_SUB;
                prize.repaint();
                Emotion.trigger(Emotion.SLEEPY, "穷、穷喵……(捂脸)");
                return;
            }
            Db.set("lotto_date", today);
            int roll = RND.nextInt(100);
            int amount;
            String big, text;
            Color tint;
            if (roll < 5)        { amount = 50; big = "50";  text = "一等奖！蹦起来了喵！";  tint = Ui.GOLD; }
            else if (roll < 20)  { amount = 20; big = "20";  text = "二等奖，不错喵~";      tint = Ui.PINK_DEEP; }
            else if (roll < 50)  { amount = 10; big = "10";  text = "三等奖，收下吧喵";     tint = Ui.PURPLE; }
            else if (roll < 80)  { amount = 5;  big = "5";   text = "安慰奖……聊胜于无喵";  tint = new Color(0x6F, 0xA8, 0xFF); }
            else                 { amount = 0;  big = "0";   text = "谢谢参与喵（灰溜溜）"; tint = Ui.TEXT_SUB; }

            if (amount > 0) {
                Growth.addCoins(amount);
                Emotion.trigger(Emotion.HAPPY, "抽到 " + amount + " 金币喵！");
                head.setText(free ? "免费抽到 " + amount + " 金币喵！" : "花 8 金币抽到 " + amount + " 金币喵！");
            } else {
                Emotion.trigger(Emotion.ANGRY, "明天一定会抽到大奖的喵！");
                head.setText("这次没中喵……明天再来！");
            }
            sub.setText(free ? "（本次免费）" : "（本次花了 8 金币）· 余额 " + Growth.coins());
            prize.big = big;
            prize.subText = text;
            prize.tint = tint;
            prize.repaint();
        });

        w.body.add(headBox, BorderLayout.NORTH);
        w.body.add(prize, BorderLayout.CENTER);
        w.body.add(foot, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /** 奖盘卡片：中间一个大圆，显示结果数字 */
    static class PrizePanel extends JComponent {
        String big = "?";
        String subText = "";
        Color tint = Ui.PINK;

        PrizePanel() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.aa(g);
            int w = getWidth(), h = getHeight();

            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));
            g2.setPaint(new GradientPaint(0, 0,
                    new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 30),
                    0, h, new Color(255, 255, 255, 0)));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));
            g2.setColor(new Color(0xFF, 0xE2, 0xEC));
            g2.setStroke(new BasicStroke(1.3f));
            g2.draw(new RoundRectangle2D.Float(0.7f, 0.7f, w - 1.4f, h - 1.4f, 20, 20));

            int cx = w / 2, cy = h / 2 - 4;
            int r = Math.min(w, h) / 2 - 16;
            g2.setColor(new Color(0xFF, 0xF3, 0xF8));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            g2.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 120));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);

            g2.setFont(Ui.f(40, true));
            g2.setColor(tint);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(big, cx - fm.stringWidth(big) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);

            if (!subText.isEmpty()) {
                g2.setFont(Ui.f(12, false));
                g2.setColor(Ui.TEXT_SUB);
                fm = g2.getFontMetrics();
                g2.drawString(subText, cx - fm.stringWidth(subText) / 2, h - 14);
            }
            g2.dispose();
        }
    }
}
