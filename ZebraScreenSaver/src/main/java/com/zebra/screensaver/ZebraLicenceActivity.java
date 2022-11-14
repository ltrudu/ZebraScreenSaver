package com.zebra.screensaver;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class ZebraLicenceActivity extends AppCompatActivity {

    WebView mWebView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence);

        mWebView = (WebView)findViewById(R.id.wvLicence);
        mWebView.loadUrl("file:///android_asset/zebralicence.html");
    }
}
