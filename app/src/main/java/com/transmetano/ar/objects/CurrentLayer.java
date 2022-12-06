package com.transmetano.ar.objects;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.transmetano.ar.R;

public class CurrentLayer {

    private static EntityLayer entityLayer;

    private static final String PREFS = "LayerPreferences";
    private static final String LAYER_PREFS = "LastLayer";

    private static int maxRange = 500;
    private static int range = 200;
    private static int heading = 0;
    private static int altitude = 0;
    private static float baseSurface = 0;

    public static EntityLayer getCurrent(Context context) {
        if (entityLayer == null) {
            SharedPreferences mPrefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
            String layerPref = mPrefs.getString(LAYER_PREFS, "");
            if (!layerPref.isEmpty()) {
                entityLayer = new Gson().fromJson(layerPref, EntityLayer.class);
            } else {
                entityLayer = new EntityLayer(
                        context.getString(R.string.layer_name),
                        context.getString(R.string.layer_type),
                        context.getString(R.string.layer_url));
            }
        }
        return entityLayer;
    }

    public static void setCurrent(EntityLayer entityLayer) {
        CurrentLayer.entityLayer = entityLayer;
    }

    public static int getMaxRange() {
        return maxRange;
    }

    public static void setMaxRange(int maxRange) {
        CurrentLayer.maxRange = maxRange;
    }

    public static void setRange(int range) {
        CurrentLayer.range = range;
    }

    public static int getRange() {
        return range;
    }

    public static int getHeading() {
        return heading;
    }

    public static void setHeading(int heading) {
        CurrentLayer.heading = heading;
    }

    public static int getAltitude() {
        return altitude;
    }

    public static void setAltitude(int altitude) {
        CurrentLayer.altitude = altitude;
    }

    public static float getBaseSurface() {
        return baseSurface;
    }

    public static void setBaseSurface(float baseSurface) {
        CurrentLayer.baseSurface = baseSurface;
    }

}
