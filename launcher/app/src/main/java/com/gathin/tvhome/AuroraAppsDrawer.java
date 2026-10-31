package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Aurora Orbit 1.4: a full-screen, VERTICAL, 7-column app drawer.
 *
 * HOME stays exactly seven pinned favourites. Press DOWN to slide the
 * catalogue over the existing wallpaper. Unlike the legacy library's
 * horizontal popup, this view fills the screen and scrolls vertically.
 *
 * Only exported, launchable apps are displayed. Other installed Aurora
 * revisions and stock HOME launchers remain installed but are never shown as
 * redundant "Início" entries. No packages are uninstalled or disabled.
 */
final class AuroraAppsDrawer {
    private static final int COLS = 8;
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Activity activity;
    private final FrameLayout root;
    private final FrameLayout panel;
    private final RecyclerView grid;
    private final TextView countView;
    private final Runnable afterClose;
    private final AppGrid adapter = new AppGrid();
    /** At 256x256, keep at most 28 icons (~7 MB) on this 1.3 GB RAM television. */
    private final android.util.LruCache<String, AuroraIconSkin.Result> darkIconCache =
            new android.util.LruCache<>(28);
    private boolean closed;
    private final int tileSide;
    private final int densityWidth;

    private static final class App {
        final ComponentName component;
        final String packageName;
        final String label;
        final boolean leanback;
        App(ResolveInfo info, String name, boolean tv) {
            component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            packageName = info.activityInfo.packageName;
            label = name;
            leanback = tv;
        }
    }

