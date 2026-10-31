package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;

/**
 * Aurora 0.6: monochrome, transparent wordmarks above a photographic wallpaper.
 * These are minimalist text interpretations (not copies of official logo artwork).
 * No network calls, bitmap allocations, animation loops or dynamic resources.
 */
final class AuroraFloatingLogo extends Drawable {
    private static final int W = 400;
    private static final int H = 225;
    private final String packageName;
    private final String label;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private int alpha = 255;
    private boolean focused;

    /** White at rest; brand colour only for the tile holding remote focus. */
    void setFocused(boolean value) {
        if (focused == value) return;
        focused = value;
        invalidateSelf();
    }

    static int accentForPackage(String pkg) {
        if (pkg == null) return 0xFFA66BFF;
        switch (pkg) {
            case "com.netflix.ninja": return 0xFFE50914;
            case "com.amazon.amazonvideo.livingroom": return 0xFF00A8E1;
            case "com.disney.disneyplus": return 0xFF6FA9FF;
            case "com.wbd.stream":
            case "com.hbo.hbonow":
            case "com.hbo.hbonow.tv": return 0xFFB277FF;
            case "com.google.android.youtube.tv": return 0xFFFF1736;
            case "com.google.android.youtube.tvmusic":
            case "com.google.android.apps.youtube.music": return 0xFFFF3C60;
            case "com.globo.globotv": return 0xFFFF7348;
            case "com.spotify.tv.android": return 0xFF1ED760;
            case "com.apple.atve.androidtv.appletv": return 0xFFC6DAF2;
            default: return 0xFFA66BFF;
        }
    }

    private AuroraFloatingLogo(String packageName, String label) {
        this.packageName = packageName;
        this.label = label == null ? "" : label;
    }

    static Drawable forApp(String packageName, String label) {
        return new AuroraFloatingLogo(packageName, label);
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        int checkpoint = canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(b.width() / (float) W, b.height() / (float) H);
        p.reset();
        p.setAntiAlias(true);
        p.setSubpixelText(true);
        p.setColor(focused ? accentForPackage(packageName) : 0xFFF9F7FF);
        p.setAlpha(alpha);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        String name = label;
        float sz = 59f;
        if ("com.netflix.ninja".equals(packageName)) {
            name = "NETFLIX";
            sz = 63f;
        } else if ("com.amazon.amazonvideo.livingroom".equals(packageName)) {
            name = "prime video";
            sz = 56f;
        } else if ("com.disney.disneyplus".equals(packageName)) {
            name = "Disney+";
            sz = 69f;
            p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3.2f);
            Path arc = new Path();
            arc.moveTo(92, 98);
            arc.cubicTo(145, 39, 247, 40, 308, 93);
            canvas.drawPath(arc, p);
            p.setStyle(Paint.Style.FILL);
        } else if ("com.wbd.stream".equals(packageName)
                || "com.hbo.hbonow".equals(packageName)
                || "com.hbo.hbonow.tv".equals(packageName)) {
            name = "max";
            sz = 86f;
        } else if ("com.google.android.youtube.tv".equals(packageName)) {
            name = "YouTube";
            sz = 61f;
        } else if ("com.google.android.youtube.tvmusic".equals(packageName)
                || "com.google.android.apps.youtube.music".equals(packageName)) {
            name = "YouTube Music";
            sz = 43f;
        } else if ("com.globo.globotv".equals(packageName)) {
            name = "globoplay";
            sz = 54f;
        } else if ("com.spotify.tv.android".equals(packageName)) {
            name = "Spotify";
            sz = 64f;
        } else if ("com.apple.atve.androidtv.appletv".equals(packageName)) {
            name = "Apple TV";
            sz = 62f;
        }

        p.setTextSize(sz);
        float measured = p.measureText(name);
        if (measured > 330f) p.setTextSize(sz * 330f / measured);
        canvas.drawText(name, 200f, 137f, p);

        if ("com.amazon.amazonvideo.livingroom".equals(packageName)) {
            p.setColor(focused ? accentForPackage(packageName) : 0xFFE7EDFF);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4.5f);
            p.setStrokeCap(Paint.Cap.ROUND);
            Path smile = new Path();
            smile.moveTo(128, 152);
            smile.quadTo(200, 179, 271, 152);
            canvas.drawPath(smile, p);
        }
        canvas.restoreToCount(checkpoint);
    }

    @Override public void setAlpha(int a) { alpha = a; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return W; }
    @Override public int getIntrinsicHeight() { return H; }
}
