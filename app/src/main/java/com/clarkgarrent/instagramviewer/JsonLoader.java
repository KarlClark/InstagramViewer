package com.clarkgarrent.instagramviewer;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.clarkgarrent.instagramviewer.Models.ErrorResponse;
import com.clarkgarrent.instagramviewer.Models.LoaderResult;
import com.clarkgarrent.instagramviewer.Models.Meta;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class JsonLoader extends AsyncTaskLoader<LoaderResult> {

    // A loader that loads and deserializes json data by executing Retrofit Call objects.
    // The highest level model used to deserialize the json data must implement LoaderInterface.
    // The LoaderInterface.getLoaderResult() method is used to return the deserialized objects
    // to the calling activity.

    private LoaderResult mLoaderResult;
    private Call mCall;
    public static final String NETWORK_IO_ERROR = "network_io_error";
    public static final String RESPONSE_IO_ERROR = "response_io_error";

    public JsonLoader(Context context, Call call){
        super(context);
        // The Retrofit Call object that will be used to load and deserialize the data.
        mCall = call;
    }

    @Override
    public void onStartLoading(){
        // A typical onStartLoading method based on Android documentation.
        if (mLoaderResult == null){
            forceLoad();
        } else {
            deliverResult(mLoaderResult);
        }
    }

    @Override
    public void deliverResult(LoaderResult loaderResult){
        // A typical deliverResult method based on the Android documentation.

        if (isReset()){
            return;
        }

        mLoaderResult = loaderResult; // Cache the data.

        if (isStarted()) {
            super.deliverResult(loaderResult);
        }
    }

    @Override
    public LoaderResult loadInBackground(){
        // The basic idea is to just call execute() on a Retrofit Call object.  This will
        // create instance of one of our models.  Since our models implement the LoaderInterface
        // we can retrieve a LoaderResult instance to return to the calling activity.  However
        // in the case of certain errors we have to generate our own LoaderResult object to return.
        Response response;
        try {
            response = mCall.execute();
        } catch (IOException e) {
            return new LoaderResult(null, new Meta(NETWORK_IO_ERROR, e.getMessage()));
        }

        // For some errors Instagram returns the meta data in the error body instead of
        // the response body.  In this case we have to deserialize the data ourselves.
       if (response.body() == null && response.errorBody() != null){
           Gson gson = new Gson();
           TypeAdapter<ErrorResponse> adapter = gson.getAdapter(ErrorResponse.class);
           try {
               ErrorResponse errorResponse = adapter.fromJson(response.errorBody().string());
               return new LoaderResult(null, errorResponse.getMeta());
           } catch (IOException e) {
               return new LoaderResult(null, new Meta(RESPONSE_IO_ERROR, e.getMessage()));
           }
       }

       return ((LoaderInterface)response.body()).getLoaderResult();
    }
}
