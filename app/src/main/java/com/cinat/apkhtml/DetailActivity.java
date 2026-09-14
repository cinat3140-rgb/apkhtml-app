package com.cinat.apkhtml;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;

public class DetailActivity extends Activity {

    private MainActivity.Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        } catch (Exception ignored) {}

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(241, 245, 249));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(0), 0, dp(0), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        /* ---- cover banner ---- */
        ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackgroundColor(Color.rgb(226, 232, 240));
        Bitmap bm = loadCover();
        if (bm != null) cover.setImageBitmap(bm);
        content.addView(cover, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(190)));

        /* ---- title block ---- */
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(18), dp(18), dp(18), dp(6));

        TextView title = new TextView(this);
        title.setText(game.title == null ? "Başlıksız" : game.title);
        title.setTextColor(Color.rgb(15, 23, 42));
        title.setTextSize(25);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBox.addView(title);

        if (game.developer != null && !game.developer.isEmpty()) {
            TextView dev = new TextView(this);
            dev.setText(game.developer);
            dev.setTextColor(Color.rgb(100, 116, 139));
            dev.setTextSize(14);
            titleBox.addView(dev);
        }

        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setPadding(0, dp(8), 0, 0);
        if (game.genre != null && !game.genre.isEmpty()) tagRow.addView(tag(game.genre));
        if (game.androidVer != null && !game.androidVer.isEmpty()) tagRow.addView(tag("Android " + game.androidVer));
        if (game.version != null && !game.version.isEmpty()) tagRow.addView(tag("v" + game.version));
        titleBox.addView(tagRow);

        content.addView(titleBox);

        /* ---- download button ---- */
        TextView dlBtn = new TextView(this);
        dlBtn.setText("⬇ APK İndir");
        dlBtn.setTextColor(Color.WHITE);
        dlBtn.setTextSize(17);
        dlBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        dlBtn.setGravity(Gravity.CENTER);
        dlBtn.setBackgroundColor(Color.rgb(37, 99, 235));
        dlBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        dlBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                MainActivity.downloadGame(DetailActivity.this, game);
            }
        });
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.setMargins(dp(18), dp(8), dp(18), dp(4));
        content.addView(dlBtn, lpBtn);

        /* ---- description ---- */
        if (game.description != null && !game.description.isEmpty()) {
            content.addView(sectionTitle("Hakkında"));
            TextView desc = new TextView(this);
            desc.setText(game.description);
            desc.setTextColor(Color.rgb(71, 85, 105));
            desc.setTextSize(15);
            desc.setLineSpacing(4, 1);
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
                iv.setPadding(dp(3), 0, dp(3), 0);
                shotRow.addView(iv, new LinearLayout.LayoutParams(dp(190), dp(107)));
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
        t.setTextColor(Color.rgb(37, 99, 235));
        t.setTextSize(12);
        t.setBackgroundColor(Color.rgb(238, 242, 255));
        t.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView sectionTitle(String txt) {
        TextView t = new TextView(this);
        t.setText(txt);
        t.setTextColor(Color.rgb(30, 41, 59));
        t.setTextSize(17);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(dp(18), dp(16), dp(18), dp(4));
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(Color.WHITE);
        c.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(18), dp(4), dp(18), dp(8));
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout infoRow(String k, String v) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        TextView kk = new TextView(this);
        kk.setText(k);
        kk.setTextColor(Color.rgb(100, 116, 139));
        kk.setTextSize(14);
        kk.setMaxLines(1);
        row.addView(kk, new LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView vv = new TextView(this);
        vv.setText(v == null || v.isEmpty() ? "—" : v);
        vv.setTextColor(Color.rgb(15, 23, 42));
        vv.setTextSize(14);
        vv.setMaxLines(2);
        row.addView(vv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
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
        if ("8ballpool".equals(t) || "8ballpool".equals(title.toLowerCase().replace(" ", ""))) return "8ball-pool";
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