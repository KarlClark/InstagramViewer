package com.clarkgarrent.instagramviewer.Activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.clarkgarrent.instagramviewer.R;

/**
 * This activity handles the OAuth duties of the app and allows the user to login to
 * his Instagram account.  The activity uses a WebView to interface with the Instagram
 * website and display the Instagram login screen. After the user enter his usercode and
 * password Instagram returns our redirect uri.  This actual contains our access token.
 * We must strip the token from the uri string.  We return the token to the calling
 * activity using an intent extra in setResult.
 */

public class OAuthActivity extends AppCompatActivity {

    private WebView mWvLogin;
    private static final String INSTAGRAM_AUTHORITY = "api.instagram.com";
    public static final String TOKEN_EXTRA = " token_extra";
    private static final String REDIRECT_URI= "https://www.23andme.com";
    private static final String CLIENT_ID = "0637825256de4d9e9c969ec594b032c8";
    private static final String TAG ="## My Info ##";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth);

        setResult(RESULT_CANCELED, new Intent());
        // If we don't remove cookies, Instagram will often just send us the access token
        // without displaying the login screen.  If we got into the activity we assume the user
        // opted to have to log in again (ie. by logging out early);
        removeCookies();
        setUpWebView();
        mWvLogin.loadUrl(generateUrlString());
    }

    private void setUpWebView(){
        mWvLogin = (WebView) findViewById(R.id.wvLogin);
        mWvLogin.clearCache(true);
        mWvLogin.getSettings().setJavaScriptEnabled(true);
        mWvLogin.getSettings().setBuiltInZoomControls(true);
        mWvLogin.getSettings().setDisplayZoomControls(false);
        mWvLogin.setWebViewClient(mWebViewClient);
    }

    private String generateUrlString(){
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority(INSTAGRAM_AUTHORITY)
                .path("oauth/authorize/")
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("scope", "public_content+likes")
                .appendQueryParameter("response_type", "token")
                .build();
        return (Uri.decode(uri.toString()));
    }

    @SuppressWarnings("deprecation")
    private void removeCookies(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else
        {
            CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(this);
            cookieSyncMngr.startSync();
            CookieManager cookieManager=CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if ((url != null) && (url.startsWith(REDIRECT_URI))) {
                // Instagram has sent us our redirect uri.
                mWvLogin.stopLoading();
                mWvLogin.setVisibility(View.INVISIBLE);
                // The access token is in the url after the equal sign.
                int indexOfToken = url.indexOf("=") + 1;
                String token = url.substring(indexOfToken);
                // Return token to calling activity.
                Intent intent = new Intent();
                intent.putExtra(TOKEN_EXTRA, token);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                // Not the redirect url so let the WebView do its thing.
                super.onPageStarted(view, url, favicon);
            }
        }
    };
}
