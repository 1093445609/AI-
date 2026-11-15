package pet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * 二次元猫娘桌宠（Java Swing 版）—— 入口类
 * 左键点头部=摸头 / 脸部=戳脸；按住拖动；右键菜单：养成/游戏/助手/聊天/退出
 *
 * 运行：java -cp "out;lib/*" pet.Pet   （JDK 8 及以上）
 */
public class Pet {

    static final Random RND = new Random();
    static JFrame frame;
    static JLabel spriteLabel;
    static BufferedImage idleImg, happyImg;
    static JWindow bubble;
    static Timer bubbleTimer, bobTimer;
    static javax.swing.Timer walkTimer;
    static boolean autoWalk = true;
    static boolean dragging = false;
    static Point pressPoint;
    static int baseY;          // 待机时上下漂浮的基准 y
    static int bobPhase = 0;   // 漂浮相位：0 上 1 下
    static boolean happyNow = false;
    static long lastInteraction = System.currentTimeMillis();   // 最近一次互动（困倦判定用）

    // 猫娘台词（Java 学习主题混搭，随机抽一句）
    static final String[] LINES = {
        "主人，今天的 HashMap 复习了吗喵？",
        "写完代码要自己跑一遍才算数喵！",
        "注意边界值 0 哦，主人已经栽过三次了喵！",
        "String 拼接要用 StringBuilder 喵！",
        "喵呜~ 该喝水啦，杯子里的水是不是凉了喵？",
        "久坐对腰不好，起来伸个懒腰喵~",
        "`(n-1) & hash` 前提是容量是 2 的幂喵！",
        "主人加油喵！20K 的路我们一起走~",
        "try 里 return 了，finally 还会跑喵！",
        "偶尔回头看看学过的东西，不然会忘喵~"
    };

    public static void main(String[] args) throws Exception {
        Ui.installGlobal();   // 二次元主题：菜单/提示框/滚动条 全局换肤
        Db.init();   // 阶段0：养成/记忆数据全部落 SQLite

        File dir = new File("assets");
        idleImg  = load(dir, "cat_idle.png");
        happyImg = load(dir, "cat_happy.png");

        frame = new JFrame("Java猫娘");
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setBackground(new Color(0, 0, 0, 0));   // 整窗透明，只留 PNG 本体
        frame.setType(Window.Type.UTILITY);
        frame.setLayout(null);

        spriteLabel = new JLabel(new ImageIcon(idleImg), SwingConstants.CENTER);
        spriteLabel.setBounds(0, 0, idleImg.getWidth(), idleImg.getHeight());
        frame.add(spriteLabel);
        frame.setSize(idleImg.getWidth(), idleImg.getHeight());

        // 初始位置：屏幕右下角
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screen.width - idleImg.getWidth() - 60,
                screen.height - idleImg.getHeight() - 80);
        baseY = frame.getY();

        bindMouse();
        buildMenu();
        startAnim();          // 阶段5：统一帧驱动（呼吸/步态/敲键盘抖动）
        startWalking();
        startIdleChatter();   // 阶段1：闲置时随机冒一句复习提醒
        startIdleMood();      // 阶段5：闲太久会自己摸鱼
        startSleepyWatch();   // 阶段1：6 分钟没互动自动犯困
        Assist.startRestReminder();   // 阶段4：久坐提醒（45 分钟一轮）

