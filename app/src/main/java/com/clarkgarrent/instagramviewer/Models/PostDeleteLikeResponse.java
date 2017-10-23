package com.clarkgarrent.instagramviewer.Models;

import com.clarkgarrent.instagramviewer.LoaderInterface;

/**
 * Created by karlc on 8/11/2017.
 */

public class PostDeleteLikeResponse implements LoaderInterface{

    private Meta meta;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public LoaderResult getLoaderResult(){
        return new LoaderResult(null, meta);
    }
}
