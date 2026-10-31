package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Always drawn ABOVE the installed app's full-bleed icon or banner.
 * The previous Orbit version placed the focus outline in the BACKGROUND;
 * the artwork hid it when enlarged to fill the circle.
 *
 * All strokes are inside bounds so the circle is properly clipped and
 * still shows a premium, lightweight brand-coloured focus ring.
 */
final class AuroraOrbitRim extends Drawable {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private final int accent;
    private boolean focused;

    AuroraOrbitRim(int accent) {
        this.accent = 0xFF000000 | (accent & 0x00FFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    void setFocused(boolean focus) {
        if (focused == focus) return;
        focused = focus;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty() || !focused) return; // no purple/faint outline on idle icons
        float r = Math.min(b.width(), b.height()) * 0.5f;
        float x = b.exactCenterX(), y = b.exactCenterY();
        stroke.setShader(null);
        stroke.setStrokeWidth(Math.max(2.7f, r * 0.055f));
        stroke.setColor(0xFF000000 | (accent & 0xFFFFFF));
        canvas.drawCircle(x, y, r - stroke.getStrokeWidth() * 0.62f, stroke);

        // A short glossy reflection on the upper edge: cheap, no bitmap blur.
        stroke.setStrokeWidth(Math.max(0.85f, r * 0.018f));
        stroke.setColor(0xB9FFFFFF);
        float inset = Math.max(3f, r * 0.09f);
        arc.set(x-r+inset, y-r+inset, x+r-inset, y+r-inset);
        canvas.drawArc(arc, 206, 114, false, stroke);
    }

    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter colorFilter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
