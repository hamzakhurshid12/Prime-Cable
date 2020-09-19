package com.blackstar.primecable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private WebView myWebView;
    private String GameID = "3800031";
    private boolean testMode = false;
    private String bannerAdPlacement = "banner";
    private String interstitialAdPlacement = "video";
    private ReviewManager manager;
    private ProgressDialog progressDialog;

    private int adMinutes = 10;
    private boolean branding = true;
    private int reviewMinutes = 8;
    private boolean killSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = ReviewManagerFactory.create(this);
        //manager = new FakeReviewManager(this);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            myWebView.setWebContentsDebuggingEnabled(true);
        }
        startOperationsCheckDatabase();
    }

    private void showProgressDialog(){
        if(progressDialog == null)
            progressDialog = new ProgressDialog(MainActivity.this);
        if(!progressDialog.isShowing()) {
            progressDialog.setMessage("Please wait while we load resources!");
            progressDialog.show();
        }
    }

    private void startOperationsCheckDatabase(){
        showProgressDialog();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("configuration");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adMinutes = snapshot.child("adsMinutes").getValue(Integer.class);
                reviewMinutes = snapshot.child("reviewMinutes").getValue(Integer.class);
                branding = snapshot.child("branding").getValue(String.class).equals("true");
                killSwitch = snapshot.child("killSwitch").getValue(String.class).equals("true");
                String killMessage = snapshot.child("killMessage").getValue(String.class);

                if(killSwitch){
                    Toast.makeText(MainActivity.this, killMessage, Toast.LENGTH_LONG).show();
                    return;
                }

                myWebView.loadUrl("https://vidly.tv");
                myWebView.loadUrl("javascript:var myInterval=setInterval(function(){if(document.getElementsByClassName(\"logo\")[0]){void(document.getElementsByClassName(\"logo\")[0].getElementsByClassName(\"img-responsive\")[0].src = \"https://i.ibb.co/FJh2FbW/primecable-200x200-1.png\");clearInterval(myInterval);}},50);");
                //myWebView.clearCache(true);
                myWebView.clearHistory();
                myWebView.getSettings().setJavaScriptEnabled(true);
                myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                myWebView.setWebViewClient(new MyWebViewClient());

                startReviewTimer();
                startUnityAdTimer();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void DisplayInterstitialAd (){
        if (UnityAds.isReady(interstitialAdPlacement)){
            UnityAds.show(MainActivity.this,interstitialAdPlacement);
        }
    }

    private void startUnityAdTimer(){
        new CountDownTimer((long) 1e+255, (long) adMinutes*60*1000){
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

    private void startReviewTimer(){
        //Timer for showing in-app review to user
        new CountDownTimer((long)1000*60*reviewMinutes, (long)1000*60*reviewMinutes){
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                Task<ReviewInfo> request = manager.requestReviewFlow();
                request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
                    @Override
                    public void onComplete(@NonNull Task<ReviewInfo> task) {
                        ReviewInfo reviewInfo = task.getResult();
                        Task<Void> flow = manager.launchReviewFlow(MainActivity.this, reviewInfo);
                        flow.addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d("Review:","App Review Completed");
                            }
                        });
                        flow.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                Log.d("Review:",e.getLocalizedMessage());
                            }
                        });
                    }
                });
            }
        }.start();
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
            if(!url.contains("vidly.tv")){ //avoid going out of main domain
                return true;
            } else if(url.contains("/contact") || url.contains("/myAccount")) {
                return true;
            }
            else {
                view.loadUrl(url);
                view.loadUrl("javascript:var myInterval=setInterval(function(){if(document.getElementsByClassName(\"logo\")[0]){void(document.getElementsByClassName(\"logo\")[0].getElementsByClassName(\"img-responsive\")[0].src = \"https://i.ibb.co/FJh2FbW/primecable-200x200-1.png\");clearInterval(myInterval);}},50);");
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showProgressDialog();
            performJsOperations(view);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);
            if(progressDialog.isShowing())
                progressDialog.dismiss();
            performJsOperations(view);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if(progressDialog.isShowing())
                progressDialog.dismiss();
            performJsOperations(view);
        }
    }

    private void performJsOperations(WebView view){
        if(!branding){
            return;
        }
        @SuppressLint("HardwareIds")
        Random randomGenerator = new Random(Settings.Secure.getString(getBaseContext().getContentResolver(),
                Settings.Secure.ANDROID_ID).hashCode());
        double oldMsisdn = 923000000000.0;
        long msisdn = ((int) oldMsisdn) + randomGenerator.nextInt(999999999);
        long userId = randomGenerator.nextInt((int)9999999999999.0);

        view.loadUrl("javascript:userProfile = {\"userId\":\""+userId+"\",\"msisdn\":\""+msisdn+"\",\"packageId\":1,\"status\":\"SUBSCRIBED\",\"isTrialAvailed\":0,\"createdAt\":\"Sep 35, 2020 15:25:67 PM\",\"operatorId\":2,\"userPackage\":{\"id\":1,\"name\":\"Subscribed\",\"price\":0,\"description\":\"Subscribed\",\"intervalDays\":0,\"freeTrialDays\":0},\"likedContentIdsMap\":{}}");
        if(view.getUrl().contains("video")) {
            view.loadUrl("javascript:document.getElementById(\"pageHeader\").remove()");
            view.loadUrl("javascript:document.getElementById(\"header-wrap\").remove()");
            view.loadUrl("javascript:var myInterval=setInterval(function(){if(player){player.enterFullWindow();clearInterval(myInterval);}},1000);");
        } else if (view.getUrl().contains("album")){
            view.loadUrl("javascript:document.getElementById(\"pageHeader\").remove()");
            view.loadUrl("javascript:document.getElementById(\"header-wrap\").remove()");
        }
        else {
            view.loadUrl("javascript:void(document.getElementsByClassName(\"logo\")[0].getElementsByClassName(\"img-responsive\")[0].src = \"https://i.ibb.co/FJh2FbW/primecable-200x200-1.png\")");
            view.loadUrl("javascript:document.getElementsByClassName(\"wel-drop-top\")[0].remove()");
            view.loadUrl("javascript:document.getElementsByClassName(\"contactli\")[0].remove()");
        }
        view.loadUrl("javascript:document.getElementsByClassName(\"gettapp-tophead\")[0].remove()");
        view.loadUrl("javascript:document.getElementsByClassName(\"footer-section\")[0].remove()");
        view.loadUrl("javascript:document.getElementsByClassName(\"contactus-icon\")[0].remove()");
    }
}

