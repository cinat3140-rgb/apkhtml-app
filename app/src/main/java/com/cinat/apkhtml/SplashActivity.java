package com.cinat.apkhtml;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setBackgroundColor(0xFF2563EB);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.splash_logo);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(
            200, 200);
        root.addView(logo, lpLogo);

        TextView title = new TextView(this);
        title.setText("ApkHTML");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(30);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTitle.topMargin = 18;
        root.addView(title, lpTitle);

        TextView sub = new TextView(this);
        sub.setText("Güvenilir Android APK Kütüphanesi");
        sub.setTextColor(0xEEFFFFFF);
        sub.setTextSize(15);
        sub.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lpSub = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpSub.topMargin = 6;
        root.addView(sub, lpSub);

        TextView ver = new TextView(this);
        ver.setText("v1.1.0");
        ver.setTextColor(0xCCFFFFFF);
        ver.setTextSize(13);
        LinearLayout.LayoutParams lpVer = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpVer.topMargin = 46;
        root.addView(ver, lpVer);

        root.setPadding(0, 0, 0, 32);
        setContentView(root);

        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 1400);
    }
}