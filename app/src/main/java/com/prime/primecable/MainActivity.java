package com.prime.primecable;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.UnityBanners;

public class MainActivity extends AppCompatActivity {
    private WebView myWebView;
    private String GameID = "3800031";
    private boolean testMode = true; //TODO
    private String bannerAdPlacement = "banner";
    private String interstitialAdPlacement = "video";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hideStatusBar();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        UnityAds.initialize(MainActivity.this,GameID,testMode);
        IUnityAdsListener unityAdsListener = new IUnityAdsListener() {
            @Override
            public void onUnityAdsReady(String s) {
                //Toast.makeText(MainActivity.this, "Interstitial Ad ready" , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsStart(String s) {
                //Toast.makeText(MainActivity.this, "Interstititl is playing", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
                //Toast.makeText(MainActivity.this, "Interstitial is Finished" , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {
                //Toast.makeText(MainActivity.this, unityAdsError.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        UnityAds.setListener(unityAdsListener);

        myWebView = findViewById(R.id.webView);
        myWebView.loadUrl("https://vidly.tv");
        myWebView.clearCache(true);
        myWebView.clearHistory();
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        myWebView.setWebViewClient(new MyWebViewClient());

        new CountDownTimer((long) 1e+255, (long) 1*60*1000){
            boolean firstTime = true;
            @Override
            public void onTick(long l) {
                if(firstTime){
                    firstTime = false;
                } else {
                    UnityAds.load(interstitialAdPlacement);
                    DisplayInterstitialAd();
                }
            }

            @Override
            public void onFinish() {
                UnityAds.load(interstitialAdPlacement);
                DisplayInterstitialAd();
            }
        }.start();
    }

    private void DisplayInterstitialAd (){
        if (UnityAds.isReady(interstitialAdPlacement)){
            UnityAds.show(MainActivity.this,interstitialAdPlacement);
        }
    }

    public void showAd(View view){

        //Toast.makeText(this, "Triggered Ad", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (myWebView.canGoBack()) {
                        myWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);
            performJsOperations(view);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            //performJsOperations(view);
        }
    }

    private void performJsOperations(WebView view){
        view.loadUrl("javascript:userProfile = {\"userId\":\"1598255786108\",\"msisdn\":\"923000000000\",\"packageId\":1,\"status\":\"SUBSCRIBED\",\"isTrialAvailed\":0,\"createdAt\":\"Sep 35, 2020 15:25:67 PM\",\"operatorId\":2,\"userPackage\":{\"id\":1,\"name\":\"Subscribed\",\"price\":0,\"description\":\"Subscribed\",\"intervalDays\":0,\"freeTrialDays\":0},\"likedContentIdsMap\":{}}");
        if(view.getUrl().contains("video")) {
            view.loadUrl("javascript:document.getElementById(\"pageHeader\").remove()");
            view.loadUrl("javascript:document.getElementById(\"header-wrap\").remove()");
        }
        else {
            //view.loadUrl("javascript:document.getElementsByClassName(\"logo\").innerHTML = '<a href=\"/home\"><img class=\"img-responsive\" src=\"https://i.ibb.co/FJh2FbW/primecable-200x200-1.png\" alt=\"\"></a></div>'");
            view.loadUrl("javascript:document.getElementsByClassName(\"logo\")[0].remove()");
            view.loadUrl("javascript:document.getElementsByClassName(\"wel-drop-top\")[0].remove()");
            view.loadUrl("javascript:document.getElementsByClassName(\"contactli\")[0].remove()");
        }
        view.loadUrl("javascript:document.getElementsByClassName(\"footer-section\")[0].remove()");
        view.loadUrl("javascript:document.getElementsByClassName(\"contactus-icon\")[0].remove()");
    }
}

