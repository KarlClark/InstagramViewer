package com.clarkgarrent.instagramviewer.Models;

import com.clarkgarrent.instagramviewer.LoaderInterface;

import java.util.ArrayList;

/**
 * Created by karlc on 8/12/2017.
 */

public class UserSearchResponse implements LoaderInterface{

    private ArrayList<UserData> data;
    private Meta meta;

    public ArrayList<UserData> getData() {
        return data;
    }

    public void setData(ArrayList<UserData> data) {
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
