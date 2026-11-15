package pet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.Timer;

/**
 * 情绪系统（阶段1）
 * 六态：待机 / 开心 / 害羞 / 生气 / 困倦 / 撒娇
 * 优先用 assets 里的对应立绘，没有就由 Effects 在待机图上叠加特效。
 * set() 会同步 Pet.happyNow，让原来的呼吸/游走暂停逻辑无缝生效。
 */
public enum Emotion {
    IDLE, HAPPY, SHY, ANGRY, SLEEPY, ACT_CUTE;

    static final Random RND = new Random();
    static Emotion current = IDLE;
    static Timer expiry;                                    // 情绪到点回落待机
    static final Map<Emotion, BufferedImage> FACE_CACHE = new HashMap<Emotion, BufferedImage>();

    // ==================== 情绪台词池 ====================

    public String[] lines() {
        switch (this) {
            case HAPPY: return new String[] {
                "嘿嘿，主人摸得真舒服喵~",
                "开心到尾巴都竖起来了喵！",
                "今天的代码也是元气满满喵！"
            };
            case SHY: return new String[] {
                "唔……不要一直盯着人家看喵……",
                "脸、脸红什么的才没有喵！",
                "主人突然这样人家会害羞的喵……"
            };
            case ANGRY: return new String[] {
                "哼！生气了，不理主人三秒钟喵！",
                "再戳脸就咬你哦喵！（炸毛）",
                "有戳脸的功夫快去写代码喵！"
            };
            case SLEEPY: return new String[] {
                "Zzz……主人也休息一下眼睛喵……",
                "猫娘困了……五分钟，就五分钟喵……",
                "代码不会跑掉的，睡一觉再说喵……"
            };
            case ACT_CUTE: return new String[] {
                "主人~ 再摸摸头嘛喵（蹭蹭）",
                "刚才那个摸头，人家还想再来一次喵~",
                "要听主人说一句「写得真好」喵~"
            };
            default: return new String[] { "" };
        }
    }

    /** 该情绪持续多久后自动回到待机（毫秒） */
    public int duration() {
        switch (this) {
            case HAPPY:    return 1600;
            case SHY:      return 2600;
            case ANGRY:    return 3000;
            case SLEEPY:   return 4000;
            case ACT_CUTE: return 2800;
            default:       return 0;
        }
    }

    /** 对应立绘文件名（不存在时用叠加特效） */
    public String assetName() {
        switch (this) {
            case HAPPY:    return "cat_happy.png";
            case SHY:      return "cat_shy.png";
            case ANGRY:    return "cat_angry.png";
            case SLEEPY:   return "cat_sleepy.png";
            case ACT_CUTE: return "cat_actcute.png";
            default:       return "cat_idle.png";
        }
    }

    // ==================== 状态机 ====================

    /** 触发情绪：切表情 + 冒一句该情绪的台词，到点回待机 */
    public static void trigger(Emotion e) { trigger(e, null); }

    public static void trigger(Emotion e, String customLine) {
        set(e);
        if (e != IDLE && Pet.spriteLabel != null)   // UI 未就绪（如单测）就不冒气泡
            Pet.say(customLine != null ? customLine : pick(e));
        if (expiry != null) expiry.stop();
        if (e == IDLE) return;
        expiry = new Timer(e.duration(), ev -> set(IDLE));
        expiry.setRepeats(false);
        expiry.start();
    }

    /** 只切表情不说话（悬停等场景） */
    public static void set(Emotion e) {
        current = e;
        if (Pet.spriteLabel != null) {
            Pet.spriteLabel.setIcon(new ImageIcon(face(e)));
            Pet.lastIcon = null;      // 让帧驱动下一帧重新接管（否则会卡在情绪图上）
        }
        Pet.happyNow = (e != IDLE);          // 复用原有动画暂停逻辑
        if (e == IDLE && Pet.bubble != null && Pet.bubble.isVisible()) Pet.bubble.setVisible(false);
    }

    public static Emotion current() { return current; }

    /** 取该情绪的脸：优先 assets 立绘，否则待机图叠加特效；结果缓存 */
    static BufferedImage face(Emotion e) {
        BufferedImage cached = FACE_CACHE.get(e);
        if (cached != null) return cached;
        BufferedImage img;
        File f = new File("assets", e.assetName());
        if (f.exists()) {
            try { img = Pet.load(f.getParentFile(), f.getName()); }
            catch (Exception ex) { img = Effects.apply(idleBase(), e); }
        } else {
            img = Effects.apply(idleBase(), e);   // 没立绘 → 代码叠加特效
        }
        FACE_CACHE.put(e, img);
        return img;
    }

    /** 待机底图：Pet.main 已加载就用它，否则兜底从 assets 现读 */
    private static BufferedImage idleBase() {
        if (Pet.idleImg != null) return Pet.idleImg;
        try { return Pet.load(new File("assets"), "cat_idle.png"); }
        catch (Exception ex) { throw new RuntimeException("找不到 assets/cat_idle.png", ex); }
    }

    static String pick(Emotion e) {
        String[] ls = e.lines();
        return ls[RND.nextInt(ls.length)];
    }
}
