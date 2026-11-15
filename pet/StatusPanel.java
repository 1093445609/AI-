package pet;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * 状态面板（阶段2）
 * 右键菜单「我的猫娘」打开：头像/等级/亲密度进度条 + 六项统计 + 成就墙。
 */
public class StatusPanel {

    /** 打开面板（Pet 右键菜单调用） */
    public static void show() {
        final Ui.Win w = Ui.win("我的猫娘", 436, 520);

        // ============ 档案卡：头像 + 等级 + 进度条 ============
        JPanel profile = Ui.card();
        profile.setLayout(new BorderLayout(12, 0));

        JLabel avatar = new JLabel(Ui.catIcon(56));
        avatar.setVerticalAlignment(JLabel.CENTER);
        profile.add(avatar, BorderLayout.WEST);

        int lv = Growth.level();
        int need = lv * 100;
        int have = Growth.intimacy();
        int intoLv = have - (lv - 1) * 100;      // 本级已攒的亲密度

        JPanel mid = Ui.clear();
        mid.setLayout(new javax.swing.BoxLayout(mid, javax.swing.BoxLayout.Y_AXIS));
        JLabel name = Ui.label("喵酱", 16, true, Ui.TEXT);
        name.setAlignmentX(0f);
        JComponent bar = Ui.progress(intoLv, 100, 10);
        bar.setAlignmentX(0f);
        JLabel barText = Ui.label(String.format("Lv.%d → Lv.%d 还差 %d 点亲密度喵", lv, lv + 1, need - have),
                11, false, Ui.TEXT_SUB);
        barText.setAlignmentX(0f);
        mid.add(name);
        mid.add(Box.createVerticalStrut(5));
        mid.add(bar);
        mid.add(Box.createVerticalStrut(4));
        mid.add(barText);
        profile.add(mid, BorderLayout.CENTER);

        JPanel east = Ui.clear();
        east.setLayout(new BorderLayout());
        east.add(Ui.chip("亲密度 " + have, Ui.PINK_DEEP, Ui.PINK_PALE), BorderLayout.NORTH);
        east.add(Ui.chip("金币 " + Growth.coins(), new Color(0xB5, 0x7C, 0x14), new Color(0xFF, 0xF3, 0xD6)),
                BorderLayout.SOUTH);
        profile.add(east, BorderLayout.EAST);

        // ============ 统计卡 ============
        JPanel stats = new JPanel(new GridLayout(2, 3, 8, 8));
        stats.setOpaque(false);
        stats.add(statCell(String.valueOf(Db.getInt("touch_total", 0)), "累计摸头"));
        stats.add(statCell(String.valueOf(Db.chatCount()), "累计聊天"));
        stats.add(statCell(String.valueOf(Db.getInt("games_won", 0)), "游戏胜利"));
        stats.add(statCell(String.valueOf(Db.getInt("pomodoros", 0)), "番茄钟"));
        stats.add(statCell(String.valueOf(Db.getInt("quiz_correct", 0)), "答对面试题"));
        stats.add(statCell(Db.signCount() + " 天", "累计签到"));

        JPanel statsBox = Ui.card();
        statsBox.setLayout(new BorderLayout(0, 8));
        statsBox.add(sectionTitle("小账本"), BorderLayout.NORTH);
        statsBox.add(stats, BorderLayout.CENTER);

        // ============ 成就墙 ============
        JPanel ach = Ui.card();
        ach.setLayout(new BorderLayout(0, 6));
        int gotCount = Db.achievements().size();
        ach.add(sectionTitle("成就墙 " + gotCount + " / " + Growth.ACH.size()), BorderLayout.NORTH);

        JPanel rows = Ui.clear();
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        for (Map.Entry<String, String[]> e : Growth.ACH.entrySet()) {
            boolean ok = Db.hasAchievement(e.getKey());
            rows.add(achRow(e.getValue()[0], e.getValue()[1], ok));
            rows.add(Box.createVerticalStrut(4));
        }
        ach.add(rows, BorderLayout.CENTER);

        // ============ 组装 ============
        JPanel stack = Ui.clear();
        stack.setLayout(new javax.swing.BoxLayout(stack, javax.swing.BoxLayout.Y_AXIS));
        stack.add(profile);
        stack.add(Box.createVerticalStrut(10));
        stack.add(statsBox);
        stack.add(Box.createVerticalStrut(10));
        stack.add(ach);
        stack.add(Box.createVerticalStrut(4));

        JButton close = Ui.btn("知道啦", Ui.GHOST);
        close.addActionListener(ev -> w.dispose());
        JPanel foot = Ui.clear();
        foot.setLayout(new FlowLayout(FlowLayout.CENTER));
        foot.add(close);
        foot.setBorder(new EmptyBorder(6, 0, 2, 0));

        w.body.add(Ui.scroll(stack), BorderLayout.CENTER);
        w.body.add(foot, BorderLayout.SOUTH);
        w.setVisible(true);
    }

    /** 小节标题 */
    static JComponent sectionTitle(String text) {
        JLabel l = Ui.label(text, 12, true, Ui.PINK_DEEP);
        l.setBorder(new EmptyBorder(0, 2, 0, 2));
        return l;
    }

    /** 一格统计：大数字 + 小标签 */
    static JComponent statCell(final String num, final String name) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                g2.setColor(new Color(0xFF, 0xF6, 0xFA));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(new Color(0xFF, 0xE6, 0xF0));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 4, 8, 4));
        JLabel n = Ui.label(num, 20, true, Ui.PINK_DEEP);
        n.setHorizontalAlignment(JLabel.CENTER);
        JLabel l = Ui.label(name, 11, false, Ui.TEXT_SUB);
        l.setHorizontalAlignment(JLabel.CENTER);
        p.add(n, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    /** 一行成就 */
    static JComponent achRow(String name, String bonus, final boolean got) {
        JPanel p = Ui.clear();
        p.setLayout(new BorderLayout(8, 0));
        p.setBorder(new EmptyBorder(3, 6, 3, 6));

        JLabel mark = Ui.label(got ? "√" : "○", 14, true, got ? Ui.OK : new Color(0xD8, 0xCC, 0xD2));
        p.add(mark, BorderLayout.WEST);

        JLabel n = Ui.label(name, 12, got, got ? Ui.TEXT : Ui.TEXT_SUB);
        p.add(n, BorderLayout.CENTER);

        JLabel b = Ui.label(got ? "已解锁" : "+" + bonus + " 金币", 10, false, got ? Ui.OK : Ui.TEXT_SUB);
        p.add(b, BorderLayout.EAST);
        return p;
    }
}
