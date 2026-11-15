package pet;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Desktop;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import pet.Emotion;
import pet.Growth;

/**
 * Java 面试题练习（阶段4）
 *
 * 题库不再写死在代码里：从 data/quiz.txt 读取，改完文件再点一次菜单就是新题库。
 * 文件不存在时自动把下面这份内置题库导出成模板，所以删了也不会炸。
 *
 * 文件格式（一行一题，竖线分隔）：
 * 题目 | 参考答案 | 关键词1,关键词2,关键词3
 * 关键词可以省略，省略时这道题「写了答案就算对」，不判分。
 */
public final class QuizBank {

    static final Random RND = new Random();

    /** 题库文件，相对于项目根目录 */
    public static final String FILE = "data/quiz.txt";

    /** 上一次抽到的题号，避免连抽同一题 */
    private static int lastIdx = -1;

    /** 一道题：题目 / 参考答案 / 判分关键词（回答里含任意一个就算对；keys 为空=不判分） */
    public static class Q {
        public final String ask, ref;
        public final String[] keys;

        public Q(String ask, String ref, String... keys) {
            this.ask = ask;
            this.ref = ref;
            this.keys = keys == null ? new String[0] : keys;
        }
    }

    /**
     * 内置题库：外部文件读不到、或被改坏时兜底用，也是自动生成的模板内容。
     * 题目全部出自主人 Day1~Day5 学过的知识点。
     */
    private static final Q[] BUILTIN = {
        new Q("HashMap 为什么容量一定是 2 的幂？",
              "为了用 (n-1) & hash 位运算代替取模：容量是 2 的幂时，n-1 的二进制低位全 1，与运算等价于 hash % n，速度快且散列均匀。",
              "2的幂", "位运算", "取模", "与运算", "(n-1)"),
        new Q("JDK8 HashMap 扩容后，元素怎么决定留在原位还是搬到新位置？",
              "看 (e.hash & oldCap) 是否为 0：为 0 留在原下标 i，不为 0 搬到 i + oldCap，一条链表会被拆成高低位两条。",
              "oldcap", "hash & oldcap", "高位", "原位", "翻倍"),
        new Q("new HashMap<>(100000) 传十万，实际初始容量是多少？",
              "tableSizeFor 会把容量抬到不小于「期望容量/0.75」的最小 2 的幂：ceil(100000/0.75)=133334 → 2^18 = 262144。",
              "262144", "2^18", "tablesizefor", "18次方"),
        new Q("ArrayList 默认扩容是多少倍？怎么扩的？",
              "1.5 倍：newCapacity = oldCapacity + (oldCapacity >> 1)，10 → 15 → 22，用 Arrays.copyOf 整体复制。",
              "1.5", "oldcapacity >> 1"),
        new Q("ArrayList 的 modCount 字段是干什么用的？",
              "记录结构性修改次数。迭代器遍历时对比自己的 expectedModCount，不一致就抛 ConcurrentModificationException（fail-fast 快速失败机制）。",
              "fail-fast", "快速失败", "并发修改", "modcount不一致", "concurrentmodification"),
        new Q("如果 try 里已经 return 了，finally 还会执行吗？finally 里的 return 会怎样？",
              "finally 一定执行。finally 里的 return 会覆盖 try 里的返回值（这是大坑），所以禁止在 finally 里写 return。",
              "覆盖", "吞掉", "会执行", "一定执行"),
        new Q("try-with-resources 多个资源时，关闭的顺序是什么？",
              "逆序关闭：后声明的先关，正好和声明顺序相反，保证依赖资源先释放。",
              "逆序", "倒序", "反序", "相反"),
        new Q("String s = \"a\" + \"b\"，编译之后 s 是什么？",
              "编译期常量折叠，直接变成 s = \"ab\"，进字符串常量池，和 String s = \"ab\" 完全一样。",
              "常量折叠", "常量池", "编译期", "\"ab\""),
        new Q("Error 和 Exception 有什么区别？",
              "都是 Throwable 的子类。Error 是程序无法处理的严重错误（OutOfMemoryError、StackOverflowError），一般不捕获；Exception 是程序可以处理，分 checked（编译期强制处理）和 unchecked（RuntimeException）。",
              "无法处理", "不能处理", "严重", "oome", "堆内存"),
        new Q("为什么重写 equals 必须同时重写 hashCode？",
              "hashCode 约定：equals 相等的对象必须有相同 hashCode。不重写的话，两个「相等」对象 hashCode 不同，放进 HashSet/HashMap 会被当成两个不同的元素。",
              "hashcode", "hash", "约定", "set", "map"),
        new Q("(n-1) & hash 这个位运算等价于哪种算术运算？前提是什么？",
              "等价于 hash % n（对容量取模），前提是容量 n 是 2 的幂。",
              "取模", "模运算", "%"),
        new Q("HashMap 链表转红黑树需要满足哪两个条件？",
              "链表长度达到 8，并且 table 数组长度 ≥ 64（不满足就先扩容而不是树化）。",
              "8", "64", "树化", "红黑树"),
        new Q("checked 异常和 unchecked 异常的区别是什么？",
              "checked 异常编译期强制处理（必须 try/catch 或 throws），如 IOException；unchecked 是 RuntimeException 及其子类，编译器不强制，如 NullPointerException。",
              "编译", "runtime", "运行时"),
        new Q("为什么 String 拼接用 StringBuilder 比 + 快？",
              "String 不可变，+ 每次都 new 新对象再复制内容，循环拼接会产生大量临时对象；StringBuilder 在内部 char[] 上原地追加，扩容才复制。",
              "不可变", "新对象", "复制", "临时对象"),
    };

