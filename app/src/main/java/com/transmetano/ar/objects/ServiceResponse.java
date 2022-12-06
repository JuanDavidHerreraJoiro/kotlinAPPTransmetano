package com.transmetano.ar.objects;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ServiceResponse {

    @SerializedName("currentVersion")
    @Expose
    private Double currentVersion;
    @SerializedName("services")
    @Expose
    private List<EntityLayer> services = null;

    public Double getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Double currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<EntityLayer> getServices() {
        return services;
    }

    public void setServices(List<EntityLayer> services) {
        this.services = services;
    }

}