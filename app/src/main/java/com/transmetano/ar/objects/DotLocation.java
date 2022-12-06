package com.transmetano.ar.objects;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DotLocation implements Serializable {

    // Latitud
    @SerializedName("x")
    final double lat;
    @SerializedName("y")
    final double lon;
    @SerializedName("z")
    final double alt;

    public DotLocation(double lat, double lon, double alt) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getAlt() {
        return alt;
    }
}

