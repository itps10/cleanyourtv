package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Aurora Orbit — original circular streaming badge.
 * One cached set of shaders per tile size; lightweight on Android TV 11.
 * The native installed app logo is drawn by the ImageView above this background.
 */
final class AuroraGlassCard extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int tint;
    private final int top;
    private final int bottom;
    private boolean selected;
    private LinearGradient base;
    private RadialGradient aura;
    private RadialGradient shadow;
    private final RectF arc = new RectF();

    AuroraGlassCard(String pkg, int fallbackColor) {
        tint = AuroraFloatingLogo.hasBrandColor(pkg)
                ? AuroraFloatingLogo.accentForPackage(pkg) : fallbackColor;
        if ("com.netflix.ninja".equals(pkg)) {
            top = 0xFF6D102D; bottom = 0xFF0D0817;
        } else if ("com.amazon.amazonvideo.livingroom".equals(pkg)) {
            top = 0xFF075B8E; bottom = 0xFF071329;
        } else if ("com.disney.disneyplus".equals(pkg)) {
            top = 0xFF1B5DB2; bottom = 0xFF09112E;
        } else if ("com.wbd.stream".equals(pkg)) {
            top = 0xFF6835A9; bottom = 0xFF180B31;
        } else if ("com.spotify.tv.android".equals(pkg)) {
            top = 0xFF116F50; bottom = 0xFF041D18;
        } else if ("com.google.android.youtube.tv".equals(pkg)
                || "com.google.android.youtube.tvmusic".equals(pkg)) {
            top = 0xFF9A2047; bottom = 0xFF200A20;
        } else {
            // Unknown apps use their icon's dominant colour via fallbackColor.
            top = mix(0xFF111124, tint, 0.37f);
            bottom = 0xFF0A0C1F;
        }
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1f-t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1f-t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1f-t) + (b & 255) * t);
        return 0xFF000000 | (r<<16) | (g<<8) | bl;
    }

    private static int a(int color, int alpha) {
        return (color & 0xFFFFFF) | (alpha << 24);
    }

    void setSelected(boolean yes) {
        if (selected == yes) return;
        selected = yes;
        invalidateSelf();
    }

    @Override protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b);
        if (b.isEmpty()) return;
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float r = Math.min(b.width(), b.height()) * 0.5f;
        // iPhone-dark-inspired circular backing: charcoal with a whisper of
        // the app colour rather than a brightly coloured opaque ball.
        int darkTop = mix(0xFF090A12, top, 0.17f);
        int darkBottom = mix(0xFF070912, bottom, 0.14f);
        base = new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{darkTop, mix(darkTop, darkBottom, 0.62f), darkBottom},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP);
        aura = new RadialGradient(cx + 0.29f*r, cy - 0.47f*r, r*1.45f,
                new int[]{a(tint, 0x8B), a(tint, 0x35), a(tint,0)},
                new float[]{0f, 0.57f, 1f}, Shader.TileMode.CLAMP);
        shadow = new RadialGradient(cx, cy + r*0.13f, r*0.95f,
                new int[]{0x72030413, 0x29030413, 0x00030413},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float radius = Math.min(b.width(),b.height())*0.5f - 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(base);
        c.drawCircle(cx,cy,radius,paint);
        paint.setShader(aura);
        paint.setAlpha(selected ? 220 : 54);
        c.drawCircle(cx,cy,radius,paint);
        paint.setAlpha(255);
        paint.setShader(shadow);
        c.drawCircle(cx,cy,radius,paint);
        paint.setShader(null);

        // Coloured focus ring; idle badges keep a narrow subtle outline.
        // The actual focus outline is painted above the icon by AuroraOrbitRim.
        // No thin purple ring (or white reflection) is drawn on idle icons.
        paint.setStyle(Paint.Style.FILL);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter filter) {}
    @Override public int getOpacity() {return PixelFormat.TRANSLUCENT;}
}
