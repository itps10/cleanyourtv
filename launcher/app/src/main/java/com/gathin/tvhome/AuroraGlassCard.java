package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
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
 * A lightweight glass tile with a restrained per-platform highlight.
 * No bitmap allocations, CPU blur, RenderScript or repeating animators.
 * One instance is held by each visible tile.
 */
final class AuroraGlassCard extends Drawable {
    private final Paint surface = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int accent;
    private boolean selected;
    private LinearGradient restingFill;
    private RadialGradient activeGlow;

    AuroraGlassCard(int accent) {
        this.accent = accent | 0xFF000000;
        edge.setStyle(Paint.Style.STROKE);
    }

    void setSelected(boolean value) {
        if (selected != value) {
            selected = value;
            invalidateSelf();
        }
    }

    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.isEmpty()) return;
        restingFill = new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                new int[]{0xA819142C, 0x9C0C0A20, 0xC30D0A22},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP);
        activeGlow = new RadialGradient(bounds.exactCenterX(), bounds.top + bounds.height() * 0.35f,
                Math.max(bounds.width(), bounds.height()) * 0.73f,
                new int[]{withAlpha(accent, 0x72), withAlpha(accent, 0x25), withAlpha(accent, 0)},
                new float[]{0f, 0.57f, 1f}, Shader.TileMode.CLAMP);
    }

    private static int withAlpha(int c, int alpha) {
        return (c & 0xFFFFFF) | (alpha << 24);
    }

    @Override public void draw(Canvas canvas) {
        if (getBounds().isEmpty()) return;
        float w = getBounds().width();
        float h = getBounds().height();
        float radius = Math.min(w, h) * 0.17f;
        rect.set(getBounds().left + 1.5f, getBounds().top + 1.5f,
                getBounds().right - 1.5f, getBounds().bottom - 1.5f);

        surface.setStyle(Paint.Style.FILL);
        surface.setShader(restingFill);
        canvas.drawRoundRect(rect, radius, radius, surface);
        surface.setShader(null);
        if (selected && activeGlow != null) {
            surface.setShader(activeGlow);
            canvas.drawRoundRect(rect, radius, radius, surface);
            surface.setShader(null);
        }

        edge.setStrokeWidth(selected ? 3f : 1.2f);
        edge.setColor(selected ? withAlpha(accent, 0xF2) : 0x62CEC3E7);
        canvas.drawRoundRect(rect, radius, radius, edge);

        // A precise specular top rim, not an expensive Gaussian blur.
        edge.setStrokeWidth(0.7f);
        edge.setColor(selected ? 0xB4FFFFFF : 0x45FFFFFF);
        canvas.drawLine(rect.left + radius, rect.top + 2, rect.right - radius, rect.top + 2, edge);
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
