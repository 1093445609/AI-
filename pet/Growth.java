package pet;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 养成系统（阶段2）
 * 亲密度 → 等级（每 100 点升 1 级）；金币用于喂食和抽奖；
 * 每日签到攒连续天数；成就达成自动弹气泡 + 发奖金。
 * 所有数值落 SQLite（pet_state / sign_log / achievements）。
 */
public final class Growth {

    private Growth() {}

    // ==================== 基础读写 ====================

    public static int intimacy() { return Db.getInt("intimacy", 0); }
    public static int coins()    { return Db.getInt("coins", 0); }

    /** 等级 = 亲密度 / 100 + 1（亲密度只涨不清零） */
    public static int level() { return intimacy() / 100 + 1; }

    public static void addIntimacy(int n) {
        int old = intimacy();
        int now = Math.max(0, old + n);
        Db.setInt("intimacy", now);
        if (n > 0 && now / 100 > old / 100) {   // 跨过了 100 的整数倍 → 升级
            Emotion.trigger(Emotion.ACT_CUTE,
                    "哇！亲密度升级啦，现在是 " + level() + " 级喵！（转圈圈）");
        }
    }

    public static void addCoins(int n) { Db.setInt("coins", Math.max(0, coins() + n)); }

    /** 花钱，不够返回 false */
    public static boolean spend(int n) {
        if (coins() < n) return false;
        addCoins(-n);
        return true;
    }

    // ==================== 互动积分入口 ====================

    /** 各类行为加亲密度：head 摸头 / poke 戳脸 / drag 拖动 / chat 聊天 / game 赢游戏 / pomodoro 番茄钟 / quiz 答对 */
    public static void touch(String type) {
        switch (type) {
            case "head":     addIntimacy(2); bump("touch_total"); break;
            case "poke":     addIntimacy(1); break;
            case "drag":     addIntimacy(1); break;
            case "chat":     addIntimacy(2); bump("chat_total"); break;
            case "game":     addIntimacy(5); bump("games_won");  break;
            case "pomodoro": addIntimacy(15); bump("pomodoros"); break;
            case "quiz":     addIntimacy(8); bump("quiz_correct"); break;
        }
        checkAchievements();
    }

    static void bump(String key) { Db.setInt(key, Db.getInt(key, 0) + 1); }

    // ==================== 每日签到 ====================

    /** 签到，返回结果台词（已签过 / 签到成功 + 连续天数） */
    public static String sign() {
        LocalDate today = LocalDate.now();
        if (Db.signedToday(today.toString())) {
            Emotion.trigger(Emotion.HAPPY, "主人今天已经签过到啦喵，明天再来~");
            return null;
        }
        Db.sign(today.toString());
        addIntimacy(20);
        addCoins(10);
        Emotion.trigger(Emotion.ACT_CUTE, "签到成功喵！亲密度+20，金币+10，连续签到 " + streak() + " 天~");
        checkAchievements();
        return null;
    }

    /** 连续签到天数：从今天往前数连续有记录的天数（今天没签就从昨天数） */
    public static int streak() {
        LocalDate d = LocalDate.now();
        if (!Db.signedToday(d.toString())) d = d.minusDays(1);
        int s = 0;
        while (Db.signedToday(d.toString())) {
            s++;
            d = d.minusDays(1);
        }
        return s;
    }

    // ==================== 喂食 ====================

    /** 喂小鱼干：20 金币换 10 亲密度 */
    public static void feed() {
        if (!spend(20)) {
            Emotion.trigger(Emotion.SLEEPY, "金币不够喵……主人先去签到或者玩游戏赚金币吧（瘪嘴）");
            return;
        }
        addIntimacy(10);
        Emotion.trigger(Emotion.HAPPY, "哇是小鱼干喵！！咕噜咕噜~ 亲密度+10！");
        checkAchievements();
    }

    // ==================== 成就系统 ====================

    /** 成就定义表：id → [名称, 奖励金币] */
    static final Map<String, String[]> ACH = new LinkedHashMap<String, String[]>();
    static {
        ACH.put("first_sign",  new String[]{"初次签到", "10"});
        ACH.put("streak7",     new String[]{"连续签到 7 天", "50"});
        ACH.put("lv5",         new String[]{"亲密度 5 级", "30"});
        ACH.put("lv10",        new String[]{"亲密度 10 级", "80"});
        ACH.put("touch100",    new String[]{"累计摸头 100 次", "30"});
        ACH.put("chat50",      new String[]{"累计聊天 50 句", "30"});
        ACH.put("game_win1",   new String[]{"第一次游戏胜利", "10"});
        ACH.put("game_win10",  new String[]{"游戏胜利 10 次", "40"});
        ACH.put("pomodoro1",   new String[]{"完成第一个番茄钟", "20"});
        ACH.put("pomodoro10",  new String[]{"完成 10 个番茄钟", "60"});
        ACH.put("quiz10",      new String[]{"答对 10 道面试题", "40"});
        ACH.put("rich",        new String[]{"金币达到 500", "0"});
    }

    /** 检查全部成就，新解锁的弹气泡发奖金 */
    public static void checkAchievements() {
        for (Map.Entry<String, String[]> e : ACH.entrySet()) {
            String id = e.getKey();
            if (Db.hasAchievement(id)) continue;
            if (!earned(id)) continue;
            Db.unlock(id, java.time.LocalDateTime.now().toString().replace('T', ' '));
            int reward = Integer.parseInt(e.getValue()[1]);
            if (reward > 0) addCoins(reward);
            Emotion.trigger(Emotion.ACT_CUTE,
                    "成就解锁【" + e.getValue()[0] + "】喵！奖金 +" + reward + " 金币！");
        }
    }

    static boolean earned(String id) {
        switch (id) {
            case "first_sign":  return Db.signCount() >= 1;
            case "streak7":     return streak() >= 7;
            case "lv5":         return level() >= 5;
            case "lv10":        return level() >= 10;
            case "touch100":    return Db.getInt("touch_total", 0) >= 100;
            case "chat50":      return Db.chatCount() >= 50;
            case "game_win1":   return Db.getInt("games_won", 0) >= 1;
            case "game_win10":  return Db.getInt("games_won", 0) >= 10;
            case "pomodoro1":   return Db.getInt("pomodoros", 0) >= 1;
            case "pomodoro10":  return Db.getInt("pomodoros", 0) >= 10;
            case "quiz10":      return Db.getInt("quiz_correct", 0) >= 10;
            case "rich":        return coins() >= 500;
            default:            return false;
        }
    }

    /** 状态面板用的一行摘要 */
    public static String statusLine() {
        return "等级 " + level() + " · 亲密度 " + intimacy()
             + " · 金币 " + coins() + " · 连续签到 " + streak() + " 天";
    }
}
