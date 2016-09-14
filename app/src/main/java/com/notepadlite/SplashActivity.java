package com.notepadlite;

        import android.util.Log;
        import android.content.Intent;
        import android.os.Bundle;
        import android.os.Handler;
        import android.widget.Toast;
        import com.notepadlite.ConnectionDetector;
        import android.Manifest;
        import com.google.android.gms.ads.AdListener;
        import com.google.android.gms.ads.AdRequest;
        import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by HIT on 26-08-2016.
 */
public class SplashActivity extends ActivityManagePermission {

    private InterstitialAd interstitial;
    private static int SPLASH_TIME_OUT = 3000;
    AdRequest adRequest;
    private boolean isInternetPresent = false;
    private ConnectionDetector cd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        askCompactPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, new PermissionResult() {
            @Override
            public void permissionGranted() {

            }

            @Override
            public void permissionDenied() {
                Log.d("SplashActivity", "denied");
                //permission denied
                //replace with your action
            }
        });

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                isInternetPresent = cd.isConnectingToInternet();
                if (isInternetPresent) {

                    interstitial.loadAd(adRequest);// Prepare an Interstitial Ad Listener
                    interstitial.setAdListener(new AdListener() {
                        public void onAdLoaded() {
                            // Call displayInterstitial() function
                            displayInterstitial();
                        }

                        @Override
                        public void onAdClosed() {
                            Intent i = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(i);
                            finish();
                        }

                        @Override
                        public void onAdFailedToLoad(int errorCode) {
                            Intent main = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(main);
                        }
                    });
                }
                else {

                    // CustomToast.mt(getApplicationContext(), "Please Check your Internet Connection");

                    Toast.makeText(SplashActivity.this, R.string.internet_not_available, Toast.LENGTH_SHORT).show();
                    Intent main=new Intent(SplashActivity.this,MainActivity.class);
                    startActivity(main);
                }
            }
        }, SPLASH_TIME_OUT);
        interstitial = new InterstitialAd(SplashActivity.this);
        interstitial.setAdUnitId("ca-app-pub-6864292774247103/2176710674");
        adRequest = new AdRequest.Builder().build();
        cd = new ConnectionDetector(SplashActivity.this);
    }

    public void displayInterstitial() {
        // If Ads are loaded, show Interstitial else show nothing.
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }
}