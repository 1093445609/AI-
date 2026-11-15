package pet;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 人格系统（阶段5）
 * 双模式：
 *  1) 离线人格（默认）：关键词识别 + 长期记忆（SQLite）+ 用户偏好提取，不联网也能聊
 *  2) 真·大模型：data/api_config.txt 里配好 key 后自动切换，
 *     网络请求在后台线程跑，结果经 SwingUtilities 回 EDT，界面永不卡顿
 *
 * 记忆：每句话都进 chat_memory；主人名字、喜好等偏好存 pet_state。
 */
public final class Persona {

    static final Random RND = new Random();

    // LLM 配置（懒加载读 data/api_config.txt：第1行 key，第2行 base_url，第3行 model）
    static String apiKey, baseUrl, model;
    static boolean configLoaded = false;

    private Persona() {}

    /** 聊天统一入口：异步回调，回调一定在 EDT 上执行（ChatPanel 直接更新界面） */
    public static void chatAsync(String userText, java.util.function.Consumer<String> onReply) {
        Db.remember("owner", userText);
        extractPrefs(userText);
        loadConfig();
        if (apiKey == null || apiKey.isEmpty()) {
            onReply.accept(offlineReply(userText));          // 离线模式：同步算好（内部已落库）
        } else {
            Thread t = new Thread(() -> {
                String reply;
                try {
                    reply = llmReply(userText);
                    Db.remember("cat", reply);               // 关键：大模型的回复也要落库，否则重启就只剩半句对话
                    Growth.touch("chat");
                    digestAsync();                           // 老对话攒够一批就在后台压进长期摘要
                } catch (Exception e) {
                    reply = "（大模型没连上喵：" + e.getMessage() + "）\n" + offlineReply(userText);
                }
                final String r = reply;
                javax.swing.SwingUtilities.invokeLater(() -> onReply.accept(r));
            }, "persona-llm");
            t.setDaemon(true);
            t.start();
        }
    }

    // ==================== 用户偏好提取 ====================

    static final Pattern NAME_P = Pattern.compile("(?:叫我|我的名字是|我是)([\\u4e00-\\u9fa5A-Za-z0-9]{1,12})");
    static final Pattern LIKE_P = Pattern.compile("(?:我最喜欢|我喜欢|最爱)([\\u4e00-\\u9fa5A-Za-z0-9 ]{1,16})");

    static void extractPrefs(String text) {
        Matcher m = NAME_P.matcher(text);
        if (m.find()) {
            String old = Db.get("owner_name", "");
            String name = m.group(1);
            if (!name.equals(old)) {
                Db.set("owner_name", name);
                Pet.say("记住啦！以后就叫你 " + name + " 喵~");
            }
        }
        m = LIKE_P.matcher(text);
        if (m.find()) {
            String like = m.group(1).trim();
            if (!like.equals(Db.get("owner_likes", ""))) {
                Db.set("owner_likes", like);
            }
        }
    }

    // ==================== 离线人格 ====================

    static String offlineReply(String text) {
        String name = Db.get("owner_name", "主人");
        String t = text.toLowerCase();
        String r = ruleReply(name, t);
        Db.remember("cat", r);
        Growth.touch("chat");
        return r;
    }

