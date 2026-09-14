package com.cinat.apkhtml;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String CATALOG_URL = "https://cinat3140-rgb.github.io/apkhtml/catalog.json";

    private List<Game> games = new ArrayList<>();
    private List<Game> filtered = new ArrayList<>();

    private LinearLayout root;
    private EditText searchInput;
    private TextView statusLabel;
    private ListView listView;
    private ProgressBar progressBar;
    private android.widget.HorizontalScrollView chipScroll;
    private int activeCategory = -1;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    public static class Game {
        public int id;
        public String title;
        public String description;
        public String developer;
        public String genre;
        public int categoryId;
        public int popularity;
        public boolean featured;
        public String coverUrl;
        public String version;
        public long size;
        public String androidVer;
        public String arch;
        public String packageName;
        public String url;
        public String sha256;
        public List<String> permissions = new ArrayList<>();
        public List<String> screenshots = new ArrayList<>();

        public String toJson() {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id);
                o.put("title", title == null ? "" : title);
                o.put("description", description == null ? "" : description);
                o.put("developer", developer == null ? "" : developer);
                o.put("genre", genre == null ? "" : genre);
                o.put("categoryId", categoryId);
                o.put("popularity", popularity);
                o.put("featured", featured);
                o.put("version", version == null ? "" : version);
                o.put("size", size);
                o.put("androidVer", androidVer == null ? "" : androidVer);
                o.put("arch", arch == null ? "" : arch);
                o.put("packageName", packageName == null ? "" : packageName);
                o.put("url", url == null ? "" : url);
                o.put("sha256", sha256 == null ? "" : sha256);
                JSONArray p = new JSONArray();
                for (String s : permissions) p.put(s);
                o.put("permissions", p);
                JSONArray sc = new JSONArray();
                for (String s : screenshots) sc.put(s);
                o.put("screenshots", sc);
                o.put("coverUrl", coverUrl == null ? "" : coverUrl);
                return o.toString();
            } catch (Exception e) {
                return "{}";
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F5F9);

        /* ---- header ---- */
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(8), dp(10));
        header.setBackgroundColor(0xFF2563EB);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.splash_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(44), dp(44));
        header.addView(logo, lpLogo);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("ApkHTML");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBox.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Bağımsız APK Kütüphanesi");
        sub.setTextColor(0xDDFFFFFF);
        sub.setTextSize(12);
        titleBox.addView(sub);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("↻");
        refreshBtn.setTextColor(0xFFFFFFFF);
        refreshBtn.setTextSize(26);
        refreshBtn.setGravity(android.view.Gravity.CENTER);
        refreshBtn.setPadding(dp(14), dp(4), dp(14), dp(4));
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnline()) silentSync();
                else Toast.makeText(MainActivity.this, "İnternet yok, gömülü katalog gösteriliyor.", Toast.LENGTH_SHORT).show();
            }
        });
        header.addView(refreshBtn);

        TextView moreBtn = new TextView(this);
        moreBtn.setText("⋯");
        moreBtn.setTextColor(0xFFFFFFFF);
        moreBtn.setTextSize(26);
        moreBtn.setGravity(android.view.Gravity.CENTER);
        moreBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });
        header.addView(moreBtn);

        root.addView(header, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        /* ---- search row ---- */
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setPadding(dp(12), dp(10), dp(12), 0);

        searchInput = new EditText(this);
        searchInput.setHint("Oyun ara… (ör. Among Us)");
        searchInput.setSingleLine(true);
        searchInput.setTextSize(15);
        searchInput.setBackgroundResource(android.R.drawable.editbox_background);
        searchInput.setHintTextColor(0xFF94A3B8);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { applyFilters(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, dp(46), 1));

        TextView clearBtn = new TextView(this);
        clearBtn.setText("✕");
        clearBtn.setTextColor(0xFF64748B);
        clearBtn.setTextSize(18);
        clearBtn.setGravity(android.view.Gravity.CENTER);
        clearBtn.setPadding(dp(12), 0, dp(12), 0);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { searchInput.setText(""); }
        });
        searchRow.addView(clearBtn);

        root.addView(searchRow, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        /* ---- category chips ---- */
        final String[] cats = {"Tümü", "Action", "Adventure", "RPG", "Racing", "Simulation",
            "Strategy", "Horror", "FPS", "Sports", "Indie", "Open World"};
        chipScroll = new HorizontalScrollViewWrap(this);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(dp(12), dp(10), dp(12), 0);
        for (int i = 0; i < cats.length; i++) {
            final int idx = i - 1;
            final TextView chip = new TextView(this);
            chip.setText(cats[i]);
            chip.setTextSize(13);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setBackgroundResource(android.R.drawable.editbox_background);
            chip.setTag(Integer.valueOf(idx));
            chip.setTextColor(activeCategory == idx ? 0xFF2563EB : 0xFF334155);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    activeCategory = idx;
                    applyFilters();
                }
            });
            chipRow.addView(chip);
        }
        chipScroll.addView(chipRow, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chipScroll.setHorizontalScrollBarEnabled(false);
        root.addView(chipScroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        /* ---- status row ---- */
        statusLabel = new TextView(this);
        statusLabel.setTextColor(0xFF64748B);
        statusLabel.setTextSize(12);
        statusLabel.setPadding(dp(16), dp(8), dp(16), 0);
        root.addView(statusLabel, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        /* ---- list ---- */
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setPadding(0, dp(6), 0, dp(12));
        listView.setBackgroundColor(0xFFF1F5F9);
        listView.setAdapter(new GameAdapter());

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB));
        progressBar.setVisibility(View.GONE);

        LinearLayout listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        listWrap.addView(progressBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(4)));
        listWrap.addView(listView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(listWrap, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);

        loadCatalog();
    }

    /* ---------- data loading ---------- */

    private void loadCatalog() {
        statusLabel.setText("Katalog yükleniyor…");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<Game> embedded = readEmbedded();
                final List<Game> online = isOnline() ? fetchOnline() : null;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        merge(embedded, online);
                    }
                });
            }
        });
    }

    private void silentSync() {
        final List<Game> embedded = readEmbedded();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<Game> online = isOnline() ? fetchOnline() : null;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        merge(embedded, online);
                        statusLabel.setText("Güncellendi ✓");
                    }
                });
            }
        });
    }

    private void merge(List<Game> embedded, List<Game> online) {
        games = new ArrayList<>();
        if (online != null) {
            for (Game g : online) {
                if (g.url != null && !g.url.isEmpty() && g.url.startsWith("http")) games.add(g);
            }
        }
        for (Game g : embedded) {
            boolean dup = false;
            for (Game e : games) {
                if (e.id == g.id || e.url != null && g.url != null && e.url.equals(g.url)) { dup = true; break; }
            }
            if (!dup) games.add(g);
        }
        Collections.sort(games, new Comparator<Game>() {
            @Override public int compare(Game a, Game b) { return b.popularity - a.popularity; }
        });
        applyFilters();
    }

    private void applyFilters() {
        String q = (searchInput != null ? searchInput.getText() : "").toString().trim().toLowerCase();
        filtered = new ArrayList<>();
        for (Game g : games) {
            if (activeCategory > 0 && g.categoryId != activeCategory) continue;
            if (!q.isEmpty()) {
                boolean hit = (g.title != null && g.title.toLowerCase().contains(q)) ||
                    (g.developer != null && g.developer.toLowerCase().contains(q)) ||
                    (g.genre != null && g.genre.toLowerCase().contains(q)) ||
                    (g.description != null && g.description.toLowerCase().contains(q));
                if (!hit) continue;
            }
            filtered.add(g);
        }
        if (statusLabel != null) statusLabel.setText(filtered.size() + " / " + games.size() + " oyun gösteriliyor");
        syncChips();
        listView.invalidateViews();
    }

    private void syncChips() {
        if (chipScroll == null || !(chipScroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) chipScroll.getChildAt(0);
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            if (!(c instanceof TextView)) continue;
            Object tag = c.getTag();
            int idx = (tag instanceof Integer) ? (Integer) tag : -1;
            ((TextView) c).setTextColor(activeCategory == idx ? 0xFF2563EB : 0xFF334155);
        }
    }

    /* ---------- persistence / network ---------- */

    private List<Game> readEmbedded() {
        List<Game> list = new ArrayList<>();
        try {
            InputStream is = getAssets().open("catalog.json");
            String json = readAll(is);
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("games");
            if (arr != null) for (int i = 0; i < arr.length(); i++) {
                Game g = parseGame(arr.getJSONObject(i));
                if (g != null) list.add(g);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<Game> fetchOnline() {
        List<Game> list = new ArrayList<>();
        HttpURLConnection c = null;
        try {
            URL u = new URL(CATALOG_URL);
            c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "ApkHTML/2.0");
            if (c.getResponseCode() == 200) {
                String json = readAll(c.getInputStream());
                JSONObject obj = new JSONObject(json);
                JSONArray arr = obj.optJSONArray("games");
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    Game g = parseGame(arr.getJSONObject(i));
                    if (g != null) list.add(g);
                }
            }
        } catch (Exception e) {
            // offline or broken
        } finally {
            if (c != null) c.disconnect();
        }
        return list;
    }

    private Game parseGame(JSONObject j) {
        try {
            if (!j.has("title")) return null;
            Game g = new Game();
            g.id = j.optInt("id");
            g.title = j.optString("title");
            g.description = j.optString("description");
            g.developer = j.optString("developer");
            g.genre = j.optString("genre");
            g.categoryId = j.optInt("categoryId", -1);
            g.popularity = j.optInt("popularity", 50);
            g.featured = j.optBoolean("isFeatured", false);

            JSONObject apk = j.optJSONObject("apk");
            if (apk != null) {
                g.url = apk.optString("url");
                g.size = apk.optLong("fileSize");
                g.version = apk.optString("version");
                g.androidVer = apk.optString("androidVersion");
                g.arch = apk.optString("arch");
                g.packageName = apk.optString("packageName");
                g.sha256 = apk.optString("sha256");
                JSONArray perms = apk.optJSONArray("permissions");
                if (perms != null) for (int i = 0; i < perms.length(); i++) g.permissions.add(perms.optString(i));
            }
            if (g.url == null || g.url.isEmpty()) {
                JSONArray files = j.optJSONArray("latestFiles");
                if (files != null && files.length() > 0) {
                    JSONObject f = files.getJSONObject(0);
                    g.url = f.optString("downloadUrl");
                    g.size = f.optLong("fileSize", g.size);
                }
            }
            if (g.version == null || g.version.isEmpty()) {
                JSONObject lv = j.optJSONObject("latestVersion");
                if (lv != null) g.version = lv.optString("version");
            }
            g.coverUrl = j.optString("coverUrl");
            JSONArray shots = j.optJSONArray("screenshots");
            if (shots != null) for (int i = 0; i < shots.length(); i++) g.screenshots.add(shots.optString(i));
            return g;
        } catch (Exception e) {
            return null;
        }
    }

    private String readAll(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append("\n");
        r.close();
        return sb.toString();
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                return ni != null && ni.isConnected();
            }
        } catch (Exception ignored) {}
        return false;
    }

    /* ---------- UI helpers ---------- */

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static class HorizontalScrollViewWrap extends android.widget.HorizontalScrollView {
        public HorizontalScrollViewWrap(Context c) { super(c); }
    }

    private class GameAdapter extends BaseAdapter {
        @Override public int getCount() { return filtered.size(); }
        @Override public Object getItem(int p) { return filtered.get(p); }
        @Override public long getItemId(int p) { return filtered.get(p).id; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            final Game g = filtered.get(pos);

            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(6), dp(12), dp(6));

            int h = (int) Math.min(dp(140), row_dp());
            int w = (int) (h * 1.78f);

            ImageView cover = new ImageView(MainActivity.this);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm = loadCover(g);
            if (bm != null) cover.setImageBitmap(bm);
            else cover.setBackgroundColor(0xFFE2E8F0);
            LinearLayout.LayoutParams lpCover = new LinearLayout.LayoutParams(w, h);
            row.addView(cover, lpCover);

            LinearLayout info = new LinearLayout(MainActivity.this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, dp(6), 0);
            info.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView t = new TextView(MainActivity.this);
            t.setText(g.title);
            t.setTextColor(0xFF0F172A);
            t.setTextSize(16);
            t.setTypeface(null, android.graphics.Typeface.BOLD);
            t.setMaxLines(1);
            info.addView(t);

            String meta = "";
            if (g.version != null && !g.version.isEmpty()) meta += "v" + g.version;
            if (g.size > 0) meta += (meta.isEmpty() ? "" : " · ") + fmtBytes(g.size);
            if (meta.isEmpty() && g.genre != null) meta = g.genre;
            TextView m = new TextView(MainActivity.this);
            m.setText(meta);
            m.setTextColor(0xFF64748B);
            m.setTextSize(13);
            m.setMaxLines(1);
            info.addView(m);

            if (g.genre != null && !g.genre.isEmpty()) {
                TextView gen = new TextView(MainActivity.this);
                gen.setText(g.genre);
                gen.setTextColor(0xFF2563EB);
                gen.setTextSize(12);
                gen.setMaxLines(1);
                info.addView(gen);
            }
            row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView arrow = new TextView(MainActivity.this);
            arrow.setText("›");
            arrow.setTextColor(0xFF94A3B8);
            arrow.setTextSize(28);
            arrow.setPadding(dp(6), 0, dp(4), 0);
            row.addView(arrow);

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openDetail(g); }
            });

            return row;
        }
    }

    private int row_dp() {
        return Math.min(dp(110), getResources().getDisplayMetrics().widthPixels / 3);
    }

    private Bitmap loadCover(final Game g) {
        try {
            String name = slugFor(g.title);
            if (name == null) return null;
            InputStream is = getAssets().open("offline/covers/" + name + ".png");
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            return BitmapFactory.decodeStream(is, null, o);
        } catch (Exception e) {
            return null;
        }
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
        if ("pokemongo".equals(t) || "pokemon go".equals(title.toLowerCase())) return "pokemon-go";
        if ("asphalt9".equals(t)) return "asphalt-9";
        if ("8ballpool".equals(t) || "8ballpool".equals(title.toLowerCase().replace(" ", ""))) return "8ball-pool";
        if ("templerun".equals(t)) return "temple-run";
        return null;
    }

    private void openDetail(Game g) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra("game_json", g.toJson());
        startActivity(i);
    }

    /* ---------- misc ---------- */

    private String fmtBytes(long n) {
        if (n <= 0) return "";
        double d = n;
        String[] u = {"B", "KB", "MB", "GB"};
        int i = 0;
        while (d >= 1024 && i < u.length - 1) { d /= 1024; i++; }
        return String.format("%.0f %s", d, u[i]);
    }

    private void showAboutDialog() {
        String fp = "";
        try {
            PackageInfo pkg = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (Signature s : pkg.signingInfo.getApkContentsSigners()) { fp = sha256Hex(s.toByteArray()); break; }
            } else {
                for (Signature s : getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures) { fp = sha256Hex(s.toByteArray()); break; }
            }
        } catch (Exception ignored) { fp = "—"; }
        String version = "2.0.0";
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) {}
        new AlertDialog.Builder(this)
            .setTitle("ApkHTML v" + version)
            .setMessage(
                "Bağımsız APK kütüphanesi. Siteye bağlı değildir; katalog uygulama içinde gömülüdür.\n\n" +
                "Çevrimdışı katalog: 15 oyun + kapak/ekran görüntüleri.\n" +
                "Otomatik güncelleme: İnternetteyken sitedeki catalog.json ile senkronize olur, yeni oyunlar otomatik eklenir.\n\n" +
                "🔐 Sertifika SHA-256:\n" + fp + "\n\n" +
                "İmza yayınlananla eşleşmezse dosya değiştirilmiştir, kurma!")
            .setPositiveButton("Tamam", null)
            .setNegativeButton("🔒 Güvenlik", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int w) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("İzinler")
                        .setMessage("Yalnızca: \n• İnternet (güncelleme için)\n• İndirme\n\nKamera, mikrofon, konum, SMS izni YOKTUR.")
                        .setPositiveButton("Tamam", null)
                        .show();
                }
            })
            .show();
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "—"; }
    }

    private void download(Game g) {
        if (g.url == null || g.url.isEmpty()) {
            Toast.makeText(this, "Bu oyun için bağlantı yok.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(g.url));
            r.setMimeType("application/vnd.android.package-archive");
            String fn = g.title.replaceAll("[^A-Za-z0-9._-]", "_") + ".apk";
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setTitle(g.title);
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(r);
            Toast.makeText(this, "İndirme başladı: " + fn, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(g.url))); } catch (Exception ex) {}
        }
    }

    static void downloadGame(Context ctx, Game g) {
        if (g.url == null || g.url.isEmpty()) {
            Toast.makeText(ctx, "Bu oyun için bağlantı yok.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(g.url));
            r.setMimeType("application/vnd.android.package-archive");
            String fn = g.title.replaceAll("[^A-Za-z0-9._-]", "_") + ".apk";
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setTitle(g.title);
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(r);
            Toast.makeText(ctx, "İndirme başladı: " + fn, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try { ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(g.url))); } catch (Exception ex) {}
        }
    }
}