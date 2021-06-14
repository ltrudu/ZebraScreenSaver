package com.zebra.screensaver;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

public class PlasmaLicenceActivity extends AppCompatActivity {

    WebView mWebView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence);

        mWebView = (WebView)findViewById(R.id.wvLicence);
        mWebView.loadUrl("file:///android_asset/plasmalicence.html");
    }
}
