package pet;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 持久层（阶段0）
 * 所有养成数据、签到、成就、待办、聊天记忆都存在 data/pet.db。
 * 统一用 synchronized 单连接，桌面宠物这点并发量完全够用。
 */
public class Db {

    public static Connection conn;

    /** 打开连接 + 建表。找不到驱动时给出人话提示。 */
    public static synchronized void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("找不到 SQLite 驱动，请确认 lib/sqlite-jdbc-*.jar 存在，并用 -cp \"out;lib/*\" 启动");
        }
        try {
            new File("data").mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:data/pet.db");
            Statement st = conn.createStatement();
            // 养成状态：KV 表，键值都存字符串，简单直接
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pet_state("
                    + "key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            // 签到记录：一天一行
            st.executeUpdate("CREATE TABLE IF NOT EXISTS sign_log("
                    + "date TEXT PRIMARY KEY)");
            // 成就：解锁时间戳
            st.executeUpdate("CREATE TABLE IF NOT EXISTS achievements("
                    + "id TEXT PRIMARY KEY, unlocked_at TEXT NOT NULL)");
            // 待办事项
            st.executeUpdate("CREATE TABLE IF NOT EXISTS todos("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "text TEXT NOT NULL, done INTEGER DEFAULT 0)");
            // 聊天记忆：role=cat/owner
            st.executeUpdate("CREATE TABLE IF NOT EXISTS chat_memory("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "role TEXT NOT NULL, content TEXT NOT NULL,"
                    + "ts INTEGER NOT NULL)");
            st.close();
        } catch (SQLException e) {
            throw new RuntimeException("初始化数据库失败: " + e.getMessage(), e);
        }
    }

    // ==================== KV 状态 ====================

    public static synchronized void set(String key, String value) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pet_state(key,value) VALUES(?,?) "
                  + "ON CONFLICT(key) DO UPDATE SET value=excluded.value");
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) { System.err.println("DB set 失败: " + e.getMessage()); }
    }

    public static synchronized String get(String key, String def) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT value FROM pet_state WHERE key=?");
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            String v = rs.next() ? rs.getString(1) : null;
            rs.close(); ps.close();
            return v == null ? def : v;
        } catch (SQLException e) { return def; }
    }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    public static void setInt(String key, int value) { set(key, String.valueOf(value)); }

    // ==================== 签到 ====================

    /** 今天是否已签到 */
    public static synchronized boolean signedToday(String date) {
        return queryHas("SELECT 1 FROM sign_log WHERE date=?", date);
    }

    public static synchronized void sign(String date) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO sign_log(date) VALUES(?)");
            ps.setString(1, date);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { System.err.println("签到写入失败: " + e.getMessage()); }
    }

    public static synchronized int signCount() {
        return count("SELECT COUNT(*) FROM sign_log");
    }

    // ==================== 成就 ====================

    public static synchronized boolean hasAchievement(String id) {
        return queryHas("SELECT 1 FROM achievements WHERE id=?", id);
    }

    public static synchronized void unlock(String id, String time) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO achievements(id,unlocked_at) VALUES(?,?)");
            ps.setString(1, id);
            ps.setString(2, time);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { System.err.println("成就写入失败: " + e.getMessage()); }
    }

    public static synchronized List<String[]> achievements() {
        List<String[]> list = new ArrayList<String[]>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, unlocked_at FROM achievements ORDER BY unlocked_at");
            while (rs.next()) list.add(new String[]{rs.getString(1), rs.getString(2)});
            rs.close(); st.close();
        } catch (SQLException ignored) {}
        return list;
    }

    // ==================== 待办 ====================

    public static synchronized List<String[]> todos() {
        List<String[]> list = new ArrayList<String[]>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, text, done FROM todos ORDER BY id DESC");
            while (rs.next()) list.add(new String[]{String.valueOf(rs.getInt(1)), rs.getString(2), String.valueOf(rs.getInt(3))});
            rs.close(); st.close();
        } catch (SQLException ignored) {}
        return list;
    }

    public static synchronized void addTodo(String text) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO todos(text) VALUES(?)");
            ps.setString(1, text);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { System.err.println("待办写入失败: " + e.getMessage()); }
    }

    /** 双击待办 = 切换完成状态 */
    public static synchronized void toggleTodo(int id) {
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE todos SET done = 1-done WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate(); ps.close();
        } catch (SQLException ignored) {}
    }

    public static synchronized void removeTodo(int id) {
        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM todos WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate(); ps.close();
        } catch (SQLException ignored) {}
    }

    // ==================== 聊天记忆 ====================

    /** 存一句话，role: cat=猫娘 owner=主人 */
    public static synchronized void remember(String role, String content) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO chat_memory(role,content,ts) VALUES(?,?,?)");
            ps.setString(1, role);
            ps.setString(2, content);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { System.err.println("聊天记忆写入失败: " + e.getMessage()); }
    }

    /** 最近 n 句对话，按时间正序返回 [role, content] */
    public static synchronized List<String[]> recentChat(int n) {
        List<String[]> list = new ArrayList<String[]>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT role, content FROM chat_memory ORDER BY id DESC LIMIT ?");
            ps.setInt(1, n);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString(1), rs.getString(2)});
            rs.close(); ps.close();
            java.util.Collections.reverse(list);   // 倒序取出后翻回正序
        } catch (SQLException ignored) {}
        return list;
    }

    public static synchronized int chatCount() { return count("SELECT COUNT(*) FROM chat_memory"); }

    /** 分页读历史（全量保存在库里）：offset=0 是最新一页，返回按时间正序 [role, content, ts] */
    public static synchronized List<String[]> chatPage(int offset, int limit) {
        List<String[]> list = new ArrayList<String[]>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT role, content, ts FROM chat_memory ORDER BY id DESC LIMIT ? OFFSET ?");
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            rs.close(); ps.close();
            java.util.Collections.reverse(list);
        } catch (SQLException ignored) { }
        return list;
    }

    /** 取 id > afterId 的最早 limit 条（喂给大模型做长期记忆压缩用） */
    public static synchronized List<String[]> chatAfter(long afterId, int limit) {
        List<String[]> list = new ArrayList<String[]>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, role, content FROM chat_memory WHERE id > ? ORDER BY id ASC LIMIT ?");
            ps.setLong(1, afterId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString(2), rs.getString(3), rs.getString(1)});
            rs.close(); ps.close();
        } catch (SQLException ignored) { }
        return list;
    }

    public static synchronized long maxChatId() {
        try {
            java.sql.Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT IFNULL(MAX(id),0) FROM chat_memory");
            long v = rs.next() ? rs.getLong(1) : 0;
            rs.close(); st.close();
            return v;
        } catch (SQLException e) { return 0; }
    }

    /** 关键词检索历史（跨全部记录，不限页），命中按 id 正序，最多 limit 条 */
    public static synchronized List<String[]> searchChat(List<String> keys, int limit) {
        List<String[]> list = new ArrayList<String[]>();
        if (keys == null || keys.isEmpty()) return list;
        StringBuilder sql = new StringBuilder("SELECT role, content, ts FROM chat_memory WHERE ");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("content LIKE ?");
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        try {
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < keys.size(); i++) ps.setString(i + 1, "%" + keys.get(i) + "%");
            ps.setInt(keys.size() + 1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            rs.close(); ps.close();
            java.util.Collections.reverse(list);
        } catch (SQLException ignored) { }
        return list;
    }

    // ==================== 内部工具 ====================

    private static boolean queryHas(String sql, String... params) {
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            ResultSet rs = ps.executeQuery();
            boolean has = rs.next();
            rs.close(); ps.close();
            return has;
        } catch (SQLException e) { return false; }
    }

    private static int count(String sql) {
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            int c = rs.next() ? rs.getInt(1) : 0;
            rs.close(); st.close();
            return c;
        } catch (SQLException e) { return 0; }
    }
}
