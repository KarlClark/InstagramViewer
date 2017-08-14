package com.clarkgarrent.instagramviewer.Models;

/**
 * Created by karlc on 8/9/2017.
 */

public class Images {

    private ImageUrl low_resolution;
    private ImageUrl thumbnail;
    private ImageUrl standard_resolution;

    public ImageUrl getLow_resolution() {
        return low_resolution;
    }

    public void setLow_resolution(ImageUrl low_resolution) {
        this.low_resolution = low_resolution;
    }

    public ImageUrl getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(ImageUrl thumbnail) {
        this.thumbnail = thumbnail;
    }

    public ImageUrl getStandard_resolution() {
        return standard_resolution;
    }

    public void setStandard_resolution(ImageUrl standard_resolution) {
        this.standard_resolution = standard_resolution;
    }
}
