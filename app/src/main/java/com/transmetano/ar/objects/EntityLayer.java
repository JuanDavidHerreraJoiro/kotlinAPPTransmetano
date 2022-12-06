package com.transmetano.ar.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EntityLayer {

    // token
    private static final String PREFS_TOKEN = "LoginPreferences";
    private static final String TOKEN_PREFS = "token";

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("url")
    @Expose
    private String url;

    public EntityLayer(String name, String type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url + "/0";
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
