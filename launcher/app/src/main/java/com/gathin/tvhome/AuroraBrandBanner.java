package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Aurora Purple: a small set of coherent, vector-drawn TV tiles.
 *
 * Draws in the dimensions of one TV banner, with no PNG loading, remote calls
 * or animations. Wordmarks are stylised typography; these are not the
 * platform companies' official logo artwork. Every other installed app keeps
 * its own icon provided by Android.
 */
final class AuroraBrandBanner extends Drawable {
    private static final int W = 400;
    private static final int H = 225;
    private static final int NETFLIX = 1;
    private static final int PRIME = 2;
    private static final int DISNEY = 3;
    private static final int MAX = 4;
    private static final int YOUTUBE = 5;
    private static final int GLOBOPLAY = 6;
    private static final int SPOTIFY = 7;
    private final int brand;
    private final int start;
    private final int finish;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private int alpha = 255;

    private AuroraBrandBanner(int brand, int start, int finish) {
        this.brand = brand;
        this.start = start;
        this.finish = finish;
    }

    static Drawable forPackage(String pkg) {
        if (pkg == null) return null;
        switch (pkg) {
            case "com.netflix.ninja":
                return new AuroraBrandBanner(NETFLIX, 0xFF2C0B1D, 0xFF110710);
            case "com.amazon.amazonvideo.livingroom":
                return new AuroraBrandBanner(PRIME, 0xFF092A52, 0xFF080E23);
            case "com.disney.disneyplus":
                return new AuroraBrandBanner(DISNEY, 0xFF172F65, 0xFF0B0E30);
            case "com.wbd.stream":
            case "com.hbo.hbonow":
            case "com.hbo.hbonow.tv":
                return new AuroraBrandBanner(MAX, 0xFF40216A, 0xFF1A0B34);
            case "com.google.android.youtube.tv":
                return new AuroraBrandBanner(YOUTUBE, 0xFF351326, 0xFF150D20);
            case "com.globo.globotv":
                return new AuroraBrandBanner(GLOBOPLAY, 0xFF4F2030, 0xFF1D0C24);
            case "com.spotify.tv.android":
                return new AuroraBrandBanner(SPOTIFY, 0xFF0C4136, 0xFF071E21);
            default:
                return null;
        }
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        int count = canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(b.width() / (float) W, b.height() / (float) H);
        p.setStyle(Paint.Style.FILL);
        p.setAlpha(alpha);
        p.setShader(new LinearGradient(0, 0, W, H, start, finish, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, p);
        p.setShader(null);

        // A single soft ambient light source, drawn once to the hardware cache.
        p.setShader(new RadialGradient(208, 102, 184, new int[]{
                0x2EB88AFF, 0x0E8F58D5, 0x008F58D5
        }, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(208, 102, 184, p);
        p.setShader(null);

        // Consistent tiny top-right signature and lower baseline for the set.
        p.setColor(0x88CBB1FF);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextSize(9);
        p.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("AURORA", 376, 24, p);
        p.setColor(0x356F52A8);
        p.setStrokeWidth(1f);
        canvas.drawLine(28, 199, 372, 199, p);

        p.setTextAlign(Paint.Align.CENTER);
        switch (brand) {
            case NETFLIX:
                word(canvas, "NETFLIX", 44f, 0xFFFF3350, true, 200f, 132f);
                break;
            case PRIME:
                word(canvas, "prime video", 43f, 0xFFF2F8FF, true, 200f, 131f);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(4.5f);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setColor(0xFF50C8FF);
                Path smile = new Path();
                smile.moveTo(123, 144);
                smile.quadTo(200, 169, 277, 144);
                canvas.drawPath(smile, p);
                p.setStyle(Paint.Style.FILL);
                break;
            case DISNEY:
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2.6f);
                p.setColor(0xFF9CB6FF);
                Path arc = new Path();
                arc.moveTo(100, 103);
                arc.cubicTo(160, 39, 248, 38, 311, 100);
                canvas.drawPath(arc, p);
                p.setStyle(Paint.Style.FILL);
                word(canvas, "Disney+", 58f, 0xFFF8F9FF, false, 200f, 153f);
                break;
            case MAX:
                word(canvas, "max", 87f, 0xFFF5E9FF, true, 200f, 146f);
                break;
            case YOUTUBE:
                p.setColor(0xFFFF344E);
                canvas.drawRoundRect(75, 91, 169, 150, 17, 17, p);
                Path play = new Path();
                play.moveTo(113, 105);
                play.lineTo(113, 136);
                play.lineTo(140, 120.5f);
                play.close();
                p.setColor(Color.WHITE);
                canvas.drawPath(play, p);
                p.setTextAlign(Paint.Align.LEFT);
                word(canvas, "YouTube", 43f, 0xFFF8F4FF, true, 180f, 136f);
                break;
            case GLOBOPLAY:
                word(canvas, "globoplay", 47f, 0xFFF8EFFF, true, 200f, 135f);
                break;
            case SPOTIFY:
                p.setColor(0xFF37E99C);
                canvas.drawCircle(92, 118, 33, p);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFF092B25);
                p.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < 3; i++) {
                    p.setStrokeWidth(4 - i * 0.4f);
                    Path wave = new Path();
                    float y = 109 + i * 10;
                    wave.moveTo(73, y);
                    wave.quadTo(91, y - 8, 111, y + 1);
                    canvas.drawPath(wave, p);
                }
                p.setStyle(Paint.Style.FILL);
                p.setTextAlign(Paint.Align.LEFT);
                word(canvas, "Spotify", 49f, 0xFFF1FFF7, true, 145f, 136f);
                break;
            default:
                break;
        }
        canvas.restoreToCount(count);
    }

    private void word(Canvas canvas, String name, float size, int color,
                      boolean bold, float x, float baseline) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setAlpha(alpha);
        p.setTextSize(size);
        p.setTypeface(Typeface.create(
                brand == DISNEY ? "cursive" : "sans-serif",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        // Preserve the desired left alignment on YouTube and Spotify.
        canvas.drawText(name, x, baseline, p);
    }

    @Override public void setAlpha(int a) {
        alpha = a;
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        p.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override public int getIntrinsicWidth() { return W; }
    @Override public int getIntrinsicHeight() { return H; }
}
