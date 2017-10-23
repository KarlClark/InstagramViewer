package com.clarkgarrent.instagramviewer.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.clarkgarrent.instagramviewer.Adapters.ThumbsAdapter;
import com.clarkgarrent.instagramviewer.GlobalValues;
import com.clarkgarrent.instagramviewer.InstagramEndpointsInterface;
import com.clarkgarrent.instagramviewer.JsonLoader;
import com.clarkgarrent.instagramviewer.Models.LikesData;
import com.clarkgarrent.instagramviewer.Models.LoaderResult;
import com.clarkgarrent.instagramviewer.Models.Meta;
import com.clarkgarrent.instagramviewer.Models.UserData;
import com.clarkgarrent.instagramviewer.Models.UserMediaData;
import com.clarkgarrent.instagramviewer.R;
import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** This Activity performs much of the work of the app. It provides the following user functionality:
 *     Displays images in a RecyclerView grid layout.
 *     Allows user to switch to am Activity for viewing images one at a time in a larger size.
 *     Allows user to "like/un-like" an image.
 *     Allows user to search for another username.
 *     Allows user to select a username from the ones found in the search.
 *     When viewing images from another user, allows user to revert back to his own images.
 *     Allows user to logout.
 *  To provide this functionality, it starts several other activities.  One checks for a network
 *  connection and allows user to turn on the Wifi.  Another allows user to login to Instagram.
 *
 *  This activity  also retrieves data from Instagram's REST api.  It uses Square's Retrofit library
 *  and an AsyncTaskLoader to do this.  Using Retrofit involves several steps.
 *      First you create a bunch of java (POJO) model classes that correspond to the JSON data
 *      described on the Instagram web site.  Retrofit will automatically generate instances of
 *      these POJOs from the JSON data.  These objects can then be used programmatically to access
 *      the data.
 *
 *      Then create an interface that describes the REST api calls (GET, POST, DELETE) that will
 *      be made on the Instagram endpoints.  Each method in this interface describes a request to
 *      a different endpoint.  Java annotations provided by the Retrofit library are used
 *      extensively to write these methods.
 *
 *      Then this interface is passed to Retrofit which uses it to generate a service class.  This
 *      service can be used to generate what Retrofit terms a Call object that corresponds to
 *      a specific endpoint.  Calling teh execute method on the call object will retrieve the
 *      data from that endpoint.  This activity passes the call object to the AsyncTaskLoader
 *      to load the data in the background.
 *
 *  The activity then uses the data to fill in the UI.  Note: the JSON data contains URLs pointing
 *  to the actual images.  The actual images are retrieved by the RecyclerView adapter using
 *  Square's Picasso library.
*/
public class GridViewActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LoaderResult> {

    private RecyclerView mRecyclerView;
    private TextView mTvInfo;
    private EditText mEtUsername;
    private Dialog mUsernameDialog;
    private ThumbsAdapter mThumbsAdapter;
    private InstagramEndpointsInterface mApiService;
    private SharedPreferences mPrefs;
    private String mUsername;
    private String mUserId;
    private String mLikeId;
    private boolean mShowRevertOption = false;
    private static final int OAUTH_ACTIVITY_TAG = 0;
    private static final int CONNECTION_ACTIVITY_TAG = 1;
    private static final int LARGE_VIEW_ACTIVITY_TAG = 2;
    private static final int SELF_MEDIA_LOAD = 3;
    private static final int SELF_MEDIA_AND_LIKES_LOAD = 4;
    private static final int USER_MEDIA_AND_LIKES_LOAD = 5;
    private static final int USER_MEDIA_LOAD = 6;
    private static final int MATCHING_USERS_LOAD = 7;
    private static final int POST_LIKE_LOAD = 8;
    private static final int DELETE_LIKE_LOAD = 9;

