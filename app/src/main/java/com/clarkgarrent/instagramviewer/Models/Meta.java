package com.clarkgarrent.instagramviewer.Models;

/**
 * Created by karlc on 8/10/2017.
 */

public class Meta {

    private String error_type;
    private String code;
    private String error_message;

    public Meta(){}

    public Meta(String error_type, String error_message) {
        this.error_type = error_type;
        this.error_message = error_message;
        code = "";
    }

    public String getError_type() {
        return error_type;
    }

    public void setError_type(String error_type) {
        this.error_type = error_type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }
}
