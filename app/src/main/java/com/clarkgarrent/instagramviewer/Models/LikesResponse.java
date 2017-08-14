package com.clarkgarrent.instagramviewer.Models;

/**
 * Created by karlc on 8/9/2017.
 */

public class LikesResponse{

    private LikesData[] data;
    private Meta meta;

    public LikesData[] getData(){
        return data;
    }

    public void setData(LikesData[] data){
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }
}
