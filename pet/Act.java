package pet;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 动作系统（阶段5）：让猫娘不只是"换表情"，而是有姿态和肢体动作。
 *
 * 六个动作：待机 / 散步 / 疲倦 / 摸鱼 / 敲代码 / 伸懒腰
 * - 立绘优先读 assets/cat_tired.png、cat_fish.png、cat_work.png，缺图自动回退待机图（不会崩）
 * - 动作是"长时间姿态"，情绪（Emotion）是"短时表情"：情绪触发时临时盖住动作，回落后再继续
 * - 所有位移/抖动都由 dy() 给，由 Pet 的 40ms 帧定时器统一驱动，不会再出现"整张图平移"
 */
public final class Act {

    public enum A {
        IDLE, WALK, TIRED, FISHING, WORKING, STRETCH
    }

    static A cur = A.IDLE;
    static long startAt = System.currentTimeMillis();
    static long until = 0;                 // 0 = 一直持续，直到被叫停
    static int frame = 0;

    /** 帧缓存：立绘 + 压扁帧 + 镜像帧 */
    private static final Map<String, BufferedImage> CACHE = new HashMap<String, BufferedImage>();

    private Act() {}

    /* ==================== 立绘 ==================== */

    static String art(A a) {
        switch (a) {
            case TIRED:   return "cat_tired.png";
            case STRETCH: return "cat_stretch.png";
            case FISHING: return "cat_fish.png";
            case WORKING: return "cat_work.png";
            default:      return "cat_idle.png";
        }
    }

    static String label(A a) {
        switch (a) {
            case WALK:    return "散步";
            case TIRED:   return "疲倦";
            case STRETCH: return "伸懒腰";
            case FISHING: return "摸鱼";
            case WORKING: return "敲代码";
            default:      return "待机";
        }
    }

    /** 动作自带的台词（播放时可选地说一句） */
    static String[] lines(A a) {
        switch (a) {
            case TIRED:
                return new String[] {
                    "唔……眼睛好涩，打个哈欠喵（揉眼）",
                    "盯屏幕太久了，脑袋要当机了喵……",
                    "让我趴一会儿，就一小会儿喵……"
                };
            case FISHING:
                return new String[] {
                    "嘿嘿，抱着大肥鱼摸一会儿鱼喵~",
                    "写代码的事……等这条鱼吃完再说喵",
                    "今天也是摸鱼的一天喵（满足）"
                };
            case WORKING:
                return new String[] {
                    "嗒嗒嗒……主人在写代码，我也来敲两下喵！",
                    "认真敲键盘中的猫娘，请勿打扰喵~",
                    "一起写代码吧！bug 退散喵！"
                };
            case STRETCH:
                return new String[] {
                    "伸——个——懒——腰——喵！",
                    "久坐伤腰喵，起来动一动！"
                };
            default:
                return new String[] { "" };
        }
    }

    /* ==================== 状态机 ==================== */

    /** 播放动作，ms<=0 表示一直持续到被替换 */
    public static void play(A a, int ms) {
        play(a, ms, false);
    }

    /** 播放动作；sayLine=true 时顺便冒一句该动作的台词 */
    public static void play(A a, int ms, boolean sayLine) {
        if (cur == a && (until == 0 || System.currentTimeMillis() < until)) return;   // 已经在演，不打断
        cur = a;
        startAt = System.currentTimeMillis();
        until = ms > 0 ? startAt + ms : 0;
        frame = 0;
        if (sayLine && a != A.IDLE && Pet.spriteLabel != null) {
            String[] ls = lines(a);
            Pet.say(ls[(int) (Math.random() * ls.length)]);
        }
        if (a == A.IDLE) Pet.happyNow = Emotion.current() != Emotion.IDLE;
    }

    public static void play(A a) {
        play(a, defaultMs(a), false);
    }

    static int defaultMs(A a) {
        switch (a) {
            case TIRED:   return 8000;
            case FISHING: return 120000;
            case WORKING: return 1500000;   // 25 分钟，配合番茄钟
            case STRETCH: return 3000;
            case WALK:    return 6000;
            default:      return 0;
        }
    }

    public static A current() {
        return cur;
    }

    public static boolean is(A a) {
        return cur == a;
    }

    /** 立刻回待机 */
    public static void reset() {
        cur = A.IDLE;
        startAt = System.currentTimeMillis();
        until = 0;
        frame = 0;
    }

    /** 每帧推进（Pet 的 40ms 定时器调用）：到点自动回待机 */
    public static void tick() {
        frame++;
        if (until > 0 && System.currentTimeMillis() > until) reset();
    }

    public static int frames() {
        return frame;
    }

    /* ==================== 帧内容 ==================== */

    /**
     * 当前帧该显示哪张图。
     * 情绪激活时返回 null，让 Emotion 的立绘继续显示（动作让位于情绪）。
     */
    public static BufferedImage icon(boolean faceLeft) {
        if (Emotion.current() != Emotion.IDLE) return null;

        BufferedImage base = base();
        if (base == null) return null;

        if (cur == A.WORKING) {
            // 敲代码：本体不动，只有双手+键盘区域左右交替下压 1px（见 typing()）
            return typing(base, faceLeft);
        }
        if (cur == A.WALK) {
            // 走路的四拍：站 → 压 → 站 → 压，配合镜像表示朝向
            double sy = (frame % 6 < 3) ? 1.0 : 0.94;
            return squash(base, sy, faceLeft);
        }
        return base;
    }