    /** 题库文件绝对路径 */
    public static File quizFile() {
        return new File(System.getProperty("user.dir"), FILE);
    }

    /**
     * 加载题库：优先 data/quiz.txt；文件不存在就先导出模板；解析不出题目则退回内置。
     * 每次点菜单都会重新读，所以改完文件不用重启桌宠。
     */
    public static List<Q> load() {
        File f = quizFile();
        if (!f.isFile()) {
            dumpTemplate(f);
        }
        List<Q> list = new ArrayList<Q>();
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            list = parse(lines);
        } catch (Exception e) {
            System.out.println("[题库] 读取 " + FILE + " 失败：" + e);
        }
        if (list.isEmpty()) {
            for (Q q : BUILTIN) list.add(q);
            System.out.println("[题库] " + FILE + " 没解析出有效题目（每行要「题目|参考答案|关键词」），已改用内置题库");
        } else {
            System.out.println("[题库] 已加载 " + list.size() + " 题（" + f.getAbsolutePath() + "）");
        }
        return list;
    }

    /** 解析题库文本：跳过空行与 #、// 注释；一段坏行只跳过自己，不影响其他题 */
    static List<Q> parse(List<String> lines) {
        List<Q> list = new ArrayList<Q>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);   // 去掉记事本加的 BOM
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

            String[] p = line.split("\\|");
            if (p.length < 2) {
                System.out.println("[题库] 第 " + (i + 1) + " 行缺少竖线，已跳过：" + line);
                continue;
            }
            String ask = p[0].trim();
            String ref = p[1].trim();
            if (ask.isEmpty() || ref.isEmpty()) {
                System.out.println("[题库] 第 " + (i + 1) + " 行题目或答案为空，已跳过");
                continue;
            }

            List<String> ks = new ArrayList<String>();
            for (int k = 2; k < p.length; k++) {
                for (String s : p[k].split("[,，;；]")) {
                    s = s.trim();
                    if (!s.isEmpty()) ks.add(s);
                }
            }
            list.add(new Q(ask, ref, ks.toArray(new String[ks.size()])));
        }
        return list;
    }

    /** 把内置题库导出成模板文件（UTF-8 带 BOM，记事本打开不乱码） */
    static void dumpTemplate(File f) {
        File dir = f.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();

        StringBuilder sb = new StringBuilder();
        sb.append("# ===== 桌宠 Java 面试题库（随便改，改完再点一次菜单就生效，不用重启）=====\n");
        sb.append("#\n");
        sb.append("# 格式：一行一题，用竖线 | 分成三段\n");
        sb.append("#       题目 | 参考答案 | 关键词1,关键词2,关键词3\n");
        sb.append("#\n");
        sb.append("# 规则：\n");
        sb.append("#   1. 以 # 开头的行是注释，空行忽略；写错了只跳过那一行，不会炸；\n");
        sb.append("#   2. 关键词用逗号隔开（中文逗号「，」也认），回答里【含任意一个】就算答对；\n");
        sb.append("#   3. 只写「题目|参考答案」两段也行，这种题写了答案就算对（不判分）；\n");
        sb.append("#   4. 文件请存成 UTF-8 编码，用记事本 / VS Code 直接改，别用 Word 另存；\n");
        sb.append("#   5. 关键词不区分大小写，写 oldcap 和 OldCap 一个效果。\n");
        sb.append("#\n");
        sb.append("# 下面 14 题是内置题库，可以删、可以改，也可以继续往下加自己的题。\n\n");

        for (Q q : BUILTIN) {
            sb.append(q.ask).append(" | ").append(q.ref).append(" | ")
              .append(String.join(",", q.keys)).append('\n');
        }

        try {
            byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
            byte[] all = new byte[body.length + 3];
            all[0] = (byte) 0xEF;
            all[1] = (byte) 0xBB;
            all[2] = (byte) 0xBF;
            System.arraycopy(body, 0, all, 3, body.length);
            Files.write(f.toPath(), all);
            System.out.println("[题库] 已生成题库模板：" + f.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[题库] 生成模板失败：" + e);
        }
    }

    /** 菜单「编辑题库」调用：直接用系统默认编辑器打开 txt */
    public static void openFile() {
        File f = quizFile();
        if (!f.isFile()) dumpTemplate(f);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                Desktop.getDesktop().edit(f);
                return;
            }
        } catch (Exception ignored) {
            // 落到下面的备选方案
        }
        try {
            new ProcessBuilder("notepad.exe", f.getAbsolutePath()).start();
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "打不开编辑器，请手动打开这个文件改：\n" + f.getAbsolutePath());
        }
    }

    /** 随机抽一题，尽量不连续抽到同一道 */
    static int pick(int n) {
        if (n <= 1) return 0;
        int i;
        do {
            i = RND.nextInt(n);
        } while (i == lastIdx);
        lastIdx = i;
        return i;
    }

    public static void show() {
        final List<Q> bank = load();
        if (bank.isEmpty()) return;
        final int idx = pick(bank.size());
        final Q q = bank.get(idx);

        final Ui.Win w = Ui.win("Java 面试题练习", 500, 520);

        // ============ 顶部：题号 + 奖励提示 ============
        JPanel head = Ui.clear();
        head.setLayout(new BorderLayout(8, 0));
        head.setBorder(new javax.swing.border.EmptyBorder(0, 2, 8, 2));
        head.add(Ui.chip("第 " + (idx + 1) + " / " + bank.size() + " 题", Ui.PINK_DEEP, Ui.PINK_PALE),
                BorderLayout.WEST);
        JLabel reward = Ui.label("答对 +8 亲密度 +3 金币喵", 11, false, Ui.TEXT_SUB);
        head.add(reward, BorderLayout.EAST);

        // ============ 题目卡 ============
        JPanel qCard = Ui.card();
        qCard.setLayout(new BorderLayout(0, 8));
        qCard.add(Ui.label("面试官喵酱：", 11, false, Ui.TEXT_SUB), BorderLayout.NORTH);
        JLabel ask = Ui.label("<html><body style='width:380px;line-height:1.6'>"
                + q.ask + "</body></html>", 15, true, Ui.TEXT);
        qCard.add(ask, BorderLayout.CENTER);

        // ============ 答题区（圆角输入框） ============
        final javax.swing.JTextArea answer = new javax.swing.JTextArea(4, 24);
        answer.setOpaque(false);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        answer.setFont(Ui.f(13, false));
        answer.setForeground(Ui.TEXT);
        answer.setCaretColor(Ui.PINK_DEEP);
        answer.setBorder(new javax.swing.border.EmptyBorder(8, 10, 8, 10));

        JPanel inputBox = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = Ui.aa(g);
                g2.setColor(Color.WHITE);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                boolean focus = answer.hasFocus();
                g2.setColor(focus ? Ui.PINK : Ui.PINK_LIGHT);
                g2.setStroke(new BasicStroke(focus ? 1.8f : 1.2f));
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(0.7f, 0.7f,
                        getWidth() - 1.4f, getHeight() - 1.4f, 16, 16));
                g2.dispose();
            }
        };
        inputBox.setOpaque(false);
        inputBox.add(Ui.scroll(answer), BorderLayout.CENTER);

        JPanel inputPad = Ui.clear();
        inputPad.setLayout(new BorderLayout());
        inputPad.setBorder(new javax.swing.border.EmptyBorder(2, 2, 2, 2));
        inputPad.add(inputBox, BorderLayout.CENTER);

        JLabel tip = Ui.label(q.keys.length == 0
                ? "这题没配关键词，写了就算对喵（想判分就在 quiz.txt 里补关键词）"
                : "用自己的话答，关键词命中就算对喵", 10, false, Ui.TEXT_SUB);

        // ============ 结果区（提交后就地显示，不再弹灰框） ============
        final JLabel verdict = Ui.label(" ", 13, true, Ui.TEXT);
        final JLabel ref = Ui.label("<html><body style='width:380px;color:#9C8A96'>"
                + "提交答案后，参考答案会出现在这里喵</body></html>", 12, false, Ui.TEXT_SUB);

        JPanel resBox = Ui.card();
        resBox.setLayout(new BorderLayout(0, 6));
        resBox.add(verdict, BorderLayout.NORTH);
        resBox.add(ref, BorderLayout.CENTER);

        // ============ 按钮 ============
        final Ui.PetButton submit = Ui.btn("提交答案", Ui.PRIMARY);
        Ui.PetButton next = Ui.btn("换一题", Ui.GHOST);
        Ui.PetButton edit = Ui.btn("编辑题库", Ui.SOFT);
        next.addActionListener(ev -> {
            w.dispose();
            show();
        });
        edit.addActionListener(ev -> openFile());

        submit.addActionListener(ev -> {
            String ans = answer.getText().trim().toLowerCase();
            if (ans.isEmpty()) {
                verdict.setText("一个字都不写喵？快写！");
                verdict.setForeground(Ui.BAD);
                return;
            }
            boolean ok = q.keys.length == 0;   // 没配关键词：写了就算对
            for (String k : q.keys) {
                if (ans.contains(k.toLowerCase())) { ok = true; break; }
            }
            if (ok) {
                Growth.touch("quiz");
                Growth.addCoins(3);
                verdict.setText("✔ 答对了喵！+8 亲密度 +3 金币");
                verdict.setForeground(Ui.OK);
                Emotion.trigger(Emotion.ACT_CUTE, "主人答对面试题了喵！不愧是我带的~");
            } else {
                verdict.setText("✘ 没答到点上喵，记住这道题~");
                verdict.setForeground(Ui.BAD);
                Emotion.trigger(Emotion.HAPPY, "再记一遍，下次一定答对喵！");
            }
            ref.setText("<html><body style='width:380px;line-height:1.7'>"
                    + "<b>参考答案：</b><br>" + q.ref + "</body></html>");
            ref.setForeground(Ui.TEXT);
            submit.setEnabled(false);
        });

        JPanel btnRow = Ui.clear();
        btnRow.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.add(submit);
        btnRow.add(next);
        btnRow.add(edit);
        btnRow.setBorder(new javax.swing.border.EmptyBorder(10, 0, 2, 0));

        // ============ 组装 ============
        JPanel main = Ui.clear();
        main.setLayout(new javax.swing.BoxLayout(main, javax.swing.BoxLayout.Y_AXIS));
        main.add(qCard);
        main.add(javax.swing.Box.createVerticalStrut(10));
        main.add(inputPad);
        main.add(tip);
        main.add(javax.swing.Box.createVerticalStrut(8));
        main.add(resBox);

        w.body.add(head, BorderLayout.NORTH);
        w.body.add(main, BorderLayout.CENTER);
        w.body.add(btnRow, BorderLayout.SOUTH);
        w.setVisible(true);
        answer.requestFocusInWindow();
    }
}
