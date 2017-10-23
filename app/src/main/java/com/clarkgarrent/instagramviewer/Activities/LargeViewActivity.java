package com.clarkgarrent.instagramviewer.Activities;

import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.clarkgarrent.instagramviewer.Adapters.LargeViewAdapter;
import com.clarkgarrent.instagramviewer.GlobalValues;
import com.clarkgarrent.instagramviewer.InstagramEndpointsInterface;
import com.clarkgarrent.instagramviewer.JsonLoader;
import com.clarkgarrent.instagramviewer.Models.LoaderResult;
import com.clarkgarrent.instagramviewer.Models.Meta;
import com.clarkgarrent.instagramviewer.Models.UserMediaData;
import com.clarkgarrent.instagramviewer.R;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This activity uses a RecyclerView to display images one per page. It lets the user like or
 * un-like an image. The list used by the RecyclerView adapter has already been created and stored
 * in a global static field, so there is not much this activity need to do.  See the GridViewActivity
 * for some explanation on how the Retrofit calls work.
 */
public class LargeViewActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LoaderResult>{

    private LargeViewAdapter mLargeViewAdapter;
    private InstagramEndpointsInterface mApiService;
    private int mPosition;
    private String mLikeId;
    private static final int POST_LIKE_LOAD = 8;
    private static final int DELETE_LIKE_LOAD = 9;
    public static final String POSITION_EXTRA = "position_extra";
    public static final String TAG = "## My Info ##";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_large_view);

        setResult(RESULT_CANCELED, new Intent());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setUpRetrofitService();

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.rvLargeView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // The SnapHelper causes the RecyclerView to scroll one item (one page in our case) at a time.
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        mLargeViewAdapter = new LargeViewAdapter(this, GlobalValues.alUserMediaData);
        mLargeViewAdapter.setOnItemClickListener(new LargeViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                // User clicked on the like/unlike button of one of the images.  Update as
                // appropriate.  Set RESULT_OK so calling activity will know we changed the
                // data.  The calling activity will need to invalidate its
                // RecyclerView adapter.
                setResult(RESULT_OK, new Intent());
                mPosition = position;
                UserMediaData userMediaData = GlobalValues.alUserMediaData.get(position);
                mLikeId = userMediaData.getId();
                mLargeViewAdapter.notifyItemChanged(mPosition);
                if (userMediaData.isLiked()){
                    userMediaData.setLiked(false);
                    getLoaderManager().restartLoader(DELETE_LIKE_LOAD, null, LargeViewActivity.this);
                } else {
                    userMediaData.setLiked(true);
                    getLoaderManager().restartLoader(POST_LIKE_LOAD, null, LargeViewActivity.this);
                }
            }
        });
        recyclerView.setAdapter(mLargeViewAdapter);
        recyclerView.scrollToPosition(getIntent().getIntExtra(POSITION_EXTRA, 0));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args){
        Log.i(TAG,"onCreateLoader");

        switch (id) {

            case POST_LIKE_LOAD:
                return new JsonLoader(this, mApiService.postLike(mLikeId, GlobalValues.token));

            case DELETE_LIKE_LOAD:
                return new JsonLoader(this, mApiService.deleteLike(mLikeId, GlobalValues.token));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult){
        Log.i(TAG,"onLoadFinished");

        metaError(loaderResult.getMeta(), loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader){
        Log.i(TAG,"onLoaderReset");
    }


    private void setUpRetrofitService(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GlobalValues.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mApiService = retrofit.create(InstagramEndpointsInterface.class);
    }

    private boolean metaError(Meta meta, int loaderId) {

        if (meta == null){
            showErrorDialog(getString(R.string.Unknown), loaderId);
            return true;
        }

        if (meta.getCode().equals("200")){
            return false;
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
                        getLoaderManager().restartLoader(loaderId, null, LargeViewActivity.this);
                    }
                } )
                .create().show();
    }
}
