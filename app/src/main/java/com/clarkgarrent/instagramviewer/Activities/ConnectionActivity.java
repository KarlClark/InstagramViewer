package com.clarkgarrent.instagramviewer.Activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.clarkgarrent.instagramviewer.R;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The purpose of this Activity is to check if there is an internet connection.  It returns
 * RESULT_OK of there is and RESULT_CANCELLED if not.  However, we start by checking if the wifi
 * is turned on.  If it isn't we give the user the option of tuning it on.  If we turn on the wifi
 * we display a progress bar while the wifi connects.  This involves monitoring connection
 * broadcasts to determine when the connection is complete.  To do this we register two
 * broadcast receivers, one to listen for Supplicant_State_Change_Action broadcasts and one to
 * listen for Connectivity_Action broadcasts.  When the wifi in enabled, it will cause a series of
 * supplicant state changes ending with SupplicantState.COMPLETED.  Shortly after that we should
 * receive a connectivity action broadcast indicating that the wifi is CONNECTED.  If at any point
 * the time between these steps is too long we want to give up and not wait for the wifi any more.
 * To do this we post a delayed runnable.  At each step we cancel the runnable and re-post it again
 * into the future.  If at any point the runnable actually runs it means the next step didn't
 * occur fast enough, and we handle the no wifi state.
 */

public class ConnectionActivity extends Activity {

    private TextView mTvWaiting;
    private ProgressBar mPbWaitWifi;
    private CheckIfOnlineAsyncTask mCheckIfOnlineAsyncTask = new CheckIfOnlineAsyncTask(this);
    private Supplicant_State_Change_Action_Receiver mSupplicant_State_Change_Action_Receiver = new Supplicant_State_Change_Action_Receiver();
    private Connectivity_Action_Receiver mConnectivity_Action_Receiver = new Connectivity_Action_Receiver();
    private WifiManager mWifiManager;
    private Handler mHandler = new Handler();
    private Runnable mWaitWifi = new WaitWifi();
    private static final String TAG = "## My Info ##";

