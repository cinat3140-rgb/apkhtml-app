package com.cinat.apkhtml;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;

public class DetailActivity extends Activity {

    private static final String SITE_BASE = "https://cinat3140-rgb.github.io/apkhtml/#/oyun/";

    /* ---- tema ---- */
    private static final int C_BG     = 0xFF0F172A;
    private static final int C_CARD   = 0xFF1E293B;
    private static final int C_ACCENT = 0xFF38BDF8;
    private static final int C_TITLE  = 0xFFFFFFFF;
    private static final int C_SUB    = 0xFF94A3B8;
    private static final int C_MUTED  = 0xFF64748B;
    private static final int C_BTN1   = 0xFF10B981;
    private static final int C_BTN2   = 0xFF0D9488;

    private MainActivity.Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(C_BG);
            getWindow().setNavigationBarColor(C_BG);
        }

        String json = getIntent().getStringExtra("game_json");
        game = new MainActivity.Game();
        try {
            JSONObject o = new JSONObject(json);
            game.id = o.optInt("id");
            game.title = o.optString("title");
            game.description = o.optString("description");
            game.developer = o.optString("developer");
            game.genre = o.optString("genre");
            game.version = o.optString("version");
            game.size = o.optLong("size");
            game.androidVer = o.optString("androidVer");
            game.arch = o.optString("arch");
            game.packageName = o.optString("packageName");
            game.url = o.optString("url");
            game.sha256 = o.optString("sha256");
            game.coverUrl = o.optString("coverUrl");
        } catch (Exception ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(C_BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(C_BG);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(0), 0, dp(0), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        /* ---- cover banner ---- */
        FrameLayoutWrap banner = new FrameLayoutWrap(this);
        ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackgroundColor(Color.rgb(30, 41, 59));
        Bitmap bm = null;
        String coverUrl = game.coverUrl;
        if (coverUrl != null && !coverUrl.isEmpty()) {
            if (!coverUrl.startsWith("http")) coverUrl = "https://cinat3140-rgb.github.io/apkhtml/" + coverUrl;
            try {
                java.net.HttpURLConnection uc = (java.net.HttpURLConnection) new java.net.URL(coverUrl).openConnection();
                uc.setConnectTimeout(5000);
                uc.setReadTimeout(5000);
                if (uc.getResponseCode() == 200) {
                    bm = BitmapFactory.decodeStream(uc.getInputStream());
                }
                uc.disconnect();
            } catch (Exception ignored) {}
        }
        if (bm == null) bm = loadCover();
        if (bm != null) cover.setImageBitmap(bm);
        banner.addView(cover, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(200)));

        GradientDrawable scrim = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0x00000000, 0xCC0F172A});
        banner.setBackground(scrim);

        TextView badge = new TextView(this);
        badge.setText(game.genre != null && !game.genre.isEmpty() ? game.genre : "APK");
        badge.setTextColor(0xFFFFFFFF);
        badge.setTextSize(12);
        badge.setBackground(roundRect(0xCC1D4ED8, 14));
        badge.setPadding(dp(12), dp(5), dp(12), dp(5));
        badge.setGravity(Gravity.CENTER);
        FrameLayoutWrap.LayoutParams lpBadge = new FrameLayoutWrap.LayoutParams(
            FrameLayoutWrap.LayoutParams.WRAP_CONTENT, FrameLayoutWrap.LayoutParams.WRAP_CONTENT);
        lpBadge.gravity = Gravity.BOTTOM | Gravity.END;
        lpBadge.setMargins(0, 0, dp(16), dp(16));
        banner.addView(badge, lpBadge);

        content.addView(banner, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(200)));

        /* ---- title block ---- */
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(18), dp(18), dp(18), dp(6));

        TextView title = new TextView(this);
        title.setText(game.title == null ? "Başlıksız" : game.title);
        title.setTextColor(C_TITLE);
        title.setTextSize(25);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBox.addView(title);

        if (game.developer != null && !game.developer.isEmpty()) {
            TextView dev = new TextView(this);
            dev.setText(game.developer);
            dev.setTextColor(C_SUB);
            dev.setTextSize(14);
            titleBox.addView(dev);
        }

        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setPadding(0, dp(10), 0, 0);
        if (game.androidVer != null && !game.androidVer.isEmpty()) tagRow.addView(tag("Android " + game.androidVer));
        if (game.version != null && !game.version.isEmpty()) tagRow.addView(tag("v" + game.version));
        if (game.size > 0) tagRow.addView(tag(fmtBytes(game.size)));
        titleBox.addView(tagRow);

        content.addView(titleBox);

        /* ---- site navigation button ---- */
        TextView siteBtn = new TextView(this);
        siteBtn.setText("Bu Sayfaya Git");
        siteBtn.setTextColor(Color.WHITE);
        siteBtn.setTextSize(17);
        siteBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        siteBtn.setGravity(Gravity.CENTER);
        GradientDrawable btnBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR, new int[]{C_BTN1, C_BTN2});
        btnBg.setCornerRadius(dp(16));
        siteBtn.setBackground(btnBg);
        siteBtn.setPadding(dp(16), dp(15), dp(16), dp(15));
        siteBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String siteUrl = SITE_BASE + game.id;
                try {
                    Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl));
                    startActivity(browser);
                } catch (Exception e) {
                    Toast.makeText(DetailActivity.this, "Tarayıcı açılamadı.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.setMargins(dp(18), dp(12), dp(18), dp(4));
        content.addView(siteBtn, lpBtn);

        /* ---- description ---- */
        if (game.description != null && !game.description.isEmpty()) {
            content.addView(sectionTitle("Hakkında"));
            TextView desc = new TextView(this);
            desc.setText(game.description);
            desc.setTextColor(C_SUB);
            desc.setTextSize(15);
            desc.setLineSpacing(5, 1);
            desc.setPadding(dp(18), dp(4), dp(18), dp(6));
            content.addView(desc);
        }

        /* ---- screenshots ---- */
        if (loadShot(0) != null) {
            content.addView(sectionTitle("Ekran Görüntüleri"));
            HorizontalScrollView shotScroll = new HorizontalScrollView(this);
            shotScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout shotRow = new LinearLayout(this);
            shotRow.setOrientation(LinearLayout.HORIZONTAL);
            shotRow.setPadding(dp(18), dp(4), dp(18), dp(10));
            for (int i = 0; i < 4; i++) {
                Bitmap s = loadShot(i);
                if (s == null) continue;
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(s);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                roundImage(iv);
                LinearLayout.LayoutParams lpShot = new LinearLayout.LayoutParams(dp(190), dp(110));
                lpShot.rightMargin = dp(8);
                shotRow.addView(iv, lpShot);
            }
            shotScroll.addView(shotRow);
            content.addView(shotScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        /* ---- APK info ---- */
        content.addView(sectionTitle("APK Bilgileri"));
        LinearLayout infoCard = card();
        infoCard.addView(infoRow("Sürüm", game.version));
        infoCard.addView(infoRow("Boyut", fmtBytes(game.size)));
        infoCard.addView(infoRow("Android", empty54(game.androidVer)));
        infoCard.addView(infoRow("Mimari", empty54(game.arch)));
        infoCard.addView(infoRow("Paket", empty54(game.packageName)));
        if (game.sha256 != null && !game.sha256.isEmpty())
            infoCard.addView(infoRow("SHA-256", trunc(game.sha256, 24) + "…"));
        content.addView(infoCard);

        root.addView(scroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private TextView tag(String txt) {
        TextView t = new TextView(this);
        t.setText(txt);
        t.setTextColor(C_ACCENT);
        t.setTextSize(12);
        t.setBackground(roundRect(0xFF11273F, 14));
        t.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView sectionTitle(String txt) {
        TextView t = new TextView(this);
        t.setText(txt);
        t.setTextColor(C_TITLE);
        t.setTextSize(18);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(dp(18), dp(20), dp(18), dp(4));
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(roundRect(C_CARD, 16));
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(18), dp(4), dp(18), dp(8));
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout infoRow(String k, String v) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView kk = new TextView(this);
        kk.setText(k);
        kk.setTextColor(C_MUTED);
        kk.setTextSize(14);
        kk.setMaxLines(1);
        row.addView(kk, new LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView vv = new TextView(this);
        vv.setText(v == null || v.isEmpty() ? "—" : v);
        vv.setTextColor(C_TITLE);
        vv.setTextSize(14);
        vv.setMaxLines(2);
        row.addView(vv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private void roundImage(final ImageView iv) {
        if (Build.VERSION.SDK_INT >= 21) {
            iv.setClipToOutline(true);
            iv.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(14));
                }
            });
        }
    }

    private static class FrameLayoutWrap extends android.widget.FrameLayout {
        public FrameLayoutWrap(android.content.Context c) { super(c); }
    }

    private Bitmap loadCover() {
        String name = slugFor(game.title);
        if (name == null) return null;
        try {
            InputStream is = getAssets().open("offline/covers/" + name + ".png");
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 1;
            return BitmapFactory.decodeStream(is, null, o);
        } catch (Exception e) { return null; }
    }

    private Bitmap loadShot(int i) {
        String name = slugFor(game.title);
        if (name == null) return null;
        try {
            InputStream is = getAssets().open("offline/covers/" + name + "-shot" + (i + 1) + ".png");
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 1;
            return BitmapFactory.decodeStream(is, null, o);
        } catch (Exception e) { return null; }
    }

    private String slugFor(String title) {
        if (title == null) return null;
        String t = title.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
        if ("amongus".equals(t)) return "among-us";
        if ("minecraft".equals(t)) return "minecraft";
        if ("pubgmobile".equals(t)) return "pubg-mobile";
        if ("subwaysurfers".equals(t)) return "subway-surfers";
        if ("candycrushsaga".equals(t)) return "candy-crush";
        if ("callofdutymobile".equals(t)) return "cod-mobile";
        if ("fortnite".equals(t)) return "fortnite";
        if ("genshinimpact".equals(t)) return "genshin";
        if ("clashofclans".equals(t)) return "clash-of-clans";
        if ("brawlstars".equals(t)) return "brawl-stars";
        if ("roblox".equals(t)) return "roblox";
        if ("pokemongo".equals(t)) return "pokemon-go";
        if ("asphalt9".equals(t)) return "asphalt-9";
        if ("8ballpool".equals(t)) return "8ball-pool";
        if ("templerun".equals(t)) return "temple-run";
        return null;
    }

    private String fmtBytes(long n) {
        if (n <= 0) return "";
        double d = n;
        String[] u = {"B", "KB", "MB", "GB"};
        int i = 0;
        while (d >= 1024 && i < u.length - 1) { d /= 1024; i++; }
        return String.format("%.0f %s", d, u[i]);
    }

    private String empty54(String s) { return s == null ? "" : s; }

    private String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}