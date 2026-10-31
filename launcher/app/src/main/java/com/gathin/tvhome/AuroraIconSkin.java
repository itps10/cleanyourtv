package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.text.Normalizer;
import java.util.Locale;

/**
 * iOS-dark-inspired icon treatment, performed ONCE per original app artwork.
 *
 * This is a local visual effect only: it does not alter installed APK icons.
 * Flat/white square backdrops are shifted toward midnight black; colourful
 * emblems and photographs are preserved. Always use the real installed image
 * except for the two user-requested TV/film fallback icons.
 */
final class AuroraIconSkin {
    static final int SIZE = 256; // high-resolution 256x256 icon atlas, cached per app
    private AuroraIconSkin() {}

    static final class Result {
        final Drawable icon;
        final int accent;
        Result(Drawable icon, int accent) {
            this.icon = icon; this.accent = accent;
        }
    }

    static Result forApp(Context context, String pkg, String label, Drawable original) {
        int special = specialKind(label);
        int forced = special == 1 ? 0xFF49C7E9 : special == 2 ? 0xFFF2A15D : 0;
        if (special != 0) return drawSpecial(context, special, forced);

        Bitmap source = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(source);
        if (original == null) original = context.getDrawable(android.R.drawable.sym_def_app_icon);
        Rect old = new Rect(original.getBounds()); // snapshot: avoid mutating Drawable's bounds
        original.setBounds(0, 0, SIZE, SIZE);
        original.draw(canvas);
        original.setBounds(old);

        int[] pixels = new int[SIZE * SIZE];
        source.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE);
        source.recycle();

        int bg = commonCorner(pixels);
        int fallback = AuroraFloatingLogo.hasBrandColor(pkg)
                ? AuroraFloatingLogo.accentForPackage(pkg) : 0xFFA0B3D1;
        int accent = AuroraFloatingLogo.hasBrandColor(pkg)
                ? fallback : dominantInk(pixels, bg, fallback);
        int dark = darkBase(accent);
        for (int i = 0; i < pixels.length; i++) {
            int v = pixels[i], a = Color.alpha(v);
            if (a < 8) continue;
            int r = Color.red(v), g = Color.green(v), b = Color.blue(v);
            if (bg != 0 && distance(v, bg) < 65) {
                // Replace the bright flat square of a traditional app icon
                // with a dark graphite base, keeping the original emblem.
                float mix = Math.min(1f, Math.max(0f, (65f - distance(v, bg))/35f));
                pixels[i] = Color.argb(a,
                        blend(r, Color.red(dark), mix),
                        blend(g, Color.green(dark), mix),
                        blend(b, Color.blue(dark), mix));
            } else {
                // Saturated logo paint is retained. Lift low-contrast greys
                // slightly so a dark emblem remains readable on dark backing.
                int max = Math.max(r, Math.max(g,b)), min = Math.min(r, Math.min(g,b));
                if (max < 62 && max-min < 24 && bg != 0) {
                    pixels[i] = Color.argb(a, Math.min(255,r+67),
                            Math.min(255,g+67), Math.min(255,b+74));
                }
            }
        }
        Bitmap output = Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888);
        BitmapDrawable sharp = new BitmapDrawable(context.getResources(), output);
        sharp.setFilterBitmap(true);
        sharp.setDither(true);
        return new Result(sharp, accent);
    }

    private static int blend(int a, int b, float t) {
        return Math.round(a * (1f-t) + b*t);
    }
    private static int darkBase(int tint) {
        return Color.rgb(10 + Color.red(tint)/17, 12 + Color.green(tint)/17,
                20 + Color.blue(tint)/17);
    }
    private static int distance(int a, int b) {
        return (Math.abs(Color.red(a)-Color.red(b)) +
                Math.abs(Color.green(a)-Color.green(b)) +
                Math.abs(Color.blue(a)-Color.blue(b))) / 3;
    }
    private static int commonCorner(int[] p) {
        int pad = 6, n = SIZE - 7;
        int[] cs = {p[pad*SIZE+pad],p[pad*SIZE+n],p[n*SIZE+pad],p[n*SIZE+n]};
        for (int v: cs) if (Color.alpha(v) < 225) return 0;
        int avg = Color.rgb((Color.red(cs[0])+Color.red(cs[1])+Color.red(cs[2])+Color.red(cs[3]))/4,
                (Color.green(cs[0])+Color.green(cs[1])+Color.green(cs[2])+Color.green(cs[3]))/4,
                (Color.blue(cs[0])+Color.blue(cs[1])+Color.blue(cs[2])+Color.blue(cs[3]))/4);
        for (int v:cs) if(distance(v,avg)>35) return 0; // photographic/noisy icon: preserve
        return avg;
    }

    private static int dominantInk(int[] p, int bg, int fallback) {
        float best = -1f;
        int colour = fallback;
        float[] hsv = new float[3]; // reuse array; reduce GC during drawer scrolling
        for(int i=0;i<p.length;i+=23) {
            int c=p[i]; if(Color.alpha(c)<195)continue;
            if(bg!=0&&distance(c,bg)<55)continue;
            Color.colorToHSV(c,hsv);
            float score=hsv[1] * (0.7f + hsv[2]*0.3f);
            if(score>best && hsv[2] > 0.21f) { best=score; colour=c|0xFF000000; }
        }
        return colour;
    }

    /** Detect both named apps even on firmware with unrecognized package IDs. */
    static int specialKind(String label) {
        if(label==null)return 0;
        String norm=Normalizer.normalize(label,Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+","")
                .toLowerCase(Locale.ROOT).trim();
        if(norm.equals("tv aberta")||norm.equals("tv ao vivo")||norm.equals("tvaberta"))return 1;
        if(norm.equals("play filmes")||norm.equals("playfilmes")||norm.equals("play filme"))return 2;
        return 0;
    }

    private static Result drawSpecial(Context context,int kind,int accent) {
        Bitmap bmp=Bitmap.createBitmap(SIZE,SIZE,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        c.scale(SIZE / 144f, SIZE / 144f); // render original vector art at 256px
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(kind==1?0xFF0B2130:0xFF231825);
        c.drawCircle(72,72,72,p);

        p.setColor((accent&0x00FFFFFF)|0xFF000000);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        if(kind==1) {
            // TV Aberta: a live television screen, antenna and small bright signal.
            c.drawRoundRect(28,42,116,103,10,10,p);
            c.drawLine(72,104,72,116,p);
            c.drawLine(53,119,91,119,p);
            c.drawLine(55,33,72,42,p); c.drawLine(89,33,72,42,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFDAF8FF);
            Path play=new Path();play.moveTo(65,61);play.lineTo(65,88);
            play.lineTo(89,74.5f);play.close();c.drawPath(play,p);
        } else {
            // Play Filmes: a movie-frame outline plus a single play triangle.
            c.drawRoundRect(28,40,116,103,9,9,p);
            for(int i=0;i<4;i++){
                float y=49+i*13;
                c.drawLine(36,y,40,y,p);
                c.drawLine(104,y,108,y,p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFE3C5);
            Path play=new Path();play.moveTo(64,57);play.lineTo(64,89);
            play.lineTo(92,73);play.close();c.drawPath(play,p);
        }
        BitmapDrawable sharp = new BitmapDrawable(context.getResources(), bmp);
        sharp.setFilterBitmap(true);
        sharp.setDither(true);
        return new Result(sharp, accent);
    }
}