    AuroraAppsDrawer(Activity activity, FrameLayout root, boolean animations, Runnable afterClose) {
        this.activity = activity;
        this.root = root;
        this.afterClose = afterClose;
        float density = activity.getResources().getDisplayMetrics().density;
        densityWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int available = densityWidth - Math.round(100 * density);
        // v1.7: smaller circles leave genuinely clear air around the selected rim.
        // 56dp maximum instead of 70dp; the row still contains eight icons.
        tileSide = Math.min(Math.round(56 * density), Math.max(Math.round(38 * density),
                available / COLS - Math.round(27 * density)));

        panel = new FrameLayout(activity);
        GradientDrawable backdrop = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF00B0D22, 0xF50C1029, 0xFB080A18});
        panel.setBackground(backdrop);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setFocusable(true);
        panel.setClickable(true);
        panel.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        panel.setTag("aurora-all-apps");

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        // Prevent the selected circle in the first/last visible row from
        // being cut by parent clipping or top-of-scroll alignment.
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(dp(46), dp(34), dp(46), dp(21));
        panel.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout heading = new LinearLayout(activity);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Todos os aplicativos", 29, Color.WHITE);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 1));
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        countView = text("Carregando...", 14, 0xFFC4B4DD);
        countView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        heading.addView(countView, new LinearLayout.LayoutParams(dp(200), dp(48)));
        content.addView(heading, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView hint = text("← → Escolher       ↓ Ver mais       OK Abrir       ↑ / Voltar: início", 13,
                0xFFC7B6E7);
        hint.setPadding(dp(2), 0, 0, dp(8));
        content.addView(hint, new LinearLayout.LayoutParams(-1, dp(39)));

        grid = new RecyclerView(activity);
        grid.setLayoutManager(new GridLayoutManager(activity, COLS, RecyclerView.VERTICAL, false));
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setHasFixedSize(true);
        // Room for the focus animation: otherwise the top row is cropped.
        grid.setPadding(0, dp(44), 0, dp(55));
        grid.setItemAnimator(null);
        grid.setAdapter(adapter);
        content.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));

        root.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Use a short scene transition, without video/blur/continual animation.
        if (animations) {
            panel.setTranslationY(activity.getResources().getDisplayMetrics().heightPixels);
            panel.animate().translationY(0f).setDuration(270).start();
        }
        load();
    }

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, int sp, int ink) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(ink);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private static boolean isExtraHome(String pkg) {
        // Every prior CleanYourTV/Aurora fork used this namespace, including
        // com.gathin.tvhome itself. Do not show duplicate "Início" launchers.
        return pkg.startsWith("com.gathin.tvhome")
                || pkg.equals("com.google.android.tvlauncher");
    }

    private void load() {
        PackageManager pm = activity.getPackageManager();
        LOADER.execute(() -> {
            Map<String, App> apps = new LinkedHashMap<>();
            boolean hasTvAberta = false; // show only one TV Aberta entry
            for (String category : new String[]{
                    Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
                for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                    if (ri.activityInfo == null || !ri.activityInfo.exported) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (isExtraHome(pkg) || apps.containsKey(pkg)) continue;
                    String name;
                    try {
                        name = String.valueOf(ri.loadLabel(pm));
                    } catch (RuntimeException err) {
                        name = pkg;
                    }
                    // Some television firmware exposes TV Aberta under multiple
                    // packages/activities. Keep the first TV-launchable entry;
                    // don't uninstall or disable any of the underlying apps.
                    String key = java.text.Normalizer.normalize(name,
                            java.text.Normalizer.Form.NFD)
                            .replaceAll("\\p{M}+", "")
                            .toLowerCase(Locale.ROOT)
                            .replace(" ", "").trim();
                    if ("tvaberta".equals(key)) {
                        if (hasTvAberta) continue;
                        hasTvAberta = true;
                    }
                    apps.put(pkg, new App(ri, name,
                            category.equals(Intent.CATEGORY_LEANBACK_LAUNCHER)));
                }
            }
            List<App> found = new ArrayList<>(apps.values());
            Collator collator = Collator.getInstance(new Locale("pt", "BR"));
            found.sort((a, b) -> collator.compare(a.label, b.label));
            main.post(() -> {
                if (closed || !panel.isAttachedToWindow()) return;
                adapter.replace(found);
                countView.setText(found.size() + " apps");
                if (!found.isEmpty()) grid.post(() -> focusAt(0));
                else countView.setText("Nenhum app disponível");
            });
        });
    }

    private void focusAt(int position) {
        if (closed || adapter.getItemCount() == 0) return;
        position = Math.max(0, Math.min(adapter.getItemCount() - 1, position));
        RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(position);
        if (holder != null && holder.itemView.findViewById(android.R.id.icon).requestFocus()) return;
        final int target = position;
        grid.scrollToPosition(target);
        grid.post(() -> {
            if (closed) return;
            RecyclerView.ViewHolder visible = grid.findViewHolderForAdapterPosition(target);
            if (visible != null) {
                View button = visible.itemView.findViewById(android.R.id.icon);
                if (button != null) button.requestFocus();
            }
        });
    }

    private void open(App app) {
        close(false);
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (app.leanback) intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(activity, "Não foi possível abrir " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    void close() { close(true); }

    /** HOME button restores the seven favourites, without focus jumping back later. */
    void dismissForHome() { close(false); }

    private void close(boolean returnFocus) {
        if (closed) return;
        closed = true;
        panel.animate().cancel();
        root.removeView(panel);
        adapter.clear();
        if (returnFocus && afterClose != null) root.post(afterClose);
    }

    boolean isOpen() { return !closed; }

    private final class AppGrid extends RecyclerView.Adapter<Tile> {
        private final List<App> apps = new ArrayList<>();
        void replace(List<App> next) {
            apps.clear();
            apps.addAll(next);
            notifyDataSetChanged();
        }
        void clear() {
            apps.clear();
            notifyDataSetChanged();
        }
        @Override public int getItemCount() { return apps.size(); }

        @NonNull @Override public Tile onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            // Inset selected circles from row edges so the border stays visible.
            item.setPadding(dp(5), dp(24), dp(5), dp(18));
            item.setClipChildren(false);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, tileSide + dp(88));
            item.setLayoutParams(lp);

            FrameLayout button = new FrameLayout(activity);
            button.setId(android.R.id.icon);
            button.setFocusable(true);
            button.setClickable(true);
            button.setClipToOutline(true);
            button.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline outline) {
                    outline.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(0xFF211B37);
            button.setBackground(background);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(tileSide, tileSide);
            item.addView(button, bp);

            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            button.addView(image);
            TextView label = text("", 12, 0xFFF0EAF9);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, dp(30));
            tlp.topMargin = dp(4);
            item.addView(label, tlp);

            return new Tile(item, button, image, label);
        }

        @Override public void onBindViewHolder(@NonNull Tile tile, int position) {
            App app = apps.get(position);
            tile.name.setText(app.label);
            AuroraIconSkin.Result painted = darkIconCache.get(app.packageName);
            if (painted == null) {
                Drawable drawable;
                try {
                    drawable = activity.getPackageManager().getApplicationIcon(app.packageName);
                } catch (PackageManager.NameNotFoundException | RuntimeException ex) {
                    drawable = activity.getDrawable(android.R.drawable.sym_def_app_icon);
                }
                // TV Aberta and Play Filmes get clear TV and movie symbols.
                // Other apps retain their native brand icon, with a dark icon skin.
                painted = AuroraIconSkin.forApp(activity, app.packageName, app.label, drawable);
                darkIconCache.put(app.packageName, painted);
            }
            tile.image.setImageDrawable(painted.icon);
            int accent = painted.accent;
            AuroraOrbitRim rim = new AuroraOrbitRim(accent);
            rim.setFocused(tile.button.hasFocus());
            tile.button.setForeground(rim);
            tile.button.setOnClickListener(v -> open(app));
            tile.button.setOnFocusChangeListener((v, focused) -> {
                Drawable foreground = tile.button.getForeground();
                if (foreground instanceof AuroraOrbitRim)
                    ((AuroraOrbitRim) foreground).setFocused(focused);
                v.animate().cancel();
                // Tiny highlight; enlarging 5.5% could clip the upper edge on
                // TPV firmware when RecyclerView scrolls a row to viewport top.
                // Colour ring remains the primary, accessible focus indicator.
                v.animate().scaleX(focused ? 1.02f : 1f)
                        .scaleY(focused ? 1.02f : 1f)
                        .setDuration(focused ? 145 : 115).start();
                tile.name.setTextColor(focused ? Color.WHITE : 0xFFD0C4DE);
            });
            tile.button.setOnKeyListener((v, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                int pos = tile.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                if (key == KeyEvent.KEYCODE_DPAD_UP) {
                    if (pos < COLS) { close(); return true; }
                    focusAt(pos - COLS);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (pos + COLS < apps.size()) {
                        focusAt(pos + COLS);
                    }
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (pos % COLS == 0) return true;
                    focusAt(pos - 1);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (pos % COLS == COLS - 1 || pos + 1 >= apps.size()) return true;
                    focusAt(pos + 1);
                    return true;
                }
                return false;
            });
        }
    }

    private static final class Tile extends RecyclerView.ViewHolder {
        final FrameLayout button;
        final ImageView image;
        final TextView name;
        Tile(View root, FrameLayout button, ImageView image, TextView name) {
            super(root);
            this.button = button;
            this.image = image;
            this.name = name;
        }
    }
}
