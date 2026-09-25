package com.tnfisheries.ornamentalfish;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toolbar;

/**
 * Framework-only shell. Native toolbar + bottom tabs satisfy Play
 * minimum-functionality without AppCompat/Material (those pulled
 * ProfileInstaller / DUMP and DebugProbesKt.bin into 2.5.x).
 */
public class MainActivity extends Activity {

    static final String ASSET_HOME = "file:///android_asset/index.html";
    static final int REQ_CREATE_DOC = 2601;
    static final int REQ_POST_NOTIF = 2602;

    private WebView webView;
    private WebAppInterface jsBridge;
    private boolean syncingNavFromJs;
    private TextView[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_OrnamentalFish);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        webView = findViewById(R.id.webview);
        setupTabs();
        setupWebView();

        if (savedInstanceState == null) {
            webView.loadUrl(ASSET_HOME);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        jsBridge = new WebAppInterface(this);
        webView.addJavascriptInterface(jsBridge, "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Deprecated
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }
        });
    }

    private void setupTabs() {
        tabs = new TextView[] {
            findViewById(R.id.nav_home),
            findViewById(R.id.nav_atlas),
            findViewById(R.id.nav_learn),
            findViewById(R.id.nav_quiz),
            findViewById(R.id.nav_tools)
        };
        String[] names = { "home", "atlas", "book", "quiz", "tools" };
        for (int i = 0; i < tabs.length; i++) {
            final String tab = names[i];
            tabs[i].setOnClickListener(v -> {
                if (syncingNavFromJs) return;
                paintTab(tab);
                webView.evaluateJavascript(
                    "window.__setNativeTab && window.__setNativeTab('" + tab + "')",
                    null);
            });
        }
        paintTab("home");
    }

    public void selectTabFromJs(String tab) {
        runOnUiThread(() -> {
            syncingNavFromJs = true;
            try {
                paintTab(tab);
            } finally {
                syncingNavFromJs = false;
            }
        });
    }

    private void paintTab(String tab) {
        String t = tab == null ? "home" : tab;
        int selected = R.id.nav_home;
        if ("atlas".equals(t)) selected = R.id.nav_atlas;
        else if ("book".equals(t)) selected = R.id.nav_learn;
        else if ("quiz".equals(t)) selected = R.id.nav_quiz;
        else if ("tools".equals(t)) selected = R.id.nav_tools;
        for (TextView tv : tabs) {
            boolean on = tv.getId() == selected;
            tv.setTextColor(getResources().getColor(on ? R.color.gold_soft : R.color.nav_idle));
        }
    }

    boolean handleUri(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        if ("file".equalsIgnoreCase(scheme)) return false;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException ignored) {
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript(
            "(function(){try{return !!(window.__appBack && window.__appBack());}"
                + "catch(e){return false;}})()",
            value -> {
                boolean handled = "true".equalsIgnoreCase(String.valueOf(value));
                if (handled) return;
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (jsBridge != null) {
            jsBridge.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (jsBridge != null) {
            jsBridge.onRequestPermissionsResult(requestCode, grantResults);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (jsBridge != null) jsBridge.shutdown();
        if (webView != null) {
            webView.removeJavascriptInterface("Android");
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