    private class Supplicant_State_Change_Action_Receiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            Log.i(TAG, "Supplicant change= " + ((SupplicantState)intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)).name());
            // Remove runnable from the queue.
            mHandler.removeCallbacks(mWaitWifi);

            // Re-post the runnable.  If we have reach the COMPLETED state unregister the listener.
            if (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE) == SupplicantState.COMPLETED) {
                unregisterReceiver(mSupplicant_State_Change_Action_Receiver);
                mHandler.postDelayed(mWaitWifi,15000);
            } else {
                mHandler.postDelayed(mWaitWifi,5000);
            }
        }
    }

    private class Connectivity_Action_Receiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            // Check if this broadcast is for wifi and if it is connected
            ConnectivityManager cm = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null) {
                Log.i(TAG, info.getTypeName() + " network change");
            }
            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI  && info.isConnected()){
                Log.i(TAG,"Wifi Connected");
                mHandler.removeCallbacks(mWaitWifi);
                unregisterRecieivers();
                mCheckIfOnlineAsyncTask.execute();
            }
        }
    }
    private static class CheckIfOnlineAsyncTask extends AsyncTask<Void, Void,Boolean> {

        // The purpose of this AsyncTask is to check for an internet connection.  This is done
        // in the doInBackground method.  It first checks for a available connected network.
        // If it finds one, it tries to connect to www.google.com.  This connection  attempt
        // may block for a long time.  This AsyncTask is declared static so that it doesn't
        // reference the in-closing  activity.  Unfortunately, there is no way to cancel the
        // connection attempt once it blocks.  It is not interruptable. Therefore, this task
        // will hang around until the connection attempt either succeeds, times out, or
        // fails for some other reason.  It may out live the starting activity and must not
        // assume at any point that the activity is still there.

        private WeakReference<ConnectionActivity> weakActivity;

        private CheckIfOnlineAsyncTask(ConnectionActivity activity){
            // Store a weak reference to the activity
            weakActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute(){
            // Start the progress bar.
            ConnectionActivity activity = weakActivity.get();
            if (activity == null){
                return;
            }
            activity.mTvWaiting.setText(R.string.waiting_internet);
            activity.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params){

            ConnectionActivity activity = weakActivity.get();
            if (activity == null){
                return false;
            }

            // Check for an available or connected network. Start by getting an array of NetworkInfo
            // objects of all the networks.  How to do this depends on the build version.
            ConnectivityManager cm = (ConnectivityManager)activity.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] networkInfos;
            if (Build.VERSION.SDK_INT < 21){
                //noinspection deprecation
                networkInfos = cm.getAllNetworkInfo();
            } else {
                Network[] networks = cm.getAllNetworks();
                networkInfos = new NetworkInfo[networks.length];
                for (int i = 0; i < networks.length; i++){
                    networkInfos[i] = cm.getNetworkInfo(networks[i]);
                }
            }

            activity = null;
            cm = null;

            // Search for a network that can connect to the internet and is either available
            // or already connected.
            boolean haveActiveNetwork = false;
            int type;
            for (NetworkInfo info : networkInfos) {
                type = info.getType();
                if (type == ConnectivityManager.TYPE_MOBILE |
                        type == ConnectivityManager.TYPE_WIFI|
                        type == ConnectivityManager.TYPE_MOBILE_DUN){
                    if (info.isAvailable() || info.isConnectedOrConnecting()){
                        Log.i(TAG,"Found network " + info.getTypeName()+ " isAvailable= " + info.isAvailable() + " isConnectedOrConnecting= " + info.isConnectedOrConnecting() );
                        haveActiveNetwork = true;
                        break;
                    }
                }
            }

            if (! haveActiveNetwork){
                Log.i(TAG,"no active network");
                return false;
            }

            // To check for an internet connection, try to connect to google.
            try
            {
                HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                urlc.setConnectTimeout(10000);  // These timeouts have no affect on any device I
                urlc.setReadTimeout(10000);     // have tested on.
                urlc.setRequestProperty("Connection", "close");
                Log.i(TAG, "Trying to connect");
                urlc.connect();
                return (urlc.getResponseCode() == 200);
            } catch (Exception e){
                Log.i(TAG, e.toString());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isOnLine) {
            Log.i(TAG, "onPostExecute isOnLine= " + isOnLine);
            final ConnectionActivity activity = weakActivity.get();
            if (activity == null || isCancelled()){
                return;
            }
            if (isOnLine){
                // Return RESULT_OK to calling activity.
                activity.setResult(RESULT_OK);
                activity.finish();
            } else {
                // Tell the user there is no internet connection. Return RESULT_CANCELLED which
                // was set in onCreate();
                activity.setVisibility(View.INVISIBLE);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                AlertDialog alertDialog = builder.setMessage(R.string.no_internet)
                        .setCancelable(false)
                        .setNeutralButton(activity.getString(R.string.Ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.finish();
                            }
                        })
                        .create();
                alertDialog.show();
                TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
                if (textView != null) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, activity.getResources().getInteger(R.integer.dialog_text_size));
                }
            }
        }

        @Override
        protected void onCancelled(Boolean b){
            Log.i(TAG,"Asynctask cancelled activity= " + weakActivity.get());
        }
    }


    private class WaitWifi implements Runnable {
        @Override
        public void run(){
            // If this method is running it means the wifi isn't connecting fast enough and
            // we are not going to wait for it anymore.  If WifiManager reports the state of
            // the wifi as WIFI_STATE_UNKNOWN then there is a good chance the app is running on
            // an emulator which doesn't support wifi.  So just precede to check for an
            // internet connection.  Otherwise as the user if they want to use the mobile
            // network.  If they do, check for an internet connection, otherwise stop the app.
            unregisterRecieivers();
            Log.i(TAG, "Wait for Wifi timed out wifi state= " + mWifiManager.getWifiState());
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN){
                mCheckIfOnlineAsyncTask.execute();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionActivity.this);
                AlertDialog alertDialog = builder.setMessage(R.string.no_wifi)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mCheckIfOnlineAsyncTask.execute();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create();
                alertDialog.show();
                TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
                if (textView != null) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getResources().getInteger(R.integer.dialog_text_size));
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        setResult(RESULT_CANCELED);

        mTvWaiting = (TextView)findViewById(R.id.tvWaiting);
        mPbWaitWifi = (ProgressBar)findViewById(R.id.pbWaitWifi);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        int wifiState = mWifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_UNKNOWN){
            // If the wifi state is unknown, it probably means we are running on an
            // emulator which doesn't actually support wifi.  So just go directly to checking
            // for an internet connection.
            mCheckIfOnlineAsyncTask.execute();
        } else {
            if (!mWifiManager.isWifiEnabled()) {
                // Wifi is off.  Ask user if he wants to turn it on.
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog alertDialog = builder.setMessage(getString(R.string.wifi_off))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(TAG, "Yes clicked");
                                setVisibility(View.VISIBLE);
                                registerReceiver(mSupplicant_State_Change_Action_Receiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                                registerReceiver(mConnectivity_Action_Receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                                mWifiManager.setWifiEnabled(true);
                                mHandler.removeCallbacks(mWaitWifi); // Receiver may have posted one.
                                mHandler.postDelayed(mWaitWifi, 5000);
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User doesn't want to turn on wifi.  Go ahead and check for
                                // an internet connection on another network.
                                Log.i(TAG, "no clicked");
                                mCheckIfOnlineAsyncTask.execute();
                            }
                        })
                        .create();
                alertDialog.show();
                TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
                if (textView != null) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getResources().getInteger(R.integer.dialog_text_size));
                }

            } else {
                // Wifi is on so check for internet connection.
                mCheckIfOnlineAsyncTask.execute();
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy ConnectionActivity");
        mCheckIfOnlineAsyncTask.cancel(true);
        mHandler.removeCallbacks(mWaitWifi);
        unregisterRecieivers();
    }

    private void unregisterRecieivers(){
        try {
            unregisterReceiver(mSupplicant_State_Change_Action_Receiver);
        } catch (Exception e) {}
        try {
            unregisterReceiver(mConnectivity_Action_Receiver);
        } catch (Exception e) {}
    }


    private void setVisibility(int i){
        mTvWaiting.setVisibility(i);
        mPbWaitWifi.setVisibility(i);
    }


}
