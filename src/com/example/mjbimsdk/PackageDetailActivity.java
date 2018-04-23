package com.example.mjbimsdk;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PackageDetailActivity extends Activity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_detail);

        String url = getIntent().getStringExtra("url");
        mWebView = (WebView)findViewById(R.id.demo_web);
        findViewById(R.id.demo_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        WebSettings mSet = mWebView.getSettings();
        mSet.setJavaScriptEnabled(true);
        mSet.setAllowFileAccess(true);
        mSet.setAppCacheEnabled(true);
        //mSet.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mSet.setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                //重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边
                view.loadUrl(url);
                return true;
            }
        });

        mWebView.loadUrl(url);
    }

}
