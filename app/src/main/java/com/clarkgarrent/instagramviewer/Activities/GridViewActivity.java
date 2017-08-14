package com.clarkgarrent.instagramviewer.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.clarkgarrent.instagramviewer.Models.ErrorResponse;
import com.clarkgarrent.instagramviewer.Models.LikesResponse;
import com.clarkgarrent.instagramviewer.Models.Meta;
import com.clarkgarrent.instagramviewer.Models.PostDeleteLikeResponse;
import com.clarkgarrent.instagramviewer.Models.UserData;
import com.clarkgarrent.instagramviewer.Models.UserMediaData;
import com.clarkgarrent.instagramviewer.Models.UserMediaResponse;
import com.clarkgarrent.instagramviewer.Models.UserSearchResponse;
import com.clarkgarrent.instagramviewer.R;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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
 *  to do this.  Using Retrofit involves several steps.
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
 *      Then this interface is passed to Retrofit which uses it to generate a service class that
 *      has concrete methods corresponding to each method in the interface.  These methods can
 *      be used to down load the JSON data and fill the models.
 *
 *  The activity then uses the data to fill in the UI.  Note: the JSON data contains URLs pointing
 *  to the actual images.  The actual images are retrieved by the RecyclerView adapter using
 *  Square's Picasso library.
*/
public class GridViewActivity extends AppCompatActivity {

