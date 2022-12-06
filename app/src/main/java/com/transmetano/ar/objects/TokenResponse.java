package com.transmetano.ar.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TokenResponse {

    @SerializedName("token")
    @Expose
    private String token;

    //@NonNull
    @Override
    public String toString() {
        if (this.token != null) {
            return "{" +
                    " token: " + this.token +
                    " expires: " + this.expires +
                    " ssl: " + this.ssl +
                    "}";
        }
        return "null";
    }

    @SerializedName("expires")
    @Expose
    private Long expires;
    @SerializedName("ssl")
    @Expose
    private Boolean ssl;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }

    public Boolean getSsl() {
        return ssl;
    }

    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }

}
