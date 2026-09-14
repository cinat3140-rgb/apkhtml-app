package com.cinat.apkhtml;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://cinat3140-rgb.github.io/apkhtml/";
    private static final String SITE_HOST = "cinat3140-rgb.github.io";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView statusBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout root = new RelativeLayout(this);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB));
        progressBar.setVisibility(View.GONE);
        RelativeLayout.LayoutParams lpProgress = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, 6);
        root.addView(progressBar, lpProgress);

        webView = new WebView(this);
        RelativeLayout.LayoutParams lpWeb = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        lpWeb.addRule(RelativeLayout.BELOW, progressBar.getId() == -1 ? progressBar.hashCode() : progressBar.getId());
        root.addView(webView, lpWeb);

        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (failingUrl != null && failingUrl.startsWith(HOME_URL)) {
                    showOfflineScreen();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
        });

        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    if (newProgress < 100) progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype);
                    String cookie = CookieManager.getInstance().getCookie(url);
                    if (cookie != null) request.addRequestHeader("Cookie", cookie);
                    String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setTitle(fileName);
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(MainActivity.this, "İndirme başladı: " + fileName, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    openExternal(url);
                }
            }
        });

        if (!isNetworkAvailable()) {
            showOfflineScreen();
        } else {
            loadHome();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "↻ Yenile");
        menu.add(0, 2, 0, "ℹ️ Hakkında & Güvenlik");
        menu.add(0, 4, 0, "🔒 Güvenlik Rehberi");
        menu.add(0, 3, 0, "🌐 Tarayıcıda Aç");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                webView.reload();
                return true;
            case 2:
                showAboutDialog();
                return true;
            case 4:
                webView.stopLoading();
                webView.loadUrl("file:///android_asset/security.html");
                return true;
            case 3:
                openExternal(webView.getUrl() != null ? webView.getUrl() : HOME_URL);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutDialog() {
        String fp = "";
        try {
            PackageInfo pkg = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (Signature s : pkg.signingInfo.getApkContentsSigners()) {
                    fp = sha256Hex(s.toByteArray());
                    break;
                }
            } else {
                for (Signature s : getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures) {
                    fp = sha256Hex(s.toByteArray());
                    break;
                }
            }
        } catch (Exception ignored) {
            fp = "—";
        }
        String version = "1.1.0";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        String apksha = System.getProperty("apkhtml.sha256", "");
        String message =
            "ApkHTML v" + version + "\n\n" +
            "Katalog, arama ve incelemeyi güvenli WebView üzerinden sunar. " +
            "Dosyalar Android resmi DownloadManager ile indirilir.\n\n" +
            "📦 APK Boyutu: ~" + (isApkSize() / 1024) + " KB\n" +
            "🔐 Sertifika SHA-256:\n" + fp + "\n\n" +
            "Bu imza, APK'yı yayınlayan geliştiriciyi doğrular. " +
            "İndirdiğin dosyanın sertifikası burada görünenle eşleşmiyorsa dosya değiştirilmiştir, kurma!";
        new AlertDialog.Builder(this)
            .setTitle("Hakkında & Güvenlik")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show();
    }

    private long isApkSize() {
        try {
            return new java.io.File(getApplicationInfo().sourceDir).length();
        } catch (Exception e) {
            return 0;
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "—";
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showOfflineScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String html = "<!doctype html><html><head><meta charset=\"utf-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<style>body{font-family:system-ui,sans-serif;display:flex;align-items:center;justify-content:center;" +
                    "min-height:80vh;background:#f1f5f9;color:#0f172a;margin:0;padding:24px;text-align:center}" +
                    ".box{max-width:340px;background:#fff;border-radius:18px;padding:30px;box-shadow:0 10px 30px rgba(0,0,0,.08)}" +
                    ".icon{font-size:48px;margin-bottom:10px}h1{font-size:20px;margin:0 0 8px}p{color:#64748b;font-size:14px;margin:0 0 18px}" +
                    "button{background:#2563eb;color:#fff;border:0;border-radius:12px;padding:12px 20px;font-size:15px;font-weight:600}" +
                    "</style></head><body><div class=\"box\"><div class=\"icon\">📡</div>" +
                    "<h1>İnternet bağlantısı yok</h1>" +
                    "<p>Kataloğu görüntülemek için ağ bağlantın gerekli. Bağlantıyı kur ve tekrar dene.</p>" +
                    "<button onclick=\"location.reload()\">↻ Tekrar Dene</button></div></body></html>";
                webView.loadData(html, "text/html; charset=utf-8", null);
            }
        });
    }

    private android.view.View root(WebView w, ProgressBar p) {
        return w;
    }

    private boolean handleUrl(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host != null && (host.equals(SITE_HOST) || host.endsWith("." + SITE_HOST))) {
            return false;
        }
        openExternal(url);
        return true;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            // no browser available
        }
    }

    private void loadHome() {
        webView.loadUrl(HOME_URL);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}