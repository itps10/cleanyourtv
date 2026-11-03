package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A passive Champions League score peek for the Max/HBO tile.
 *
 * It never reads Max/HBO private data or overlays another application.
 * Source: public ESPN site scoreboard endpoint (undocumented; may change).
 * No synthetic scores. Polling runs only while Home is resumed and Max is
 * focused (75 seconds during live games, 5 minutes otherwise). One HTTPS
 * request collects current and recent events. No networking on UI thread.
 */
final class AuroraChampionsPanel {
    private static final String API =
            "https://site.api.espn.com/apis/site/v2/sports/soccer/uefa.champions/scoreboard";
    private static final long LIVE_REFRESH_MS = 75_000L;
    private static final long IDLE_REFRESH_MS = 300_000L;
    private static final int MAX_BYTES = 2_000_000;
    private static final DateTimeFormatter QUERY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", new Locale("pt","BR"))
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DAY_HOUR = DateTimeFormatter.ofPattern("dd/MM HH:mm", new Locale("pt","BR"))
            .withZone(ZoneId.systemDefault());

    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r,"aurora-ucl-scores");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger generation = new AtomicInteger();
    private final LinearLayout panel;
    private final TextView timestamp;
    private final LinearLayout matches;
    private final Runnable refresh = this::requestNow;
    private boolean resumed;
    private boolean selected;
    private boolean fetching;
    private boolean closed;
    private long fetchedAt;
    private Snapshot last;

    AuroraChampionsPanel(Activity activity, FrameLayout root) {
        this.activity = activity;
        final float scale = activity.getResources().getDisplayMetrics().density;
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(19),dp(17),dp(19),dp(17));
        panel.setClickable(false);
        panel.setFocusable(false);
        panel.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        panel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        panel.setElevation(dp(11));
        GradientDrawable backdrop = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xF7170E29,0xF70A0E1F,0xF5180D2B});
        backdrop.setCornerRadius(dp(20));
        backdrop.setStroke(dp(1),0x997B47B6);
        panel.setBackground(backdrop);

        TextView title = label("⚽  UEFA CHAMPIONS LEAGUE",16,0xFFEBDBFF,true);
        title.setLetterSpacing(0.06f);
        panel.addView(title,new LinearLayout.LayoutParams(-1,dp(30)));
        timestamp = label("Ao vivo e últimos jogos • carregando…",12,0xFFCBBADA,false);
        panel.addView(timestamp,new LinearLayout.LayoutParams(-1,dp(27)));
        View separator = new View(activity);
        separator.setBackgroundColor(0x496D5491);
        LinearLayout.LayoutParams sep = new LinearLayout.LayoutParams(-1,dp(1));
        sep.bottomMargin=dp(9);
        panel.addView(separator,sep);

        matches = new LinearLayout(activity);
        matches.setOrientation(LinearLayout.VERTICAL);
        panel.addView(matches,new LinearLayout.LayoutParams(-1,-2));

        TextView disclosure = label("Fonte: ESPN • atualização automática, sujeita a atraso",10,
                0xFFA99BBD,false);
        LinearLayout.LayoutParams note = new LinearLayout.LayoutParams(-1,-2);
        note.topMargin=dp(9);
        panel.addView(disclosure,note);

        // Place only on our Home, non-interactive, above the wallpaper/tiles.
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(387),-2,
                Gravity.TOP | Gravity.RIGHT);
        lp.topMargin=dp(157);
        lp.rightMargin=dp(55);
        root.addView(panel,lp);
        panel.setVisibility(View.GONE);
    }

    static boolean isMax(String pkg) {
        return "com.wbd.stream".equals(pkg)
                || "com.hbo.hbonow".equals(pkg)
                || "com.hbo.hbonow.tv".equals(pkg)
                || "com.wbd.max".equals(pkg)
                || "com.hbo.max".equals(pkg);
    }

    private int dp(float value) {
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }

    private TextView label(String value,int size,int color,boolean bold) {
        TextView view=new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if(bold)view.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return view;
    }

    void resume() {
        if(closed)return;
        resumed=true;
    }

    void pause() {
        resumed=false;
        hide();
    }

    void focus(String pkg) {
        if(closed)return;
        if(!isMax(pkg)||!resumed){hide();return;}
        if(!selected) {
            selected=true;
            generation.incrementAndGet();
            panel.setVisibility(View.VISIBLE);
            if(last==null) showMessage("Buscando placares da Champions…");
        }
        main.removeCallbacks(refresh);
        if(last!=null) render(last);
        if(last==null||System.currentTimeMillis()-fetchedAt>45_000L)requestNow();
        else schedule(last.live);
    }

    void hide() {
        selected=false;
        generation.incrementAndGet();
        main.removeCallbacks(refresh);
        panel.setVisibility(View.GONE);
    }

    private void schedule(boolean live) {
        main.removeCallbacks(refresh);
        if(selected&&resumed&&!closed)
            main.postDelayed(refresh,live?LIVE_REFRESH_MS:IDLE_REFRESH_MS);
    }

    private void requestNow() {
        if(!selected||!resumed||closed)return;
        if(fetching) {main.postDelayed(refresh,10_000L);return;}
        fetching=true;
        final int token=generation.get();
        try {
            worker.execute(() -> {
                Snapshot loaded=null;
                Exception failure=null;
                try { loaded=download(); }
                catch(Exception e) {failure=e;}
                final Snapshot data=loaded;
                final Exception error=failure;
                main.post(() -> {
                    fetching=false;
                    if(closed||!resumed||!selected||generation.get()!=token)return;
                    if(data!=null) {
                        last=data;
                        fetchedAt=System.currentTimeMillis();
                        render(data);
                    } else if(last!=null) {
                        render(last);
                        timestamp.setText("Sem atualização • último dado " + HOUR.format(
                                Instant.ofEpochMilli(fetchedAt)));
                    } else {
                        showMessage(error==null?"Placar indisponível no momento."
                                :"Sem dados da Champions. Verifique a internet.");
                    }
                    schedule(data!=null&&data.live);
                });
            });
        } catch(RuntimeException ignored) {
            fetching=false;
            if(selected)showMessage("Consulta indisponível no momento.");
        }
    }

    private static Snapshot download() throws Exception {
        // A single season-independent date range covers recent, live and soon.
        // The site API is unofficial; fall back to today's board if it rejects ranges.
        LocalDate today=LocalDate.now(ZoneId.systemDefault());
        String from=today.minusDays(17).format(QUERY);
        String to=today.plusDays(9).format(QUERY);
        JSONObject board=loadJson(API+"?dates="+from+"-"+to+"&limit=250");
        JSONArray events=board.optJSONArray("events");
        if(events==null||events.length()==0){
            board=loadJson(API+"?dates="+today.format(QUERY));
            events=board.optJSONArray("events");
        }
        Snapshot result=new Snapshot();
        if(events==null)return result;
        for(int i=0;i<events.length();i++) {
            JSONObject event=events.optJSONObject(i);
            if(event==null)continue;
            JSONObject competition=event.optJSONArray("competitions")==null?null:
                    event.optJSONArray("competitions").optJSONObject(0);
            if(competition==null)continue;
            JSONArray competitors=competition.optJSONArray("competitors");
            if(competitors==null||competitors.length()<2)continue;
            String home=null,away=null,homeScore=null,awayScore=null;
            for(int c=0;c<competitors.length();c++){
                JSONObject competitor=competitors.optJSONObject(c);
                if(competitor==null)continue;
                JSONObject team=competitor.optJSONObject("team");
                String name=team==null?"":team.optString("abbreviation","");
                if(name.isEmpty())name=team==null?"?":team.optString("shortDisplayName",
                        team.optString("displayName","?"));
                String score=score(competitor);
                if("home".equalsIgnoreCase(competitor.optString("homeAway"))) {
                    home=name;homeScore=score;
                } else if("away".equalsIgnoreCase(competitor.optString("homeAway"))) {
                    away=name;awayScore=score;
                }
            }
            if(home==null||away==null)continue;
            JSONObject status=competition.optJSONObject("status");
            if(status==null)status=event.optJSONObject("status");
            JSONObject type=status==null?null:status.optJSONObject("type");
            String state=type==null?"":type.optString("state","");
            String kind=type==null?"":type.optString("name","");
            boolean complete=(type!=null&&type.optBoolean("completed",false))
                    ||"post".equals(state)||"STATUS_FINAL".equals(kind);
            boolean playing="in".equals(state)||kind.contains("IN_PROGRESS")
                    ||kind.contains("HALFTIME");
            long date=0L;
            try {date=Instant.parse(event.optString("date")).toEpochMilli();}
            catch(Exception ignored) {}
            String detail=type==null?"":type.optString("shortDetail","");
            Match match=new Match(home,away,homeScore,awayScore,date,detail);
            if(playing) {result.playing.add(match);result.live=true;}
            else if(complete) result.finished.add(match);
            else result.upcoming.add(match);
        }
        Collections.sort(result.playing,Comparator.comparingLong(m->m.date));
        result.finished.sort((a,b)->Long.compare(b.date,a.date));
        result.upcoming.sort(Comparator.comparingLong(m->m.date));
        return result;
    }

    private static String score(JSONObject competitor) {
        Object obj=competitor.opt("score");
        if(obj instanceof JSONObject) {
            JSONObject s=(JSONObject)obj;
            return s.optString("displayValue",s.optString("value","-"));
        }
        return obj==null?"-":String.valueOf(obj);
    }

    private static JSONObject loadJson(String url) throws Exception {
        HttpsURLConnection conn=(HttpsURLConnection)new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6_000);
        conn.setReadTimeout(7_000);
        conn.setRequestProperty("Accept","application/json");
        conn.setRequestProperty("User-Agent","AuroraHome/1.8 (Android TV; public UCL scores)");
        try {
            if(conn.getResponseCode()!=200)throw new java.io.IOException("HTTP "+conn.getResponseCode());
            try(InputStream input=conn.getInputStream();
                InputStreamReader r=new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8);
                BufferedReader reader=new BufferedReader(r)) {
                StringBuilder b=new StringBuilder();
                char[] buf=new char[4096];
                int n;
                while((n=reader.read(buf))!=-1){
                    if(b.length()+n>MAX_BYTES)throw new java.io.IOException("oversized scoreboard");
                    b.append(buf,0,n);
                }
                return new JSONObject(b.toString());
            }
        } finally {conn.disconnect();}
    }

    private void showMessage(String message) {
        matches.removeAllViews();
        TextView row=label(message,13,0xFFE6DDF0,false);
        row.setSingleLine(false);
        matches.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));
        timestamp.setText("UEFA Champions League");
    }

    private void render(Snapshot s) {
        matches.removeAllViews();
        timestamp.setText((s.live?"● AO VIVO  ":"Últimos resultados  ")
                + "• atualizado " + HOUR.format(Instant.ofEpochMilli(fetchedAt)));
        int count=0;
        if(!s.playing.isEmpty()) count+=section("AO VIVO",s.playing,3,0xFFFF6D86,0);
        if(!s.finished.isEmpty()) count+=section("ÚLTIMOS RESULTADOS",s.finished,
                Math.max(0,6-count),0xFFD9C6EF,1);
        if(!s.upcoming.isEmpty()&&count<6)count+=section("PRÓXIMOS JOGOS",s.upcoming,
                Math.min(2,6-count),0xFF98CDE7,2);
        if(count==0)showMessage("Nenhum jogo nesse período.");
    }

    private int section(String title,List<Match> events,int limit,int tint,int kind) {
        if(limit<=0)return 0;
        TextView header=label(title,11,tint,true);
        LinearLayout.LayoutParams head=new LinearLayout.LayoutParams(-1,dp(26));
        head.topMargin=dp(2);
        matches.addView(header,head);
        int total=Math.min(limit,events.size());
        for(int i=0;i<total;i++){
            Match m=events.get(i);
            String line;
            if(kind==2) {
                line=(m.date>0?DAY_HOUR.format(Instant.ofEpochMilli(m.date)):"A definir")
                        +"  ·  "+m.home+" × "+m.away;
            } else {
                String result=m.home+"  "+m.homeScore+" – "+m.awayScore+"  "+m.away;
                line=kind==0?("● "+(m.detail.isEmpty()?"AO VIVO":m.detail)
                        +"  ·  "+result):result;
            }
            TextView text=label(line,14,0xFFF4EBFF,kind==0);
            matches.addView(text,new LinearLayout.LayoutParams(-1,dp(29)));
        }
        return total;
    }

    void shutdown() {
        closed=true;
        hide();
        worker.shutdownNow();
    }

    private static final class Match {
        final String home,away,homeScore,awayScore,detail;
        final long date;
        Match(String home,String away,String homeScore,String awayScore,long date,String detail){
            this.home=home;this.away=away;this.homeScore=homeScore;this.awayScore=awayScore;
            this.date=date;this.detail=detail;
        }
    }
    private static final class Snapshot {
        final List<Match> playing=new ArrayList<>();
        final List<Match> finished=new ArrayList<>();
        final List<Match> upcoming=new ArrayList<>();
        boolean live;
    }
}
