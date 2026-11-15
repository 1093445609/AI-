package pet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 表情特效叠加（阶段1）
 * 没有专属立绘的情绪，在待机图上用 Graphics2D 画出来：
 * 害羞=腮红+小爱心 / 生气=怒气符号 / 困倦=Zzz / 撒娇=爱心+星光
 * 坐标按立绘实际构图标定：脸蛋约 (0.33w, 0.49h) 和 (0.63w, 0.49h)，头顶右上一块留白区。
 */
public final class Effects {

    static final Random RND = new Random();

    private Effects() {}

    public static BufferedImage apply(BufferedImage base, Emotion e) {
        int w = base.getWidth(), h = base.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(base, 0, 0, null);
        switch (e) {
            case SHY:    blush(g, w, h); heart(g, w * 0.20f, h * 0.14f, w * 0.045f); break;
            case ANGRY:  angerMark(g, w, h); break;
            case SLEEPY: zzz(g, w, h); break;
            case ACT_CUTE: heart(g, w * 0.20f, h * 0.14f, w * 0.05f);
                           heart(g, w * 0.80f, h * 0.22f, w * 0.04f); break;
            default: break;
        }
        g.dispose();
        return dst;
    }

    /** 两团腮红，半透明粉椭圆压在脸蛋上 */
    static void blush(Graphics2D g, int w, int h) {
        g.setColor(new Color(255, 110, 140, 85));
        int rx = Math.round(w * 0.055f), ry = Math.round(w * 0.028f);
        int cy = Math.round(h * 0.49f);
        g.fillOval(Math.round(w * 0.33f) - rx, cy - ry, rx * 2, ry * 2);
        g.fillOval(Math.round(w * 0.63f) - rx, cy - ry, rx * 2, ry * 2);
    }

    /** 怒气符号：头顶右上的四道红色爆筋 */
    static void angerMark(Graphics2D g, int w, int h) {
        float cx = w * 0.74f, cy = h * 0.10f;
        float r1 = w * 0.035f, r2 = w * 0.075f;
        g.setColor(new Color(215, 55, 55, 220));
        g.setStroke(new BasicStroke(Math.max(2f, w * 0.014f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;   // 45°/135°/225°/315°
            g.drawLine((int) Math.round(cx + Math.cos(a) * r1), (int) Math.round(cy + Math.sin(a) * r1),
                       (int) Math.round(cx + Math.cos(a) * r2), (int) Math.round(cy + Math.sin(a) * r2));
        }
    }

    /** Z z z：右上斜排三颗，越远越小 */
    static void zzz(Graphics2D g, int w, int h) {
        g.setColor(new Color(120, 130, 170, 230));
        String[] zs = {"Z", "z", "z"};
        int[] sizes = {Math.round(w * 0.085f), Math.round(w * 0.065f), Math.round(w * 0.048f)};
        int[][] pos = {
            {Math.round(w * 0.66f), Math.round(h * 0.16f)},
            {Math.round(w * 0.74f), Math.round(h * 0.10f)},
            {Math.round(w * 0.81f), Math.round(h * 0.055f)}
        };
        for (int i = 0; i < 3; i++) {
            g.setFont(new Font("微软雅黑", Font.BOLD, sizes[i]));
            g.drawString(zs[i], pos[i][0], pos[i][1]);
        }
    }

    /** 小爱心：两个圆 + 一个三角 */
    static void heart(Graphics2D g, float cx, float cy, float s) {
        g.setColor(new Color(255, 90, 130, 200));
        float r = s * 0.30f;
        g.fillOval(Math.round(cx - s * 0.30f - r), Math.round(cy - s * 0.15f - r), Math.round(r * 2), Math.round(r * 2));
        g.fillOval(Math.round(cx + s * 0.02f), Math.round(cy - s * 0.15f - r), Math.round(r * 2), Math.round(r * 2));
        int[] xs = {
            Math.round(cx - s * 0.30f), Math.round(cx + s * 0.32f), Math.round(cx + s * 0.01f)
        };
        int[] ys = {
            Math.round(cy - s * 0.02f), Math.round(cy - s * 0.02f), Math.round(cy + s * 0.55f)
        };
        g.fillPolygon(xs, ys, 3);
    }
}