    /**
     * 敲键盘帧：只动键盘那一小块，身体和头纹丝不动。
     * 立绘里键盘区域大约在 x 26%~60%、y 61%~79%（cat_work.png，1024×1024 实测）。
     * 以键盘中线为界分成左右两半，各代表一只手，120ms 交替下压 1px，
     * 像两只手指在轮流按键。先整张画 base 再叠偏移块，1px 空隙露出原图，视觉无缝。
     */
    static BufferedImage typing(BufferedImage base, boolean mirror) {
        boolean leftDown = (frame / 3) % 2 == 0;          // 120ms 换一次手
        String key = System.identityHashCode(base) + "_typing_" + leftDown + "_" + mirror;
        BufferedImage out = CACHE.get(key);
        if (out != null) return out;

        int w = base.getWidth(), h = base.getHeight();
        int x1 = (int) (w * 0.26), x2 = (int) (w * 0.60);
        int y1 = (int) (h * 0.61), y2 = (int) (h * 0.79);
        int mid = (x1 + x2) / 2;

        out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(base, 0, 0, null);
        // 镜像时区域坐标也跟着翻
        int lo = Math.min(mirror ? w - x2 : x1, mirror ? w - x1 : x2);
        int hi = Math.max(mirror ? w - x2 : x1, mirror ? w - x1 : x2);
        int mm = mirror ? w - mid : mid;
        if (leftDown) {
            g.drawImage(base, lo, y1 + 2, mm, y2 + 2, lo, y1, mm, y2, null);   // 左手下压
        } else {
            g.drawImage(base, mm, y1 + 2, hi, y2 + 2, mm, y1, hi, y2, null);   // 右手下压
        }
        g.dispose();
        CACHE.put(key, out);
        return out;
    }

    /** 立绘（带缓存，缺图回退待机图） */
    static BufferedImage base() {
        String n = art(cur);
        BufferedImage b = CACHE.get(n);
        if (b != null) return b;
        File f = new File("assets", n);
        try {
            if (f.isFile()) b = Pet.load(new File("assets"), n);
        } catch (Exception ignored) {
            b = null;
        }
        if (b == null) b = Pet.idleImg;      // 没这张立绘就用待机图
        if (b != null) CACHE.put(n, b);
        return b;
    }

    /** 竖向压扁（走路的落脚感），可选水平镜像 */
    static BufferedImage squash(BufferedImage src, double sy, boolean mirror) {
        String key = System.identityHashCode(src) + "_" + Math.round(sy * 1000) + "_" + mirror;
        BufferedImage out = CACHE.get(key);
        if (out != null) return out;

        int w = src.getWidth(), h = src.getHeight();
        out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int nh = (int) Math.round(h * sy);
        int nw = (int) Math.min(w, Math.round(w / Math.sqrt(sy)));   // 体积守恒：压扁就会变宽一点点
        int dx = (w - nw) / 2;
        int dy = h - nh;                                             // 底部对齐，脚不会浮起来
        if (mirror) {
            g.drawImage(src, dx + nw, dy, -nw, nh, null);            // 负宽度 = 水平翻转
        } else {
            g.drawImage(src, dx, dy, nw, nh, null);
        }
        g.dispose();
        CACHE.put(key, out);
        return out;
    }

    /* ==================== 每帧的纵向位移 ==================== */

    /** 当前帧相对基准 y 的偏移（负数向上）。情绪激活时不参与，保持稳定 */
    public static int dy() {
        if (Emotion.current() != Emotion.IDLE) return 0;
        long t = System.currentTimeMillis() - startAt;
        switch (cur) {
            case WALK:
                // 每一步的上下起伏：周期 240ms，幅度 4px
                return (int) Math.round(-4 * (0.5 - 0.5 * Math.cos(t / 240.0 * 2 * Math.PI)));
            case TIRED:
                // 呼吸变慢变浅 + 每 4 秒点一次头（往下沉）
                return breath(t, 2600, 1.2) + nod(t, 4000, 2);
            case FISHING:
                // 悠哉的慢呼吸，幅度大一点，像在晃脚
                return breath(t, 2000, 3);
            case WORKING:
                // 敲代码：窗口纹丝不动，敲键动画在 typing() 里做局部帧
                return 0;
            case STRETCH:
                // 伸懒腰：大幅上下两次
                return (int) Math.round(-7 * Math.abs(Math.sin(t / 700.0 * 2 * Math.PI)));
            default:
                // 待机呼吸：1.2 秒一次，2px
                return breath(t, 1200, 2);
        }
    }

    /** 呼吸波：返回 0 ~ -amp 之间平滑值（负=向上） */
    static int breath(long t, double periodMs, double amp) {
        return (int) Math.round(-amp * (0.5 - 0.5 * Math.cos(t / periodMs * 2 * Math.PI)));
    }

    /** 周期性点头：每个周期最后 300ms 往下压 depth 像素 */
    static int nod(long t, double periodMs, double depth) {
        double phase = (t % (long) periodMs) / periodMs;
        if (phase < 0.92) return 0;
        double k = (phase - 0.92) / 0.08;       // 0→1
        return (int) Math.round(depth * Math.sin(k * Math.PI));
    }
}