    private static final String TAG = "## My Info ##";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Start the activity to check the network connection. If all goes well, it
        // won't even interact with the user.  Note: the real work of this activity
        // (downloading data) doesn't start until the started activity returns a result
        // in onActivityResult();
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivityForResult(intent, CONNECTION_ACTIVITY_TAG);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_view);

        setUpViews();

        setUpRetrofitService();

        // Retrieve the saved access token.
        mPrefs = getSharedPreferences(GlobalValues.PREFS_NAME, Activity.MODE_PRIVATE);
        GlobalValues.token = mPrefs.getString(GlobalValues.PREFS_TOKEN, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        MenuItem mi = menu.findItem(R.id.miRevert);
        if (mShowRevertOption) {
            mi.setEnabled(true).setVisible(true);
        } else {
            mi.setEnabled(false).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args){

        // The LoaderManager is for a loader instance.  We use the id value to create an
        // instance of a loader that will download from the specific endpoint.  The loader
        // requires a Retrofit Call instance which is created using the Retrofit service.
        Log.i(TAG,"onCreateLoader");

        switch (id) {
            case SELF_MEDIA_AND_LIKES_LOAD:
            case USER_MEDIA_AND_LIKES_LOAD:
                // We need the likes data before we can process the media data, so load it
                // first.  The media load will be started when the likes data is returned
                // in the onLoadFinished callback.
                return new JsonLoader(this, mApiService.getLiked(GlobalValues.token));

            case SELF_MEDIA_LOAD:
                return new JsonLoader(this, mApiService.getSelfMedia(GlobalValues.token));

            case USER_MEDIA_LOAD:
                return new JsonLoader(this, mApiService.getUserMedia(mUserId, GlobalValues.token));

            case MATCHING_USERS_LOAD:
                return new JsonLoader(this, mApiService.getMatchingUsers(mUsername, GlobalValues.token));

            case POST_LIKE_LOAD:
                return new JsonLoader(this, mApiService.postLike(mLikeId, GlobalValues.token));

            case DELETE_LIKE_LOAD:
                return new JsonLoader(this, mApiService.deleteLike(mLikeId, GlobalValues.token));
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult){
        Log.i(TAG,"onLoadFinished");
        // One of the possible loader instances has returned its data.

        // First check to see if there was an error.
        if (metaError(loaderResult.getMeta(), loader.getId())){
            return;
        }

        switch (loader.getId()) {
            case SELF_MEDIA_AND_LIKES_LOAD:
                // We have the likes data so now get the media data.
                GlobalValues.setLikedIds ((ArrayList<LikesData>)loaderResult.getData());
                getLoaderManager().restartLoader(SELF_MEDIA_LOAD, null, this);
                break;

            case USER_MEDIA_AND_LIKES_LOAD:
                // We have the likes data so now get the media data.
                GlobalValues.setLikedIds ((ArrayList<LikesData>)loaderResult.getData());
                getLoaderManager().restartLoader(USER_MEDIA_LOAD, null, this);
                break;

            case SELF_MEDIA_LOAD:
                processMediaData((ArrayList<UserMediaData>)loaderResult.getData(), getString(R.string.Your_pictres), getString(R.string.Self_no_images));
                break;

            case USER_MEDIA_LOAD:
                processMediaData((ArrayList<UserMediaData>)loaderResult.getData(),getString(R.string.pictures_from, mUsername) , getString(R.string.User_no_images , mUsername));
                break;

            case MATCHING_USERS_LOAD:
                processMatchingUsersData((ArrayList<UserData>)loaderResult.getData());
                break;

            case POST_LIKE_LOAD:
            case DELETE_LIKE_LOAD:
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader){
        // Delete references to loader data.
        Log.i(TAG,"onLoaderReset");
        switch(loader.getId()){
            case SELF_MEDIA_AND_LIKES_LOAD:
            case USER_MEDIA_AND_LIKES_LOAD:
                GlobalValues.setLikedIds(null);
                break;
            case SELF_MEDIA_LOAD:
            case USER_MEDIA_LOAD:
                mRecyclerView.setAdapter(null);
                mThumbsAdapter = null;
                GlobalValues.alUserMediaData = null;
                break;
            case MATCHING_USERS_LOAD:
                mUsername = null;
                mUserId = null;
                break;
            default: {}
        }
    }

    // Called when the search option in the action bar is clicked.  Display a dialog allowing
    // user to enter a username.
    public void onSearchClicked(MenuItem mi){
        mUsernameDialog.show();
    }

    // Called when the revert option in the action bar is clicked.  Turn off the revert
    // icon and then retrieve the user's own data.
    public void onRevertClicked(MenuItem mi){
        mShowRevertOption = false;
        invalidateOptionsMenu();
        getLoaderManager().initLoader(SELF_MEDIA_LOAD, null, this);
    }

    // Called when the Logout option in the action bar is clicked. Finish the app but
    // don't save the access token.  The next time the app starts it will check and
    // see there is no access token.  It will start the OAuthActivity and user will
    // have to log on again.
    public void onLogoutClicked(MenuItem mi){
        mPrefs.edit().putString(GlobalValues.PREFS_TOKEN, "").apply();
        finish();
    }

    private void setUpViews(){

        mRecyclerView = (RecyclerView)findViewById(R.id.rvThumbs);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        mTvInfo = (TextView)findViewById(R.id.tvInfo);  // Located on top of screen

        // The following two views are used inside the alert dialog.  The dialog is used
        // to retrieve a username from the user.
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_layout, null);
        mEtUsername = (EditText)dialogView.findViewById(R.id.etUsername);
        AlertDialog.Builder builder = new AlertDialog.Builder(GridViewActivity.this);
        mUsernameDialog = builder.setView(dialogView)
                            .setPositiveButton(getString(R.string.search), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mEtUsername.getText().toString().equals("")){
                                        return;
                                    }
                                    mShowRevertOption = true;
                                    invalidateOptionsMenu();
                                    mUsername = mEtUsername.getText().toString();
                                    getLoaderManager().restartLoader(MATCHING_USERS_LOAD, null, GridViewActivity.this);
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create();
    }

    private void setUpRetrofitService(){

        // As mentioned above, this is where Retrofit is used to generate service that
        // can be used to created Call instances for specific endpoints.

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GlobalValues.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mApiService = retrofit.create(InstagramEndpointsInterface.class);
    }

    private void StartOauthActivity(){
        // The OAuthActivity lets the user login to Instagram and then returns
        // the access token in onActivityResult();
        Intent intent = new Intent(this, OAuthActivity.class);
        startActivityForResult(intent,OAUTH_ACTIVITY_TAG);
    }

    private void processMatchingUsersData(final ArrayList<UserData> userData){
        // Check if there are any matching names.
        if (userData.size() == 0){
            mTvInfo.setText(getString(R.string.NoMatched, mUsername));
            return;
        }

        // If there is only one name retrieved, and it is an exact match to what the
        // user asked for, then we don't have to show the user a list, just go ahead and
        // get the data.
        if (userData.size() == 1 && userData.get(0).getUsername().equals(mUsername)){
            mUserId = userData.get(0).getId();
            Log.i(TAG,"restart user media and likes load, mUserid= " + mUserId + " " + mUsername);
            getLoaderManager().restartLoader(USER_MEDIA_AND_LIKES_LOAD, null, GridViewActivity.this);
        }

        // Store names in an array and display to array in an AlertDialog.
        final String[] userNames = new String[userData.size()];
        for (int i = 0; i < userData.size(); i++){
            userNames[i] = userData.get(i).getUsername();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(GridViewActivity.this);
        builder.setTitle(getString(R.string.ChoseUser))
                .setItems(userNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Call method to get data for user.  Global variables used later
                        // in callbacks.
                        mUsername = userNames[which];
                        mUserId = userData.get(which).getId();
                        Log.i(TAG,"restart user media and likes load, mUserid= " + mUserId + " " + mUsername);
                        getLoaderManager().restartLoader(USER_MEDIA_AND_LIKES_LOAD, null, GridViewActivity.this);
                    }
                })
                .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User canceled dialog.
                    }
                })
                .create().show();
    }


    // Process the media data.  This involves building up an array list that can be passed to
    // the RecyclerView adapter. First we throw out video files.  Then we compare each item to see
    // if it matches one of the items in our likes list. idMsg describes the user and is displayed
    // above the RecyclerView.
    private void processMediaData(ArrayList<UserMediaData> data,String idMsg, String errorMsg){

        GlobalValues.setUserMediaData(data);

        if (GlobalValues.alUserMediaData.size()== 0){
            mTvInfo.setText(errorMsg);
        }
        mTvInfo.setText(idMsg);
        mThumbsAdapter = new ThumbsAdapter(GridViewActivity.this, GlobalValues.alUserMediaData);
        mThumbsAdapter.setOnItemClickListener(new ThumbsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // User clicked on an image.  Start LargeViewActivity to display images in
                // a larger format.
                Intent intent = new Intent(GridViewActivity.this, LargeViewActivity.class);
                intent.putExtra(LargeViewActivity.POSITION_EXTRA, position);
                startActivityForResult(intent,LARGE_VIEW_ACTIVITY_TAG);
            }
        });
        mThumbsAdapter.setOnButtonClickListener(new ThumbsAdapter.OnButtonClickListener() {
            @Override
            public void onButtonClick(int position) {
                // User clicked on a like button.  Send inof to Instagram.
                UserMediaData userMediaData = GlobalValues.alUserMediaData.get(position);
                mLikeId = userMediaData.getId();
                mThumbsAdapter.notifyItemChanged(position);
                if (userMediaData.isLiked()){
                    userMediaData.setLiked(false);
                    getLoaderManager().restartLoader(DELETE_LIKE_LOAD, null, GridViewActivity.this);
                } else {
                    userMediaData.setLiked(true);
                    getLoaderManager().restartLoader(POST_LIKE_LOAD, null, GridViewActivity.this);
                }
            }
        });
        mRecyclerView.setAdapter(mThumbsAdapter);
        mRecyclerView.invalidate();
    }

    // Check the Meta object for errors.  If we have an OAuthAccessTokenException, then
    // the access token probably expired.  Start the OAuthActivity so user can login.
    // Otherwise show the error to the user.
    private boolean metaError(Meta meta, int loaderId) {

        if (meta == null){
            showErrorDialog(getString(R.string.Unknown), loaderId);
            return true;
        }

        if (meta.getCode().equals("200")){
            return false;
        }

        if (meta.getError_type().equals("OAuthAccessTokenException")){
            StartOauthActivity();
            return true;
        }

        String msg = meta.getError_message();

        if (meta.getError_type().equals(JsonLoader.NETWORK_IO_ERROR)){
            msg = msg + getString(R.string.is_it_connected);
        }

        Log.e(TAG,"Error loading data from internet " + meta.getCode() + "  " + meta.getError_type() + " " + meta.getError_message());
        showErrorDialog(msg, loaderId);
        return true;
    }

    private void showErrorDialog(String msg, final int loaderId){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setNegativeButton(getString(R.string.close_app), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getLoaderManager().restartLoader(loaderId, null, GridViewActivity.this);
                    }
                } )
                .create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        switch (requestCode){

            case OAUTH_ACTIVITY_TAG:
                if (resultCode == RESULT_CANCELED){
                    //User pressed back button without completing login
                    finish();
                }else {
                    // Store the access token and retrieve first set of data.
                    GlobalValues.token = data.getStringExtra(OAuthActivity.TOKEN_EXTRA);
                    mPrefs.edit().putString(GlobalValues.PREFS_TOKEN, GlobalValues.token).apply();
                    getLoaderManager().restartLoader(SELF_MEDIA_AND_LIKES_LOAD, null, this);
                }
                break;

            case CONNECTION_ACTIVITY_TAG:
                if (resultCode == RESULT_CANCELED){
                    // no connection
                    finish();
                } else {
                    // We have a connection. If we don't have an access token then start
                    // OAuthActivity so user can log on.  Otherwise download first set of data.
                    if (GlobalValues.token == null || GlobalValues.token.equals("")) {
                        StartOauthActivity();
                    } else {
                        //getLiked();
                        getLoaderManager().initLoader(SELF_MEDIA_AND_LIKES_LOAD, null, this);
                    }
                }
                break;

            case LARGE_VIEW_ACTIVITY_TAG:
                if (resultCode == RESULT_OK){
                    // LargeViewActivity changed the data so we need to update the adapter.
                    mThumbsAdapter.notifyDataSetChanged();
                }
                break;
        }
    }
}
