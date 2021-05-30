package com.it123p.washkolang.ui.home;

import android.webkit.GeolocationPermissions;

import com.firebase.geofire.GeoLocation;

public class MapLocationData {
    public GeoLocation location;
    public String owner;

    public MapLocationData(GeoLocation location, String key) {
        this.location = location;
        this.owner = key;
    }
}
