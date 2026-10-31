package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Aurora Luxe landscape. A static, GPU-cached 960x540 cinematic background:
 * plum-to-navy dusk, mountain silhouettes, pale horizon, stars and lake reflections.
 * Paint once; no video, network, particles or repeating frame callbacks.
 */
public final class AuroraLandscapeView extends View {
    private Bitmap cached;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public AuroraLandscapeView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        if (cached == null || cached.isRecycled()) cached = render();
        p.setColor(Color.WHITE);
        p.setShader(null);
        p.setAlpha(255);
        canvas.drawBitmap(cached, null,
                new android.graphics.Rect(0, 0, getWidth(), getHeight()), p);
    }

    private static Paint fill(int color) {
        Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        q.setColor(color);
        q.setStyle(Paint.Style.FILL);
        return q;
    }

    private Bitmap render() {
        final int w = 960, h = 540;
        Bitmap image = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(image);
        Paint q = fill(Color.WHITE);
        q.setShader(new LinearGradient(0, 0, w, h, new int[]{
                0xFF080918, 0xFF150E34, 0xFF28113D, 0xFF542452, 0xFF100B22
        }, new float[]{0f, 0.28f, 0.56f, 0.80f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, q);
        q.setShader(null);

        // Atmospheric lights. Radials are baked once into the cached bitmap.
        q.setShader(new RadialGradient(705f, 235f, 335f,
                new int[]{0xB7935EE9, 0x387D3BBB, 0x00471B7C},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(705f, 235f, 335f, q);
        q.setShader(new RadialGradient(580f, 330f, 310f,
                new int[]{0xB15F357E, 0x34703B9A, 0x00512F73},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(580f, 330f, 310f, q);
        q.setShader(null);

        // Moon and subtle halo: offset so it never competes with the clock.
        q.setShader(new RadialGradient(782f, 126f, 93f,
                new int[]{0x73EBD8FF, 0x1E947CCB, 0x00947CCB},
                new float[]{0f, 0.41f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(782f, 126f, 93f, q);
        q.setShader(null);
        q.setColor(0xFFE1D5F6);
        c.drawCircle(782f, 126f, 13f, q);

        // Dust and sparse stars (not animated).
        Random random = new Random(85462L);
        for (int i = 0; i < 105; i++) {
            float x = random.nextFloat() * w, y = random.nextFloat() * 310f;
            q.setColor((0x35 + random.nextInt(0x45)) << 24 | 0xDDD6FC);
            c.drawCircle(x, y, 0.35f + random.nextFloat() * 1.15f, q);
        }

        // Far violet ridges, each layer with a soft gradient.
        ridge(c, new float[][]{{0,358},{95,313},{182,339},{274,291},{362,335},{460,298},{562,345},{667,290},{766,314},{849,274},{960,327}},
                0xFF42345C, 0xFF1B193C, 540);
        ridge(c, new float[][]{{0,377},{95,358},{169,382},{265,340},{340,373},{444,337},{538,370},{632,322},{733,362},{842,321},{960,352}},
                0xFF352945, 0xFF0D1531, 540);

        // Still lake with violet horizon reflection. Soft central brightness, dark corners.
        q.setShader(new LinearGradient(0, 365, 0, h,
                new int[]{0xFF3E2951, 0xFF1B1939, 0xFF0A0D22},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 369, w, h, q);
        q.setShader(null);
        // Rippling reflection tapering under the distant moon.
        for (int k = 0; k < 30; k++) {
            float y = 371 + k * 5.4f;
            float ww = 3 + k * 2.1f + random.nextInt(19);
            q.setColor((0x32 - (k/2)) << 24 | 0xB6A1FA);
            q.setStrokeWidth(1.0f + random.nextFloat());
            c.drawLine(782-ww, y, 782+ww, y, q);
        }
        // Near black shore, covering the lower zone behind the app dock.
        ridge(c, new float[][]{{0,475},{114,482},{210,462},{330,490},{440,471},{530,487},{610,465},{710,491},{826,474},{960,492}},
                0xFF15152E, 0xFF060A18, 540);

        // Gentle edge vignette to keep the launcher UI readable.
        q.setShader(new RadialGradient(w * 0.51f, h * 0.4f, 610f,
                new int[]{0x00100A1C, 0x33100A1C, 0xB70B0A19},
                new float[]{0f, 0.61f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, q);
        q.setShader(null);
        return image;
    }

    private void ridge(Canvas canvas, float[][] peaks, int upper, int lower, int bottom) {
        Path shape = new Path();
        shape.moveTo(peaks[0][0], peaks[0][1]);
        for (int i = 1; i < peaks.length; i++) {
            shape.lineTo(peaks[i][0], peaks[i][1]);
        }
        shape.lineTo(960, bottom);
        shape.lineTo(0, bottom);
        shape.close();
        p.setShader(new LinearGradient(0, 280, 0, bottom, upper, lower, Shader.TileMode.CLAMP));
        p.setAlpha(255);
        canvas.drawPath(shape, p);
        p.setShader(null);
    }

    @Override protected void onDetachedFromWindow() {
        if (cached != null) {
            cached.recycle();
            cached = null;
        }
        super.onDetachedFromWindow();
    }
}
