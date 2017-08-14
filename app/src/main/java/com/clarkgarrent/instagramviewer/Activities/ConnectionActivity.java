package com.clarkgarrent.instagramviewer.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v4.net.ConnectivityManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.clarkgarrent.instagramviewer.R;

/**
 * This activity checks for a network connection. First if give the user the option to
 * turn on the wifi if it is off.  Then it check for a network connection. Returns RESULT_OK if
 * we find a connection or RESULT_CANCELED if not.
 */
public class ConnectionActivity extends AppCompatActivity {

    private boolean mWaitForWifi = true;
    private ConnectivityManager cm;
    private Network[] mNetworks;
    private ProgressBar mPbWaitWifi;
    private TextView mTvWaiting;
    private static final String TAG ="## My Info ##";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        mPbWaitWifi = (ProgressBar)findViewById(R.id.pbWaitWifi);
        mTvWaiting = (TextView)findViewById(R.id.tvWaiting);

        cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if ( ! wifiManager.isWifiEnabled()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.wifi_off))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            wifiManager.setWifiEnabled(true);
                            checkConnection();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mWaitForWifi = false;
                            checkConnection();
                        }
                    })
                    .create().show();

        }else{
            checkConnection();
        }
    }

    private void checkConnection(){

        // Get network info for wifi. If user just turned on wifi then mWaitForWifi will be true.
        // Start a background thread to wait for the wifi to connect.  Display a progress bar and
        // message while waiting. Finish activity when done.
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWaitForWifi && networkInfo.isAvailable()) {
            mPbWaitWifi.setVisibility(View.VISIBLE);
            mTvWaiting.setVisibility(View.VISIBLE);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    waitForWifi();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                                mPbWaitWifi.setVisibility(View.INVISIBLE);
                                mTvWaiting.setVisibility(View.INVISIBLE);
                                setResult(RESULT_OK, new Intent());
                                finish();
                            } else {
                                showErrorDialog();
                            }
                        }
                    });
                }
            });
            thread.start();
            return;
        }

        // Looks like wifi is not working.  Check to see if the currently active is
        // the right type and connected.  If not display an error dialog.
        NetworkInfo info = cm.getActiveNetworkInfo();
        int type = (info == null)? -1 : info.getType();
        if (info == null ||
                ( ! info.isAvailable()) || ( ! info.isConnected()) ||
                ( ! (type == ConnectivityManager.TYPE_MOBILE ||
                     type == ConnectivityManager.TYPE_WIFI ||
                     type == ConnectivityManager.TYPE_WIMAX ||
                     type == ConnectivityManager.TYPE_MOBILE_DUN))){
           showErrorDialog();
        }else {
            setResult(RESULT_OK, new Intent());
            finish();
        }
    }

    private void waitForWifi(){

        // Poll for about 15 seconds to see if we connect to wifi.
        NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        int cnt = 0;
        while ( ( ! info.isConnected()) && cnt < 30 ){
            try {
                Thread.sleep(500);
                info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                cnt++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.no_network))
                .setNeutralButton(R.string.close_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_CANCELED, new Intent());
                        finish();
                    }
                })
                .create().show();
    }
}
