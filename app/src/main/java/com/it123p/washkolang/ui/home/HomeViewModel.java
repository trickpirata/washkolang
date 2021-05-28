package com.it123p.washkolang.ui.home;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.utils.Constants;

public class HomeViewModel extends ViewModel {

    public Location lastKnownLocation;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    public HomeViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();
    }

    public void saveUserLocation(Location location) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        mDatabase.child("users").child(firebaseUser.getUid()).child("lat").setValue(location.getLatitude());
        mDatabase.child("users").child(firebaseUser.getUid()).child("long").setValue(location.getLongitude());

        lastKnownLocation = location;
    }
}