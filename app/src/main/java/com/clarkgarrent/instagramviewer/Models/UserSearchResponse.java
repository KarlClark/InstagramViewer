package com.clarkgarrent.instagramviewer.Models;

/**
 * Created by karlc on 8/12/2017.
 */

public class UserSearchResponse {

    private UserData[] data;
    private Meta meta;

    public UserData[] getData() {
        return data;
    }

    public void setData(UserData[] data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }
}
