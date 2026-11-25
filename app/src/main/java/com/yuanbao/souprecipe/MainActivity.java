package com.yuanbao.souprecipe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 单页汤谱 App 外壳。
 *
 * 1. 离线：直接载入 assets/www/index.html，全部汤谱内嵌，飞机模式都用到。
 * 2. 联网更新：JS 透过 Android.fetchUrl() 叫原生发 HTTPS GET 去公网更新源，
 *    用原生连线而唔系 WebView fetch，所以个源唔使开 CORS。
 * 3. Android.hasNetwork() 畀 JS 判断有冇有效网络，飞机模式就唔白等 timeout。
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage：收藏、更新网址、离线缓存全靠佢
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        web.addJavascriptInterface(new Bridge(), "Android");

        // 原文链接用系统浏览器开，唔好喺 WebView 入面导航走（assets 相对路径会坏）
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                        // 冇浏览器就静静算数，唔好 crash
                    }
                    return true;
                }
                return false;
            }
        });

        web.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class Bridge {

        /** 有冇有效网络（WiFi / 流动数据都得，但要已验证可上网） */
        @JavascriptInterface
        public boolean hasNetwork() {
            try {
                ConnectivityManager cm =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm == null) return false;
                Network n = cm.getActiveNetwork();
                if (n == null) return false;
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                return nc != null
                        && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } catch (Exception e) {
                return true;   // 拎唔到就照试，反正失败会静默 fallback
            }
        }

        /**
         * 原生 GET。结果异步回叫 window.__onFetchResult(cbId, ok, text)。
         * 用 JSONObject.quote 将结果包成合法 JS 字串，含换行／引号都唔会出事。
         */
        @JavascriptInterface
        public void fetchUrl(String url, final String cbId) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean ok = true;
                    String result;
                    try {
                        URL u = new URL(url);
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        c.setConnectTimeout(3500);
                        c.setReadTimeout(3500);
                        c.setRequestMethod("GET");
                        c.setRequestProperty("Accept", "application/json, text/plain, */*");
                        c.setRequestProperty("Cache-Control", "no-cache");
                        int code = c.getResponseCode();
                        if (code < 200 || code >= 300) {
                            throw new IOException("HTTP " + code);
                        }
                        InputStream in = c.getInputStream();
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            bo.write(buf, 0, n);
                            if (bo.size() > 2 * 1024 * 1024) {   // 上限 2MB，防傻仔档案
                                throw new IOException("份 JSON 大得滞");
                            }
                        }
                        in.close();
                        c.disconnect();
                        result = new String(bo.toByteArray(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        ok = false;
                        result = (e.getMessage() == null || e.getMessage().isEmpty())
                                ? "连唔到更新源" : e.getMessage();
                    }

                    final boolean fOk = ok;
                    final String fResult = result;
                    web.post(new Runnable() {
                        @Override
                        public void run() {
                            String script = "window.__onFetchResult("
                                    + JSONObject.quote(cbId) + ","
                                    + fOk + ","
                                    + JSONObject.quote(fResult) + ")";
                            web.evaluateJavascript(script, null);
                        }
                    });
                }
            }, "soup-fetch").start();
        }
    }
}
