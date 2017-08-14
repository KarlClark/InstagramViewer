package com.clarkgarrent.instagramviewer.Activities;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.clarkgarrent.instagramviewer.Adapters.LargeViewAdapter;
import com.clarkgarrent.instagramviewer.Adapters.ThumbsAdapter;
import com.clarkgarrent.instagramviewer.GlobalValues;
import com.clarkgarrent.instagramviewer.InstagramEndpointsInterface;
import com.clarkgarrent.instagramviewer.Models.PostDeleteLikeResponse;
import com.clarkgarrent.instagramviewer.Models.UserMediaData;
import com.clarkgarrent.instagramviewer.Models.UserMediaResponse;
import com.clarkgarrent.instagramviewer.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This activity uses a RecyclerView to display images one per page. It lets the user like or
 * un-like an image. The list used by the RecyclerView adapter has already been created and stored
 * in a global static field, so there is not much this activity need to do.  See the GridViewActivity
 * for some explanation on how the Retrofit calls work.
 */
public class LargeViewActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private LargeViewAdapter mLargeViewAdapter;
    private InstagramEndpointsInterface mApiService;
    private int mPosition;
    public static final String POSITION_EXTRA = "position_extra";
    public static final String TAG = "## My Info ##";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_large_view);

        setResult(RESULT_CANCELED, new Intent());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpRetrofitService();

        mRecyclerView = (RecyclerView)findViewById(R.id.rvLargeView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // The SnapHelper causes the RecyclerView to scroll one item (one page in our case) at a time.
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);

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
                mLargeViewAdapter.notifyItemChanged(mPosition);
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
        mRecyclerView.setAdapter(mLargeViewAdapter);
        mRecyclerView.scrollToPosition(getIntent().getIntExtra(POSITION_EXTRA, 0));
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


    private void setUpRetrofitService(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GlobalValues.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mApiService = retrofit.create(InstagramEndpointsInterface.class);
    }

    private class MyCallback implements Callback<PostDeleteLikeResponse> {

        @Override
        public void onResponse(Call<PostDeleteLikeResponse> call, Response<PostDeleteLikeResponse> response) {

        }

        public void onFailure(Call<PostDeleteLikeResponse> call, Throwable t) {
            Log.e(TAG,"onFailure: " + t.getMessage());
        }
    }
}
