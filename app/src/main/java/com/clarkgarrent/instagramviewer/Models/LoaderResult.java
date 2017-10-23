package com.clarkgarrent.instagramviewer.Models;

import java.util.List;

/**
 * Created by karlc on 10/6/2017.
 */

public class LoaderResult {

    private List data;
    private Meta meta;

    public LoaderResult(List data, Meta meta){
        this.data = data;
        this.meta = meta;
    }

    public List getData() {
        return data;
    }

    public Meta getMeta() {
        return meta;
    }
}
