package com.it123p.washkolang.ui.home;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.core.GeoHash;
import com.google.firebase.FirebaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.model.UserInfo;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.utils.UserSingleton;
import com.it123p.washkolang.ui.createwash.ResultHandler;

public class HomeViewModel extends ViewModel {

    public Location lastKnownLocation;
    public String lastKnownAddress;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GeoQuery geoQuery;

    public HomeViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();
    }

    public void saveUserLocation(Location location, String address) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        mDatabase.child("users").child(firebaseUser.getUid()).child("lat").setValue(location.getLatitude());
        mDatabase.child("users").child(firebaseUser.getUid()).child("long").setValue(location.getLongitude());

        if(UserSingleton.getInstance().getCurrentUser() != null) {
            mDatabase.child("location").child(firebaseUser.getUid()).child("type").setValue(UserSingleton.getInstance().getCurrentUser().type);
        }

        mDatabase.child("location").child(firebaseUser.getUid()).child("lat").setValue(location.getLatitude());
        mDatabase.child("location").child(firebaseUser.getUid()).child("long").setValue(location.getLongitude());

        DatabaseReference ref = mDatabase.child("area");
        GeoFire geoFire = new GeoFire(ref);
        GeoLocation geoLocation = new GeoLocation(location.getLatitude(), location.getLongitude());

        geoFire.setLocation(firebaseUser.getUid(), geoLocation, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
//                    mDatabase.child("location").child(firebaseUser.getUid()).child("geohash").setValue(geoHash.getGeoHashString());
                }
            }
        });
        lastKnownLocation = location;
        lastKnownAddress = address;


    }

    public void getMapInfo(Location currentLocation, ResultHandler<MapLocationData> handler) {
        GeoLocation geoLocation = new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude());
        if(geoQuery == null) {
            DatabaseReference ref = mDatabase.child("area");
            GeoFire geoFire = new GeoFire(ref);
            geoQuery = geoFire.queryAtLocation(geoLocation, 10); //10kilometers
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    //System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
                    handler.onSuccess(new MapLocationData(location, key));
                }

                @Override
                public void onKeyExited(String key) {
                    System.out.println(String.format("Key %s is no longer in the search area", key));
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
                }

                @Override
                public void onGeoQueryReady() {
                    System.out.println("All initial data has been loaded and events have been fired!");
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    System.err.println("There was an error with this query: " + error);
                }
            });
        }

    }
}