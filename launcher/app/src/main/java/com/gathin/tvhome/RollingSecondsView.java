package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

/**
 * Small, clipped two-digit rolling seconds dial next to the system TextClock.
 * Only invalidates a 38dp-wide region and does not run while the Home window is hidden.
 */
public final class RollingSecondsView extends View {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private ValueAnimator slide;
    private boolean running;
    private int seconds = -1;
    private int previous = -1;
    private float progress = 1f;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            int now = (int) ((System.currentTimeMillis() / 1000L) % 60L);
            if (seconds != now) {
                int old = seconds;
                seconds = now;
                if (old >= 0 && ((old + 1) % 60) == now) {
                    previous = old;
                    progress = 0f;
                    if (slide != null) slide.cancel();
                    slide = ValueAnimator.ofFloat(0f, 1f);
                    slide.setDuration(250L);
                    slide.setInterpolator(new DecelerateInterpolator(1.15f));
                    slide.addUpdateListener(a -> {
                        progress = (float) a.getAnimatedValue();
                        invalidate();
                    });
                    slide.start();
                } else {
                    previous = -1;
                    progress = 1f;
                    invalidate();
                }
            }
            long delay = 1010L - (System.currentTimeMillis() % 1000L);
            handler.postDelayed(this, delay);
        }
    };

    public RollingSecondsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFFD8B8FF);
        paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(21f * getResources().getDisplayMetrics().scaledDensity);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setRunning(getWindowVisibility() == VISIBLE);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        setRunning(visibility == VISIBLE && isAttachedToWindow());
    }

    @Override protected void onDetachedFromWindow() {
        setRunning(false);
        super.onDetachedFromWindow();
    }

    private void setRunning(boolean enabled) {
        if (running == enabled) return;
        running = enabled;
        handler.removeCallbacks(tick);
        if (slide != null) {
            slide.cancel();
            slide = null;
        }
        if (enabled) {
            seconds = -1;
            previous = -1;
            progress = 1f;
            tick.run();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (seconds < 0) return;
        final float midX = getWidth() / 2f;
        Paint.FontMetrics fm = paint.getFontMetrics();
        final float baseline = (getHeight() - fm.ascent - fm.descent) / 2f;
        final float travel = getHeight() * 0.83f;
        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        if (previous >= 0 && progress < 1f) {
            paint.setAlpha(Math.round(220 * (1f - progress)));
            canvas.drawText(String.format(Locale.US, "%02d", previous),
                    midX, baseline - travel * progress, paint);
            paint.setAlpha(Math.round(240 * progress));
            canvas.drawText(String.format(Locale.US, "%02d", seconds),
                    midX, baseline + travel * (1f - progress), paint);
        } else {
            paint.setAlpha(240);
            canvas.drawText(String.format(Locale.US, "%02d", seconds), midX, baseline, paint);
        }
        canvas.restore();
    }
}
