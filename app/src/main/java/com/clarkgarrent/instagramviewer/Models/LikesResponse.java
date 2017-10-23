package com.clarkgarrent.instagramviewer.Models;

import com.clarkgarrent.instagramviewer.LoaderInterface;

import java.util.ArrayList;

/**
 * Created by karlc on 8/9/2017.
 */

public class LikesResponse implements LoaderInterface{

    private ArrayList<LikesData> data;
    private Meta meta;

    public ArrayList<LikesData> getData(){
        return data;
    }

    public void setData(ArrayList<LikesData> data){
        this.data = data;
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
