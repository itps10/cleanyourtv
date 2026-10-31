package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Aurora Neon v0.9. Individually coloured streaming cards:
 * dark centred logos, luminous corners, landscape silhouettes and edge highlights.
 *
 * The mockup is an inspiration: the card texture is original procedural artwork,
 * not screenshots from streaming services. Only one small Drawable is created
 * per visible tile; shaders are rebuilt on size changes, not every frame.
 */
final class AuroraGlassCard extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final int accent;
    private final int top;
    private final int bottom;
    private final int edgeAccent;
    private final int kind;
    private boolean selected;

    private LinearGradient baseFill;
    private RadialGradient halo;
    private RadialGradient darkCentre;
    private LinearGradient strip;

    AuroraGlassCard(String pkg, int brandTint) {
        accent = brandTint | 0xFF000000;
        if ("com.netflix.ninja".equals(pkg)) {
            kind = 1;
            top = 0xFF5C0829; bottom = 0xFF100816; edgeAccent = 0xFFFF2443;
        } else if ("com.amazon.amazonvideo.livingroom".equals(pkg)) {
            kind = 2;
            top = 0xFF085B98; bottom = 0xFF07172D; edgeAccent = 0xFF27D4FF;
        } else if ("com.disney.disneyplus".equals(pkg)) {
            kind = 3;
            top = 0xFF0658A4; bottom = 0xFF0B103A; edgeAccent = 0xFF4AAEFF;
        } else if ("com.wbd.stream".equals(pkg)
                || "com.hbo.hbonow".equals(pkg)
                || "com.hbo.hbonow.tv".equals(pkg)) {
            kind = 4;
            top = 0xFF612AB9; bottom = 0xFF1E0B42; edgeAccent = 0xFFAD59FF;
        } else if ("com.google.android.youtube.tv".equals(pkg)) {
            kind = 5;
            top = 0xFF810A2D; bottom = 0xFF1B071D; edgeAccent = 0xFFFF304E;
        } else if ("com.google.android.youtube.tvmusic".equals(pkg)
                || "com.google.android.apps.youtube.music".equals(pkg)) {
            kind = 6;
            top = 0xFF8B134F; bottom = 0xFF1E0B31; edgeAccent = 0xFFFF458D;
        } else if ("com.spotify.tv.android".equals(pkg)) {
            kind = 7;
            top = 0xFF0B6749; bottom = 0xFF041C21; edgeAccent = 0xFF23F0A2;
        } else if ("com.globo.globotv".equals(pkg)) {
            kind = 8;
            top = 0xFF954023; bottom = 0xFF29102E; edgeAccent = 0xFFFF8C57;
        } else {
            kind = 0;
            // Unknown app: keep the Aurora dark glass, but tint its edge
            // from Android's actual app icon instead of generic purple.
            top = 0xFF24213A; bottom = 0xFF0C0E28; edgeAccent = accent;
        }
    }

    void setSelected(boolean value) {
        if (selected == value) return;
        selected = value;
        invalidateSelf();
    }

    private static int alpha(int c, int a) {
        return (c & 0xFFFFFF) | (a << 24);
    }

    @Override
    protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b);
        if (b.isEmpty()) return;
        float w = b.width(), h = b.height();
        baseFill = new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{top, 0xFF090A22, bottom},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
        halo = new RadialGradient(b.right - 0.17f * w, b.top + 0.05f * h,
                w * 0.76f,
                new int[]{alpha(edgeAccent, 0xB8), alpha(edgeAccent, 0x45), alpha(edgeAccent, 0)},
                new float[]{0f, 0.41f, 1f}, Shader.TileMode.CLAMP);
        darkCentre = new RadialGradient(b.centerX(), b.centerY(), w * 0.53f,
                new int[]{0xA2050917, 0x76060919, 0x00060919},
                new float[]{0f, 0.51f, 1f}, Shader.TileMode.CLAMP);
        strip = new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{alpha(edgeAccent, 0), alpha(edgeAccent, 0xA0), alpha(edgeAccent, 0)},
                new float[]{0f, 0.49f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float w = b.width(), h = b.height();
        float x = b.left, y = b.top;
        float radius = Math.min(w, h) * 0.17f;
        rect.set(x + 1f, y + 1f, b.right - 1f, b.bottom - 1f);

        // Each badge has a saturated top corner and a deep central field,
        // so the highlighted app's logo remains legible in its own colour.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(baseFill);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(halo);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);

        // A second low-opacity landscape layer, different for each brand.
        // It is clipped by the card's own outline in AppAdapter.
        paint.setColor(alpha(edgeAccent, selected ? 0x42 : 0x2B));
        path.reset();
        path.moveTo(x, y + h * 0.85f);
        path.lineTo(x + w * 0.10f, y + h * 0.69f);
        path.lineTo(x + w * 0.22f, y + h * 0.81f);
        path.lineTo(x + w * 0.36f, y + h * (kind == 3 ? 0.58f : 0.75f));
        path.lineTo(x + w * 0.49f, y + h * 0.86f);
        path.lineTo(x + w * 0.65f, y + h * (kind == 4 ? 0.55f : 0.72f));
        path.lineTo(x + w * 0.83f, y + h * 0.78f);
        path.lineTo(x + w, y + h * 0.62f);
        path.lineTo(x + w, y + h);
        path.lineTo(x, y + h);
        path.close();
        canvas.drawPath(path, paint);

        // Diagonal light ribbons: quiet on idle, brighter only on focus.
        paint.setShader(strip);
        paint.setStrokeWidth(Math.max(1f, w * 0.009f));
        paint.setStyle(Paint.Style.STROKE);
        path.reset();
        if (kind == 4 || kind == 6) {
            path.moveTo(x - w * 0.07f, y + h * 0.86f);
            path.cubicTo(x + w * 0.30f, y + h * 0.31f, x + w * 0.57f, y + h * 1.04f,
                    x + w * 1.08f, y + h * 0.20f);
        } else {
            path.moveTo(x - w * 0.12f, y + h * 0.90f);
            path.quadTo(x + w * 0.55f, y + h * 0.50f, x + w * 1.10f, y + h * 0.11f);
        }
        canvas.drawPath(path, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);

        // Create the dark cradle for the brand colour only *inside* the card,
        // leaving the rim and corners vividly coloured like the screenshot.
        paint.setShader(darkCentre);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);

        // Thin glass rim and an inner top highlight.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? Math.max(2f, w * 0.013f) : Math.max(1.2f, w * 0.005f));
        paint.setColor(alpha(edgeAccent, selected ? 0xFF : 0xBD));
        canvas.drawRoundRect(rect, radius, radius, paint);

        paint.setStrokeWidth(Math.max(0.7f, w * 0.003f));
        paint.setColor(selected ? 0xB9FFFFFF : 0x52FFFFFF);
        canvas.drawLine(x + radius, y + 2f, b.right - radius, y + 2f, paint);

        paint.setStyle(Paint.Style.FILL);
        // Small reflection stripe near the lower edge, under the wordmark.
        paint.setColor(alpha(edgeAccent, selected ? 0x96 : 0x45));
        canvas.drawRoundRect(x + w * 0.25f, y + h * 0.963f, x + w * 0.75f,
                y + h * 0.979f, h * 0.01f, h * 0.01f, paint);
    }

    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