    static String ruleReply(String name, String t) {
        // —— 记忆召回 ——
        if (t.contains("还记得") || t.contains("聊过") || t.contains("之前说")) {
            List<String[]> h = Db.recentChat(10);
            if (h.size() <= 2) return "我们还没聊过多少天呢喵，从今天开始记！";
            StringBuilder sb = new StringBuilder("都记着呢喵！最近聊过：\n");
            for (int i = Math.max(0, h.size() - 6); i < h.size(); i++)
                sb.append(h.get(i)[0].equals("owner") ? name + "：" : "我：").append(h.get(i)[1]).append("\n");
            return sb.toString().trim();
        }
        // —— 问候 / 情绪 ——
        if (t.contains("早上好") || t.contains("早安")) return name + "早上好喵！今天的面试题练了吗？";
        if (t.contains("晚安") || t.contains("睡了"))   return "晚安喵~ 今天辛苦了，梦里也要记得 finally 一定执行哦（开玩笑的）";
        if (t.contains("累") || t.contains("烦") || t.contains("难受"))
            return "抱抱" + name + "喵~ 累了就休息 5 分钟，猫娘陪你。学不进去的时候，宁可睡也别硬撑喵。";
        if (t.contains("开心") || t.contains("哈哈") || t.contains("乐"))
            return "嘿嘿，看到" + name + "开心我也开心喵！（摇尾巴）";
        if (t.contains("你好") || t.contains("在吗") || t.contains("hello") || t.contains("hi"))
            return "在的在的喵！" + name + "今天想聊点什么？学累了也可以找我玩抽奖喵~";
        // —— Java 学习话题（对应 Day1~Day5）——
        if (t.contains("hashmap")) return "HashMap 的考点我天天念叨喵：容量 2 的幂、(n-1)&hash、JDK8 扩容看 hash&oldCap、链表到 8 且数组到 64 才树化。想测一测就说「考我一道」！";
        if (t.contains("arraylist")) return "ArrayList 记三件事喵：默认容量 10、扩容 1.5 倍（old + old>>1）、modCount 撑起 fail-fast。";
        if (t.contains("异常") || t.contains("finally") || t.contains("try")) return "异常三坑喵：finally 一定执行、finally 里 return 会覆盖返回值、try-with-resources 逆序关闭。Day5 的笔记再看一眼~";
        if (t.contains("string")) return "String 的核心是不可变喵：常量池、编译期折叠、StringBuilder 才能高效拼接。这些都在 Day5 教材里！";
        if (t.contains("考我") || t.contains("面试题") || t.contains("出一题")) {
            QuizBank.show();
            return "好喵！面试题练习窗口已经打开，答对 +8 亲密度 +3 金币！";
        }
        // —— 学习监督 ——
        if (t.contains("不学") || t.contains("摆烂")) return "哼！20K 的目标还在前面挂着呢喵！摸鱼可以，摆烂不行。现在就去写一道 HashMap 的题！";
        if (t.contains("进度") || t.contains("学到哪")) return "现在学到 Day5 关账 92 分喵，下一站 Day6 学生成绩管理小程序。详情问导师或者看 PROGRESS.md~";
        // —— 闲聊兜底：轮换不同性格回复 ——
        String[] fallback = {
            "喵呜~ " + name + "继续说，我在听。（歪头）",
            "这个话题我喜欢喵！不过别忘了今天的 1.5 小时学习额度哦~",
            "唔……这个超出了猫娘的小脑瓜，但我会记下来的喵！（认真脸）",
            Db.getInt("pomodoros", 0) == 0
                ? "聊得开心，不过番茄钟还没开过喵，要不要专注 25 分钟？"
                : name + "今天已经完成 " + Db.getInt("pomodoros", 0) + " 个番茄钟了，继续保持喵！"
        };
        return fallback[RND.nextInt(fallback.length)];
    }

    // ==================== 真·大模型（OpenAI 兼容接口） ====================

