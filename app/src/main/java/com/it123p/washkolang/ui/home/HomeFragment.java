package com.it123p.washkolang.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.R;
import com.it123p.washkolang.ui.createwash.ResultHandler;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.LocationListener;
import com.it123p.washkolang.utils.UserSingleton;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback, LocationListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private HomeViewModel homeViewModel;

    public LocationListener listener;
    private Geocoder geocoder;
    private GoogleMap mMap;
    private int PERMISSION_ID = 44;
    private MarkerOptions userMarker = new MarkerOptions();
    private boolean runOnce = false;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        if(!checkPermissions()) {
            requestPermissions();
        } else {
            startMap();
        }
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        listener = this;
    }

    // Get a handle to the GoogleMap object and display marker.
    @Override
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        if (mMap == null) {
            mMap = googleMap;
        }
        googleMap.setMyLocationEnabled(true);
        if (homeViewModel.lastKnownLocation != null) {
            updateUserLocation(homeViewModel.lastKnownLocation);
        }
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                updateMarker(latLng);
            }
        });
    }

    private void startMap() {
        geocoder = new Geocoder(getContext(), Locale.getDefault());
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(this);
    }

    // method to check for permissions
    private boolean checkPermissions() {

        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_ID);
    }

    // If everything is alright then
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMap();
            }
        }
    }


    @Override
    public void didUpdateLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        updateMarker(new LatLng(lat, lon));
        homeViewModel.saveUserLocation(location, getAddress(location));
        if (!runOnce) {
            displayOperators();
        }

    }

    public void updateUserLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();


        updateMarker(new LatLng(lat, lon));
        //Get Address
        homeViewModel.saveUserLocation(location, getAddress(location));
    }

    public Location getSelectedLocation() {
        return homeViewModel.lastKnownLocation;
    }

    public String getSelectedAddress() {
        return homeViewModel.lastKnownAddress;
    }

    private void updateMarker(LatLng latLng) {
        if (mMap == null) {
            return;
        }

        userMarker.position(latLng);

        mMap.addMarker(userMarker);
    }

    private String getAddress(Location location) {
        String strAdd = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HomeFragment", "Cannot get Address!");
        }
        return strAdd;
    }

    private void displayOperators() {
        runOnce = true;
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(homeViewModel.lastKnownLocation.getLatitude(), homeViewModel.lastKnownLocation.getLongitude()),
                10);
        mMap.moveCamera(update);
        homeViewModel.getMapInfo(homeViewModel.lastKnownLocation, new ResultHandler<MapLocationData>() {
            @Override
            public void onSuccess(MapLocationData data) {
                //TODO: display geolocation
                //display other data
                if(!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(data.owner)) {
                    MarkerOptions marker = new MarkerOptions();

                    marker.position(new LatLng(data.location.latitude, data.location.longitude));

                    mMap.addMarker(marker);
                }

            }

            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public MapLocationData onSuccess() {
                return null;
            }
        });
    }

}