        frame.setVisible(true);
    }

    /** 记录最近互动时间；被摸一下就结束摸鱼/疲倦（番茄钟进行中除外） */
    static void touch() {
        lastInteraction = System.currentTimeMillis();
        boolean pomodoroRunning = Assist.pomodoroTimer != null && Assist.pomodoroTimer.isRunning();
        if (Act.is(Act.A.FISHING) || Act.is(Act.A.TIRED) || Act.is(Act.A.STRETCH)
                || (Act.is(Act.A.WORKING) && !pomodoroRunning)) {
            Act.reset();
        }
    }

    /** 读取素材图并缩放到桌宠尺寸（高约 240px，保持比例，双线性平滑） */
    static BufferedImage load(File dir, String name) throws Exception {
        File f = new File(dir, name);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(null, "找不到素材 " + name + "，请保证和 Pet.class 同目录有 assets/" + name);
            System.exit(1);
        }
        BufferedImage src = ImageIO.read(f);
        int h = 240, w = Math.round(src.getWidth() * (h / (float) src.getHeight()));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    // ==================== 交互 ====================

    static void bindMouse() {
        spriteLabel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                pressPoint = e.getLocationOnScreen();
                dragging = false;
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (dragging) { Interact.afterDrag(); return; }
                if (SwingUtilities.isLeftMouseButton(e)) Interact.onClick(e);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (Emotion.current() == Emotion.IDLE) Emotion.set(Emotion.HAPPY);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (Emotion.current() == Emotion.HAPPY) Emotion.set(Emotion.IDLE);
            }
        });
        spriteLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                Point cur = e.getLocationOnScreen();
                if (Math.abs(cur.x - pressPoint.x) + Math.abs(cur.y - pressPoint.y) > 4) dragging = true;
                if (dragging) {
                    baseY = cur.y - idleImg.getHeight() / 2;
                    frame.setLocation(cur.x - idleImg.getWidth() / 2, baseY);
                }
            }
        });
    }

    static void buildMenu() {
        JPopupMenu menu = Ui.menu();   // 统一粉色卡片样式

        // —— 养成（阶段2）——
        JMenuItem status = new JMenuItem("我的猫娘（状态/成就）");
        status.addActionListener(e -> StatusPanel.show());
        menu.add(status);
        JMenuItem sign = new JMenuItem("每日签到");
        sign.addActionListener(e -> Growth.sign());
        menu.add(sign);
        JMenuItem feed = new JMenuItem("喂小鱼干（20金币）");
        feed.addActionListener(e -> Growth.feed());
        menu.add(feed);

        // —— 动作（阶段5）——
        JMenu acts = new JMenu("动作");
        JMenuItem actFish = new JMenuItem("摸鱼（抱着大肥鱼）");
        actFish.addActionListener(e -> Act.play(Act.A.FISHING, 180000, true));
        acts.add(actFish);
        JMenuItem actWork = new JMenuItem("敲代码（25 分钟）");
        actWork.addActionListener(e -> Act.play(Act.A.WORKING, 25 * 60000, true));
        acts.add(actWork);
        JMenuItem actTired = new JMenuItem("疲倦（演示 8 秒）");
        actTired.addActionListener(e -> Act.play(Act.A.TIRED, 8000, true));
        acts.add(actTired);
        JMenuItem actStretch = new JMenuItem("伸懒腰");
        actStretch.addActionListener(e -> Act.play(Act.A.STRETCH, 3000, true));
        acts.add(actStretch);
        acts.addSeparator();
        JMenuItem actIdle = new JMenuItem("回到待机");
        actIdle.addActionListener(e -> Act.reset());
        acts.add(actIdle);
        menu.add(acts);
        JMenuItem games = new JMenuItem("小游戏");
        games.addActionListener(e -> pet.game.GameHub.show());
        menu.add(games);
        JMenuItem pomodoro = new JMenuItem("番茄钟（25分钟）");
        pomodoro.addActionListener(e -> Assist.showPomodoro());
        menu.add(pomodoro);
        JMenuItem todos = new JMenuItem("待办事项");
        todos.addActionListener(e -> Assist.showTodos());
        menu.add(todos);
        JMenuItem quiz = new JMenuItem("Java面试题练习");
        quiz.addActionListener(e -> QuizBank.show());
        menu.add(quiz);
        JMenuItem quizEdit = new JMenuItem("编辑题库（quiz.txt）");
        quizEdit.addActionListener(e -> QuizBank.openFile());
        menu.add(quizEdit);
        JMenuItem chat = new JMenuItem("和猫娘聊天");
        chat.addActionListener(e -> ChatPanel.show());
        menu.add(chat);
        menu.addSeparator();

        JCheckBoxMenuItem walkItem = new JCheckBoxMenuItem("自动游走", true);
        walkItem.addActionListener(e -> autoWalk = walkItem.isSelected());
        menu.add(walkItem);
        JMenuItem about = new JMenuItem("关于");
        about.addActionListener(e -> say("我是 Java 猫娘喵！Swing 写的，\n代码在 java_pet/pet/ 目录，主人看得懂的喵~"));
        menu.add(about);
        JMenuItem quit = new JMenuItem("退出");
        quit.addActionListener(e -> System.exit(0));
        menu.add(quit);
        spriteLabel.setComponentPopupMenu(menu);
    }

    // ==================== 闲置氛围（阶段1） ====================

    /** 每 90~180 秒没在互动时，随机冒一句学习提醒（原台词池 LINES） */
    static void startIdleChatter() {
        javax.swing.Timer next = new javax.swing.Timer(90000 + RND.nextInt(90000), ev -> {
            if (Emotion.current() == Emotion.IDLE && !dragging) {
                say(LINES[RND.nextInt(LINES.length)]);
            }
            startIdleChatter();   // 重新摇一次间隔
        });
        next.setRepeats(false);
        next.start();
    }

    /** 每 60 秒检查一次：6 分钟没互动就犯困 */
    static void startSleepyWatch() {
        javax.swing.Timer t = new javax.swing.Timer(60000, ev -> {
            if (System.currentTimeMillis() - lastInteraction > 6 * 60000L
                    && Emotion.current() == Emotion.IDLE && !dragging) {
                Emotion.trigger(Emotion.SLEEPY);
                touch();
            }
        });
        t.setRepeats(true);
        t.start();
    }

    // ==================== 台词气泡 ====================

    static void say(String text) {
        if (frame == null) return;   // UI 未就绪（如单测）直接跳过
        if (bubble == null) {
            bubble = new JWindow(frame);
            bubble.setBackground(new Color(0, 0, 0, 0));
        }
        JLabel label = new JLabel("<html><div style='text-align:center;padding:8px 12px;'>"
                + text.replace("\n", "<br>") + "</div></html>", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 235));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(new Color(255, 170, 190));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                super.paintComponent(g);
            }
        };
        label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        label.setForeground(new Color(90, 60, 70));
        bubble.setContentPane(label);
        bubble.pack();
        int bx = frame.getX() + frame.getWidth() / 2 - bubble.getWidth() / 2;
        int by = frame.getY() - bubble.getHeight() - 8;
        bubble.setLocation(Math.max(4, bx), Math.max(4, by));
        bubble.setAlwaysOnTop(true);
        bubble.setVisible(true);

        if (bubbleTimer != null) bubbleTimer.stop();
        bubbleTimer = new Timer(2600, ev -> bubble.setVisible(false));
        bubbleTimer.setRepeats(false);
        bubbleTimer.start();
    }

    // ==================== 动画（阶段5：统一 40ms 帧驱动） ====================

    static Timer animTimer;
    static BufferedImage lastIcon;
    static int lastDy = Integer.MIN_VALUE;
    static boolean faceLeft = false;      // 朝左走时镜像立绘

    /**
     * 所有小动作统一在这里跑（25fps）：
     * 呼吸、走路起伏、敲键盘抖动、疲倦点头、摸鱼晃动，全由 Act.dy() 给出纵向偏移，
     * 走路时再用压扁帧模拟落脚。情绪激活期间让位给 Emotion 的立绘。
     */
    static void startAnim() {
        animTimer = new Timer(40, ev -> {
            Act.tick();
            if (dragging) return;
            BufferedImage img = Act.icon(faceLeft);
            if (img != null && img != lastIcon) {
                spriteLabel.setIcon(new ImageIcon(img));
                lastIcon = img;
            }
            int dy = Act.dy();
            if (dy != lastDy) {
                frame.setLocation(frame.getX(), baseY + dy);
                lastDy = dy;
            }
        });
        animTimer.start();
    }

    /** 自动游走：每 8~15 秒慢悠悠横着挪一段，挪的时候有步态 */
    static void startWalking() {
        scheduleNextWalk();
    }

    static void scheduleNextWalk() {
        if (walkTimer != null) walkTimer.stop();
        javax.swing.Timer next = new javax.swing.Timer(8000 + RND.nextInt(7000), ev -> doWalk());
        next.setRepeats(false);
        next.start();
        walkTimer = next;
    }

    static void doWalk() {
        if (!autoWalk) { scheduleNextWalk(); return; }
        // 正在敲代码 / 疲倦 / 情绪中就不乱跑，免得破坏氛围
        if (Emotion.current() != Emotion.IDLE
                || Act.is(Act.A.WORKING) || Act.is(Act.A.TIRED) || Act.is(Act.A.STRETCH)
                || Act.is(Act.A.FISHING)) {
            scheduleNextWalk();
            return;
        }

        final int direction = RND.nextBoolean() ? 1 : -1;
        faceLeft = direction < 0;                 // 朝左走就镜像
        final int distance = 70 + RND.nextInt(120);

        Act.play(Act.A.WALK, 0);                  // 进入走路动作（步态由 Act 提供）
        final int[] moved = {0};
        final javax.swing.Timer mover = new javax.swing.Timer(40, null);
        mover.addActionListener(ev -> {
            if (dragging || !autoWalk || Emotion.current() != Emotion.IDLE) {
                mover.stop();
                Act.play(Act.A.IDLE, 0);
                scheduleNextWalk();
                return;
            }
            Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
            int x = frame.getX() + direction * 2;
            if (x < 10 || x > s.width - idleImg.getWidth() - 10) {
                mover.stop();
                Act.play(Act.A.IDLE, 0);
                scheduleNextWalk();
                return;
            }
            frame.setLocation(x, frame.getY());
            moved[0] += 2;
            if (moved[0] >= distance) {
                mover.stop();
                Act.play(Act.A.IDLE, 0);
                scheduleNextWalk();
            }
        });
        mover.start();
    }

    /** 长时间没互动就自己摸鱼（不是犯困，是悠哉地抱着鱼） */
    static void startIdleMood() {
        javax.swing.Timer t = new javax.swing.Timer(60000, ev -> {
            if (dragging || Emotion.current() != Emotion.IDLE) return;
            long idle = System.currentTimeMillis() - lastInteraction;
            if (idle > 3 * 60000L && Act.is(Act.A.IDLE) && RND.nextInt(100) < 45) {
                Act.play(Act.A.FISHING, 90000, false);
            }
        });
        t.setRepeats(true);
        t.start();
    }
}
