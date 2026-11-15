package pet;

import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * 互动系统（阶段1）
 * 左键单击按位置分流：
 *   头部（画面上 45%）= 摸头 → 撒娇 / 害羞
 *   脸部（下 55%）    = 戳脸 → 生气为主，连续快速戳真的会炸毛
 * 拖动由 Pet 原有逻辑处理，松手时随机抱怨一句。
 */
public final class Interact {

    static final Random RND = new Random();
    static long lastPoke = 0;          // 上次戳脸时间戳
    static int pokeStreak = 0;         // 10 秒内连续戳脸次数

    private Interact() {}

    /** 处理一次左键单击（Pet 的 mouseReleased 调用，拖动时不会进来） */
    public static void onClick(MouseEvent e) {
        int h = Pet.idleImg.getHeight();
        if (e.getY() < h * 0.45f) pokeHead();
        else                      pokeFace();
        Pet.touch();   // 记录最近互动时间（困倦判定用）
    }

    /** 摸头：大多是撒娇回应 */
    public static void pokeHead() {
        Growth.touch("head");
        if (RND.nextInt(100) < 70) Emotion.trigger(Emotion.ACT_CUTE);
        else                       Emotion.trigger(Emotion.SHY);
    }

    /** 戳脸：偶尔害羞，连戳三次一定炸毛 */
    public static void pokeFace() {
        Growth.touch("poke");
        long now = System.currentTimeMillis();
        pokeStreak = (now - lastPoke <= 10000) ? pokeStreak + 1 : 1;
        lastPoke = now;
        if (pokeStreak >= 3) {
            Emotion.trigger(Emotion.ANGRY, "都说了别戳啦喵！！（真生气了）");
            pokeStreak = 0;
        } else if (RND.nextInt(100) < 60) {
            Emotion.trigger(Emotion.ANGRY);
        } else {
            Emotion.trigger(Emotion.SHY);
        }
    }

    /** 拖动松手后的随机台词 */
    public static void afterDrag() {
        Growth.touch("drag");
        if (RND.nextBoolean()) {
            Emotion.trigger(Emotion.HAPPY, "喵呜~ 飞起来了喵！（抓紧主人）");
        }
        Pet.touch();
    }
}
