package com.clarkgarrent.instagramviewer.Models;

/**
 * Created by karlc on 8/9/2017.
 */

public class UserMediaResponse {

    private UserMediaData[] data;
    private Meta meta;

    public UserMediaData[] getData() {
        return data;
    }

    public void setData(UserMediaData[] userMediaData) {
        this.data = userMediaData;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }
}