    private RecyclerView mRecylclerView;
    private TextView mTvInfo;
    private View mDialogView;
    private EditText mEtUsername;
    private Dialog mUsernameDialog;
    private ThumbsAdapter mThumbsAdapter;
    private InstagramEndpointsInterface mApiService;
    private SharedPreferences mPrefs;
    private String mUsername;
    private String mUserId;
    private UserData[] mUserData;
    private boolean mShowRevertOption = false;
    private static final int OAUTH_ACTIVITY_TAG = 0;
    private static final int CONNECTION_ACTIVITY_TAG = 1;
    private static final int LARGE_VIEW_ACTIVITY_TAG = 2;
    private static final String TAG = "## My Info ##";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Start the activity to check the network connection. If all goes well, it
        // won't even interact with the user.  Note: the real work of this activity
        // (downloading data) doesn't start until the started activity returns in
        // onActivityResult();
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
        getSelfMedia();
    }

    // Called when the Logout option in the action bar is clicked. Finish the app but
    // don't save the access token.  The next time the app starts it will check and
    // see there is no access token.  It will start the OAuthActivity and user will
    // have to log on again.
    public void onLogoutClicked(MenuItem mi){
        mPrefs.edit().putString(GlobalValues.PREFS_TOKEN, "").commit();
        finish();
    }

    private void setUpViews(){

        mRecylclerView = (RecyclerView)findViewById(R.id.rvThumbs);
        mRecylclerView.setLayoutManager(new GridLayoutManager(this, 3));

        mTvInfo = (TextView)findViewById(R.id.tvInfo);  // Located on top of screen

        // The following two views are used inside the alert dialog.  The dialog is used
        // to retrieve a username from the user.
        mDialogView = getLayoutInflater().inflate(R.layout.dialog_layout, null);
        mEtUsername = (EditText)mDialogView.findViewById(R.id.etUsername);
        AlertDialog.Builder builder = new AlertDialog.Builder(GridViewActivity.this);
        mUsernameDialog = builder.setView(mDialogView)
                            .setPositiveButton(getString(R.string.search), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mEtUsername.getText().toString().equals("")){
                                        return;
                                    }
                                    mShowRevertOption = true;
                                    invalidateOptionsMenu();
                                    mUsername = mEtUsername.getText().toString();
                                    getMatchingUsers();
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

        // As mentioned above, this is where Retrofit is used to generate a concrete
        // class from our interface.

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

    // The following method uses the username the user entered to search for matching names on
    // Instagram website. THe names are then displayed in a dialog so the usr can choose one.
    private void getMatchingUsers(){
        // The following is an example of using the retrofit library to retrieve JSON data from
        // a REST api and deserialize it into java model objects.  In the below line,
        // getMatchingUsers is a method name from our retrofit interface, and UserSearchResponse
        // is the java class the data will be deserialized into.
        Call<UserSearchResponse> call = mApiService.getMatchingUsers(mUsername,GlobalValues.token);

        // The Call object is now placed in a queue ot run asynchronously with a callback attached.
        call.enqueue(new Callback<UserSearchResponse>() {

            @Override
            public void onResponse(Call<UserSearchResponse> call, Response<UserSearchResponse> response) {

                Meta meta = null;
                if (response.body() != null){
                    meta = response.body().getMeta();
                } else {
                    if (response.errorBody() != null){
                        meta = getMetaFromErrorBody(response.errorBody());
                    }
                }
                if (metaError(meta, false)){
                    return;  // Instagram returns errors in an JSON object called meta.
                }

                // Instagram returns data in an object call data.  We store the data in a
                // global variable so it can be accessed in the dialog callback below.
                mUserData = response.body().getData();

                // Check if there are any matching names.
                if (mUserData.length == 0){
                    mTvInfo.setText(getString(R.string.NoMatched, mUsername));
                    return;
                }

                // If there is only one name retrieved, and it is an exact match to what the
                // user asked for, then we don't have to show the user a list, just go ahead and
                // get the data.
                if (mUserData.length == 1 && mUserData[0].getUsername() == mUsername){
                    mUserId = mUserData[0].getId();
                    getUserMediaAnLiked();
                }

                // Store names in an array and display to array in an AlertDialog.
                final String[] userNames = new String[mUserData.length];
                for (int i = 0; i < mUserData.length; i++){
                    userNames[i] = mUserData[i].getUsername();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(GridViewActivity.this);
                        builder.setTitle(getString(R.string.ChoseUser))
                                .setItems(userNames, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Call method to get data for user.  Global variables used later
                                    // in callbacks.
                                    mUsername = userNames[which];
                                    mUserId = mUserData[which].getId();
                                    getUserMediaAnLiked();
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

            @Override
            public void onFailure(Call<UserSearchResponse> call, Throwable t) {
                failureError(t);
            }
        });
    }

    // The Retrofit aspects of this method are described above.  First we queue up a call
    // retrieve a list of images the user likes.  When the call returns we retrieve the media data.
    // this way we know the likes data is available when we process the media data.
    private void getSelfMediaAndLiked(){
        Call<LikesResponse> call = mApiService.getLiked(GlobalValues.token);
        call.enqueue(new Callback<LikesResponse>() {
            @Override
            public void onResponse(Call<LikesResponse> call, Response<LikesResponse> response) {

                Meta meta = null;
                if (response.body() != null){
                    meta = response.body().getMeta();
                } else {
                    if (response.errorBody() != null){
                        meta = getMetaFromErrorBody(response.errorBody());
                    }
                }
                if (metaError(meta, true)) {
                    return;
                }
                // Store data in a public static class so other activities can access it.
                GlobalValues.setLikedIds(response.body().getData());
                // Get the media data.
                getSelfMedia();
            }

            @Override
            public void onFailure(Call<LikesResponse> call, Throwable t) {
                failureError(t);
            }
        });
    }

    // Similar to above method but for a different username.
    private void getUserMediaAnLiked(){
        Call<LikesResponse> call = mApiService.getLiked(GlobalValues.token);
        call.enqueue(new Callback<LikesResponse>() {

            @Override
            public void onResponse(Call<LikesResponse> call, Response<LikesResponse> response) {

                Meta meta = null;
                if (response.body() != null){
                    meta = response.body().getMeta();
                } else {
                    if (response.errorBody() != null){
                        meta = getMetaFromErrorBody(response.errorBody());
                    }
                }
                if (metaError(meta, false)) {
                    return;
                }
                GlobalValues.setLikedIds(response.body().getData());
                getUserMedia();
            }

            @Override
            public void onFailure(Call<LikesResponse> call, Throwable t) {
                failureError(t);
            }
        });
    }

    // Get media data for a specific user.
    private void getUserMedia(){

        Call<UserMediaResponse> call = mApiService.getUserMedia(mUserId, GlobalValues.token);
        call.enqueue(new Callback<UserMediaResponse>() {
            @Override
            public void onResponse(Call<UserMediaResponse> call, Response<UserMediaResponse> response) {
                processMediaData(response,getString(R.string.pictures_from, mUsername),getString(R.string.User_no_images , mUsername), false );
            }

            @Override
            public void onFailure(Call<UserMediaResponse> call, Throwable t) {

            }
        });
    }

    // Get media data for the user of the app.
    private void getSelfMedia(){

        Call<UserMediaResponse> call = mApiService.getSelfMedia(GlobalValues.token);
        call.enqueue(new Callback<UserMediaResponse>() {

            @Override
            public void onResponse(Call<UserMediaResponse> call, Response<UserMediaResponse> response) {
                processMediaData(response, getString(R.string.Your_pictres), getString(R.string.Self_no_images), true);
            }

            @Override
            public void onFailure(Call<UserMediaResponse> call, Throwable t) {
                failureError(t);
            }
        });
    }

    // Process the media data.  This involves building up an array list that can be passed to
    // the RecyclerView adapter. First we throw out video files.  Then we compare each item to see
    // if it matches one of the items in our likes list. idMsg describes the user and is displayed
    // above the RecyclerView.
    private void processMediaData(Response<UserMediaResponse> response, String idMsg, String errorMsg, boolean isFatal){

        Meta meta = null;
        if (response.body() != null){
            meta = response.body().getMeta();
        } else {
            if (response.errorBody() != null){
                meta = getMetaFromErrorBody(response.errorBody());
            }
        }

        if (metaError(meta, isFatal)){
            return;
        }
        GlobalValues.setUserMediaData(response.body().getData());

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
                mThumbsAdapter.notifyItemChanged(position);
                if (userMediaData.isLiked()){
                    userMediaData.setLiked(false);
                    Call<PostDeleteLikeResponse> call = mApiService.deleteLike(userMediaData.getId(),GlobalValues.token);
                    call.enqueue(new MyCallback());
                } else {
                    userMediaData.setLiked(true);
                    Call<PostDeleteLikeResponse> call = mApiService.postLike(userMediaData.getId(),GlobalValues.token);
                    call.enqueue(new MyCallback());
                }
            }
        });
        mRecylclerView.setAdapter(mThumbsAdapter);
        mRecylclerView.invalidate();
    }


    private  Meta getMetaFromErrorBody(ResponseBody responseBody){
        Gson gson = new Gson();
        ErrorResponse response;
        TypeAdapter<ErrorResponse> adapter = gson.getAdapter(ErrorResponse.class);
        try {
            response = adapter.fromJson(responseBody.string());
            return response.getMeta();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Check the Meta object for errors.  If we have an OAuthAccessTokenException, then
    // the access token probably expired.  Start the OAuthActivity so user can login.
    // Otherwise show the error to the user then close the app.
    private boolean metaError(Meta meta, boolean isFatal) {

        if (meta == null){
            showErrorDialog(getString(R.string.Unknown), isFatal);
        }

        if ( ! meta.getCode().equals("200")){
            if (meta.getError_type().equals("OAuthAccessTokenException")){
                StartOauthActivity();
            }else{
                Log.e(TAG,"Instagram api error " + meta.getCode() + "  " + meta.getError_type() + " " + meta.getError_message());
                showErrorDialog(getString(R.string.error , meta.getError_message()), isFatal);
            }
            return true;
        }
        return false;
    }

    private void showErrorDialog(String msg, final boolean isFatal){
        String buttonLabel = (isFatal) ? getString(R.string.close_app) : getString(R.string.Ok);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setNeutralButton(buttonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isFatal) {
                            finish();
                        }
                    }
                })
                .create().show();
    }

    // Retrofit returned some kind of network error.  Display the error and shut down.
    private void failureError(Throwable t){
        Log.e(TAG, getString(R.string.onFailure, t.getMessage()));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.error , t.getMessage()))
                .setNeutralButton(R.string.close_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_CANCELED, new Intent());
                        finish();
                    }
                })
                .create().show();
    }

    private class MyCallback implements Callback<PostDeleteLikeResponse> {

        @Override
        public void onResponse(Call<PostDeleteLikeResponse> call, Response<PostDeleteLikeResponse> response) {

        }

        public void onFailure(Call<PostDeleteLikeResponse> call, Throwable t) {
            Log.e(TAG,"onFailure: " + t.getMessage());
        }
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
                    mPrefs.edit().putString(GlobalValues.PREFS_TOKEN, GlobalValues.token).commit();
                    getSelfMediaAndLiked();
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
                        getSelfMediaAndLiked();
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