    static synchronized void loadConfig() {
        if (configLoaded) return;
        configLoaded = true;
        File f = new File("data", "api_config.txt");
        if (!f.exists()) return;
        try {
            List<String> lines = new ArrayList<String>();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new java.io.FileInputStream(f), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) lines.add(line);
            }
            br.close();
            if (lines.size() >= 1) apiKey  = lines.get(0);
            if (lines.size() >= 2) baseUrl = lines.get(1); else baseUrl = "https://api.deepseek.com/v1";
            if (lines.size() >= 3) model   = lines.get(2); else model   = "deepseek-flash";
        } catch (IOException e) {
            System.err.println("读取 api_config.txt 失败: " + e.getMessage());
            apiKey = null;
        }
    }

    /** 后台线程调用：拼上下文（人设 + 长期摘要 + 相关旧对话 + 近 20 句原文）→ 请求模型 */
    static String llmReply(String userText) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":\"").append(model).append("\",\"messages\":[");
        body.append("{\"role\":\"system\",\"content\":").append(json(systemPrompt(userText))).append("},");
        List<String[]> hist = Db.recentChat(20);                      // 近记忆：原文
        // chatAsync 已经把这句话存进记忆了，这里去掉最后一条，避免同一句话喂两遍
        if (!hist.isEmpty()) {
            String[] last = hist.get(hist.size() - 1);
            if ("owner".equals(last[0]) && userText.equals(last[1])) hist.remove(hist.size() - 1);
        }
        for (String[] m : hist) {
            if (m[1].startsWith("让我想想喵")) continue;
            body.append("{\"role\":\"").append(m[0].equals("owner") ? "user" : "assistant")
                .append("\",\"content\":").append(json(m[1])).append("},");
        }
        body.append("{\"role\":\"user\",\"content\":").append(json(userText)).append("}],");
        body.append("\"temperature\":0.8,\"max_tokens\":300}");
        return postChat(body.toString());
    }

    /** 一次 POST + 从 JSON 里抠出 content（极简解析：只认第一个 content 字段） */
    static String postChat(String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(data); }

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder resp = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) resp.append(line);
        br.close();
        if (code != 200) throw new IOException("HTTP " + code);

        String s = resp.toString();
        int i = s.indexOf("\"content\":\"");
        if (i < 0) throw new IOException("响应里没有 content");
        i += "\"content\":\"".length();
        StringBuilder out = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {          // 反转义常见控制符
                char n = s.charAt(++i);
                if (n == 'n') out.append('\n');
                else if (n == 't') out.append('\t');
                else if (n == '"') out.append('"');
                else if (n == '\\') out.append('\\');
                else out.append(n);
            } else if (c == '"') break;
            else out.append(c);
            i++;
        }
        return out.toString();
    }

    /** 人设 + 偏好 + 长期记忆摘要 + 相关旧对话，喂给大模型 */
    static String systemPrompt(String userText) {
        String name = Db.get("owner_name", "主人");
        String likes = Db.get("owner_likes", "（还没记住）");
        StringBuilder sb = new StringBuilder();
        sb.append("你是一只 Java 猫娘桌宠，名字叫「喵酱」，性格：软萌粘人但学习监督时很严格。")
          .append("说话带「喵」，简短（不超过3句），偶尔用颜文字。")
          .append("你的主人叫").append(name).append("，正在 90 天 Java 涨薪计划（目标内地 20K+），当前进度 Day5，")
          .append("学过 HashMap、ArrayList、异常体系、String 常量池。他自述自制力差，需要你督促。")
          .append("主人的喜好：").append(likes).append("。")
          .append("请扮演好这个角色，主动关心学习和休息，不讨论违法违规内容。");

        String digest = Db.get("mem_digest", "");
        if (!digest.isEmpty()) sb.append("\n【长期记忆（老对话压缩而成）】").append(digest);

        List<String[]> rel = relevantHistory(userText, 4);
        if (!rel.isEmpty()) {
            sb.append("\n【可能相关的旧对话，用户问到时可以参考】");
            for (String[] r : rel) {
                sb.append("\n").append("owner".equals(r[0]) ? name : "喵酱").append("：").append(r[1]);
            }
        }
        return sb.toString();
    }

    // ==================== 长期记忆：全量存档 + 分层喂给模型 ====================
    // 设计：chat_memory 一条都不删（全量记忆，界面上可以一直往前翻）；
    // 喂给模型时分三层，兼顾“记得住”和“不吃爆 token”：
    //   ① 最近 20 句原文（细节、语气、上下文）
    //   ② 更早的老对话压成 mem_digest 摘要（每累积 30 句压一次，滚动合并）
    //   ③ 按当前这句话的关键词，从全量历史里召回相关旧片段

    static final int DIGEST_BATCH = 30;

    /** 后台把还没总结的老对话并进长期摘要（不阻塞界面；失败静默，下次再试） */
    public static void digestAsync() {
        loadConfig();
        if (apiKey == null || apiKey.isEmpty()) return;
        final long upto = parseLong(Db.get("mem_digest_upto", "0"));
        final List<String[]> rows = Db.chatAfter(upto, DIGEST_BATCH);
        if (rows.size() < DIGEST_BATCH) return;              // 不够一批先不压

        Thread t = new Thread(() -> {
            try {
                StringBuilder dlg = new StringBuilder();
                for (String[] r : rows) {
                    dlg.append("owner".equals(r[0]) ? "主人：" : "喵酱：").append(r[1]).append("\n");
                }
                String old = Db.get("mem_digest", "");
                String body = "{\"model\":\"" + model + "\",\"messages\":["
                        + "{\"role\":\"system\",\"content\":" + json(
                            "你负责维护一只猫娘桌宠的长期记忆。把「已有备忘」和「新对话」合并压缩成不超过 200 字的中文备忘录，"
                          + "必须保留：主人称呼、喜好、学习进度和卡点、约定过的事、情绪状态。只输出备忘录本身，不要解释。")
                        + "},{\"role\":\"user\",\"content\":" + json(
                            "【已有备忘】" + (old.isEmpty() ? "（空）" : old)
                          + "\n【新对话】\n" + dlg) + "}],"
                        + "\"temperature\":0.3,\"max_tokens\":400}";
                String summary = postChat(body).trim();
                if (!summary.isEmpty()) {
                    Db.set("mem_digest", summary);
                    Db.set("mem_digest_upto", rows.get(rows.size() - 1)[2]);
                    System.out.println("[记忆] 已压缩 " + rows.size() + " 句老对话 -> " + summary.length() + " 字");
                }
            } catch (Exception e) {
                System.err.println("[记忆] 压缩失败（下次再试）: " + e.getMessage());
            }
        }, "persona-digest");
        t.setDaemon(true);
        t.start();
    }

    static long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    /** 从当前这句话抽关键词，去全量历史里捞相关旧片段（跨分页召回） */
    static List<String[]> relevantHistory(String userText, int limit) {
        List<String> keys = new ArrayList<String>();
        Matcher en = Pattern.compile("[A-Za-z]{3,}").matcher(userText);
        while (en.find() && keys.size() < 3) keys.add(en.group());
        String cn = userText.replaceAll("[^\\u4e00-\\u9fa5]", "");
        for (int i = 0; i + 2 <= cn.length() && keys.size() < 5; i += 2) {
            String k = cn.substring(i, i + 2);
            if (!keys.contains(k)) keys.add(k);
        }
        java.util.Set<String> recent = new java.util.HashSet<String>();
        for (String[] r : Db.recentChat(20)) recent.add(r[1]);
        List<String[]> out = new ArrayList<String[]>();
        for (String[] r : Db.searchChat(keys, 20)) {
            if (recent.contains(r[1])) continue;
            out.add(r);
            if (out.size() >= limit) break;
        }
        return out;
    }

    /** 极简 JSON 字符串转义 */
    public static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
