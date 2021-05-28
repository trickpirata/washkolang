package com.it123p.washkolang;

import android.location.Location;

import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;

public interface LocationListener {
    public void didUpdateLocation(Location location);
}
