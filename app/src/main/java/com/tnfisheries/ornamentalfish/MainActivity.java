package com.tnfisheries.ornamentalfish;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    static final String ASSET_HOME = "file:///android_asset/index.html";

    private WebView webView;
    private BottomNavigationView bottomNav;
    private WebAppInterface jsBridge;
    private boolean syncingNavFromJs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_OrnamentalFish);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        webView = findViewById(R.id.webview);
        bottomNav = findViewById(R.id.bottom_nav);
        setupWebView();
        setupBottomNav();
        setupBackHandler();

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
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        jsBridge = new WebAppInterface(this);
        webView.addJavascriptInterface(jsBridge, "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                                                    @NonNull WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Deprecated
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }
        });
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

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            if (syncingNavFromJs) return true;
            int id = item.getItemId();
            String tab = "home";
            if (id == R.id.nav_atlas) tab = "atlas";
            else if (id == R.id.nav_learn) tab = "book";
            else if (id == R.id.nav_quiz) tab = "quiz";
            else if (id == R.id.nav_tools) tab = "tools";
            final String jsTab = tab;
            webView.post(() -> webView.evaluateJavascript(
                    "window.__setNativeTab && window.__setNativeTab('" + jsTab + "')",
                    null));
            return true;
        });
    }

    public void selectTabFromJs(String tab) {
        final int id;
        switch (tab == null ? "home" : tab) {
            case "atlas":
                id = R.id.nav_atlas;
                break;
            case "book":
                id = R.id.nav_learn;
                break;
            case "quiz":
                id = R.id.nav_quiz;
                break;
            case "tools":
                id = R.id.nav_tools;
                break;
            default:
                id = R.id.nav_home;
                break;
        }
        runOnUiThread(() -> {
            syncingNavFromJs = true;
            try {
                bottomNav.setSelectedItemId(id);
            } finally {
                syncingNavFromJs = false;
            }
        });
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.evaluateJavascript(
                        "(function(){try{return !!(window.__appBack && window.__appBack());}"
                                + "catch(e){return false;}})()",
                        value -> {
                            boolean handled = "true".equalsIgnoreCase(String.valueOf(value));
                            if (handled) return;
                            if (webView.canGoBack()) {
                                webView.goBack();
                            } else {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                                setEnabled(true);
                            }
                        });
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (jsBridge != null) jsBridge.shutdown();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
