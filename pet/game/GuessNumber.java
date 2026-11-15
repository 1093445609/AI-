package pet.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import pet.Emotion;
import pet.Growth;
import pet.Ui;

/**
 * 猜数字（阶段3）：1~100，最多 7 次，赢了 +20 金币。
 * 界面：猫娘对话气泡 + 圆角输入框 + 剩余次数小圆点 + 猜测记录。
 */
public class GuessNumber {

    public static void show() {
        final Ui.Win w = Ui.win("猜数字", 404, 348);

        final int answer = new Random().nextInt(100) + 1;
        final int[] left = {7};
        final List<String> history = new ArrayList<String>();

        final JLabel tip = Ui.label("我已经想好了一个 1~100 的数字喵，猜猜看？", 14, true, Ui.TEXT);
        tip.setHorizontalAlignment(JLabel.CENTER);
        JLabel subTip = Ui.label("每猜一次我会告诉你大了还是小了", 11, false, Ui.TEXT_SUB);
        subTip.setHorizontalAlignment(JLabel.CENTER);

        JPanel head = Ui.clear();
        head.setLayout(new BorderLayout(0, 4));
        head.add(tip, BorderLayout.NORTH);
        head.add(subTip, BorderLayout.CENTER);
        head.setBorder(new EmptyBorder(2, 0, 10, 0));

        final JTextField input = Ui.input(5);
        input.setHorizontalAlignment(JTextField.CENTER);
        input.setFont(Ui.f(18, true));
        input.setPreferredSize(new Dimension(110, 44));
        final Ui.PetButton go = Ui.btn("猜！", Ui.PRIMARY);
        go.setPreferredSize(new Dimension(96, 44));

        JPanel row = Ui.clear();
        row.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.add(input);
        row.add(go);

        // 剩余次数：7 个圆点，用掉一个灭一个
        final JComponent dots = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                int n = 7, gap = 16, r = 7;
                int totalW = n * gap;
                int x0 = (getWidth() - totalW) / 2 + gap / 2;
                int y = getHeight() / 2;
                for (int i = 0; i < n; i++) {
                    boolean alive = i < left[0];
                    g2.setColor(alive ? Ui.PINK : new Color(0xF0, 0xE4, 0xEA));
                    g2.fillOval(x0 + i * gap - r / 2, y - r / 2, r, r);
                }
                g2.dispose();
            }
        };
        dots.setOpaque(false);
        dots.setPreferredSize(new Dimension(10, 26));

        final JLabel hist = Ui.label(" ", 11, false, Ui.TEXT_SUB);
        hist.setHorizontalAlignment(JLabel.CENTER);
        JLabel leftLabel = Ui.label("还剩 7 次机会", 11, false, Ui.PINK_DEEP);
        leftLabel.setHorizontalAlignment(JLabel.CENTER);

        final JPanel center = Ui.clear();
        center.setLayout(new BorderLayout(0, 6));
        center.add(row, BorderLayout.NORTH);
        center.add(dots, BorderLayout.CENTER);
        center.add(leftLabel, BorderLayout.SOUTH);

        final JPanel foot = Ui.clear();
        foot.setLayout(new BorderLayout(0, 4));
        foot.add(hist, BorderLayout.CENTER);
        foot.setBorder(new EmptyBorder(10, 0, 2, 0));

        Runnable doGuess = new Runnable() {
            public void run() {
                if (!go.isEnabled()) return;
                int guess;
                try {
                    guess = Integer.parseInt(input.getText().trim());
                } catch (NumberFormatException ex) {
                    tip.setText("要输入数字喵！");
                    return;
                }
                if (guess < 1 || guess > 100) {
                    tip.setText("1~100 之间的数字喵~");
                    return;
                }
                left[0]--;
                input.setText("");
                input.requestFocusInWindow();

                if (guess == answer) {
                    tip.setText("猜对啦！答案就是 " + answer + " 喵！");
                    subTip.setText("奖励 +20 金币，主人好厉害~");
                    leftLabel.setText("完美收官 · 用了 " + (7 - left[0]) + " 次");
                    Growth.addCoins(20);
                    Growth.touch("game");
                    Emotion.trigger(Emotion.ACT_CUTE, "主人好聪明喵！+20 金币~");
                    go.setEnabled(false);
                    input.setEnabled(false);
                    dots.repaint();
                    return;
                }

                history.add(guess + (guess < answer ? "↑" : "↓"));
                StringBuilder sb = new StringBuilder("猜过：");
                for (int i = 0; i < history.size(); i++) {
                    if (i > 0) sb.append("  ");
                    sb.append(history.get(i));
                }
                hist.setText(sb.toString());

                if (left[0] <= 0) {
                    tip.setText("次数用完啦，答案是 " + answer + " 喵");
                    subTip.setText("再来一局吧？这次一定行！");
                    leftLabel.setText("本局结束");
                    Emotion.trigger(Emotion.HAPPY, "差点就被猜到了喵！");
                    go.setEnabled(false);
                    input.setEnabled(false);
                } else {
                    tip.setText(guess < answer ? "太小了喵，往大猜！" : "太大了喵，往小猜！");
                    subTip.setText("还差一点点哦");
                    leftLabel.setText("还剩 " + left[0] + " 次机会");
                }
                dots.repaint();
            }
        };

        go.addActionListener(e -> doGuess.run());
        input.addActionListener(e -> doGuess.run());

        JPanel main = Ui.card();
        main.add(center, BorderLayout.CENTER);

        w.body.add(head, BorderLayout.NORTH);
        w.body.add(main, BorderLayout.CENTER);
        w.body.add(foot, BorderLayout.SOUTH);
        w.setVisible(true);
        input.requestFocusInWindow();
    }
}
