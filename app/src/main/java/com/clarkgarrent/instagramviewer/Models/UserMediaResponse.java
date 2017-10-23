package com.clarkgarrent.instagramviewer.Models;

import com.clarkgarrent.instagramviewer.LoaderInterface;

import java.util.ArrayList;

/**
 * Created by karlc on 8/9/2017.
 */

public class UserMediaResponse implements LoaderInterface{

    private ArrayList<UserMediaData> data;
    private Meta meta;

    public ArrayList<UserMediaData> getData() {
        return data;
    }

    public void setData(ArrayList<UserMediaData> userMediaData) {
        this.data = userMediaData;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public LoaderResult getLoaderResult(){
        return new LoaderResult(data, meta);
    }
}
