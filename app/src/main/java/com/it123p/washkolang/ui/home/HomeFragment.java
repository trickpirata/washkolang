package com.it123p.washkolang.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.MainActivity;
import com.it123p.washkolang.OrderListener;
import com.it123p.washkolang.R;
import com.it123p.washkolang.model.MarkerInfo;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.ui.createwash.ResultHandler;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.LocationListener;
import com.it123p.washkolang.utils.UserSingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.it123p.washkolang.ui.home.HomeViewModel.createCustomMarker;

public class HomeFragment extends Fragment implements OnMapReadyCallback, LocationListener, ActivityCompat.OnRequestPermissionsResultCallback, View.OnClickListener, OrderListener {

    private HomeViewModel homeViewModel;

    public LocationListener listener;
    public OrderListener orderListener;
    private Geocoder geocoder;
    private GoogleMap mMap;
    private int PERMISSION_ID = 44;
    private MarkerOptions userMarker = new MarkerOptions();
    private boolean runOnce = false;
    private BroadcastReceiver receiver;
    private boolean isMapReady = false;
    private OrderInfo currentOrder = null;
    private ArrayList<MarkerInfo> markerList = new ArrayList<>();

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

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("ORDER_RECEIVED") && intent.getExtras().getString("json") != null) {
                    try {
                        JSONObject json = new JSONObject(intent.getExtras().getString("json"));

                        String orderId = json.getString("orderId");
                        receivedOrder(orderId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if (intent.getAction().equals("ORDER_CREATED") && intent.getExtras().getString("orderId") != null) {
                    String orderId = intent.getExtras().getString("orderId");
                    loadOrder(orderId);
                }
                else {
                    //Todo: Show message
                }

            }
        };

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, new IntentFilter("ORDER_RECEIVED"));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UserSingleton.getInstance().setCurrentOrderId(null, getContext());
        ConstraintLayout bottomLayout = (ConstraintLayout) view.findViewById(R.id.orderLayout);
        String currentOrder = UserSingleton.getInstance().getCurrentOrderId(getContext());
        if(currentOrder != null && !currentOrder.equals("")) {
            loadOrder(currentOrder);
        } else {
            bottomLayout.setVisibility(View.INVISIBLE);
        }
        Button btnCancel = (Button) getView().findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        listener = this;
        orderListener = this;
        String currentOrder = UserSingleton.getInstance().getCurrentOrderId(getContext());
        if(currentOrder != null && !currentOrder.equals("")) {
            loadOrder(currentOrder);
        }
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

        isMapReady = true;
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnCancel:
                updateOrderInfo();
                break;
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

        addUpdateMarker(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latLng.latitude, latLng.longitude));
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
        if (!isMapReady) {
            return;
        }
        runOnce = true;
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(homeViewModel.lastKnownLocation.getLatitude(), homeViewModel.lastKnownLocation.getLongitude()),
                10);
        mMap.moveCamera(update);
        homeViewModel.getMapInfo(homeViewModel.lastKnownLocation, new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                addUpdateMarker(key, location);
            }

            @Override
            public void onKeyExited(String key) {
                if(!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(key)) {
                    for(int k = 0; k < markerList.size(); k++){
                        MarkerInfo marker = markerList.get(k);
                        if(marker.userId.equals(key)) { //already added, remove
                            marker.marker.remove();
                            markerList.remove(marker);
                            break;
                        }
                    }

                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                addUpdateMarker(key, location);
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void addUpdateMarker(String key, GeoLocation location) {
        boolean addMarker = true;
        for(int k = 0; k < markerList.size(); k++){
            MarkerInfo marker = markerList.get(k);
            if(marker.userId.equals(key)) { //already added, update location
                //update marker
                marker.marker.setPosition(new LatLng(location.latitude, location.longitude));
                addMarker = false;
                break;
            }
        }
        if(addMarker) {
            MarkerOptions marker = new MarkerOptions();
            if(!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(key)) {
                marker.icon(BitmapDescriptorFactory.fromBitmap(
                        createCustomMarker(getContext(),R.drawable.carpool,"")));
            }

            marker.position(new LatLng(location.latitude, location.longitude));
            MarkerInfo markerInfo = new MarkerInfo();
            markerInfo.marker = mMap.addMarker(marker);
            markerInfo.userId = key;

            markerList.add(markerInfo);
        }
    }

    private void receivedOrder(String orderId) {
        //Retreive order via order info
        homeViewModel.getOrderInfo(orderId, new ResultHandler<OrderInfo>() {
            @Override
            public void onSuccess(OrderInfo data) {
                showAlert(data);
            }

            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public OrderInfo onSuccess() {
                return null;
            }
        });


    }

    private void showAlert(OrderInfo order) {
        if (currentOrder != null) {
            return;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();

        alertDialog.setTitle("Order Received!");

        alertDialog.setMessage("Details: \n" + order.carMake);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Accept", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                homeViewModel.acceptOrder(order.orderId);
                order.operator = FirebaseAuth.getInstance().getCurrentUser().getUid();
                order.status = "accepted";
                UserSingleton.getInstance().setCurrentOrderId(order.orderId, getContext());
                loadOrderDetails(order);

            } });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Decline", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                //...

            }});

        alertDialog.show();
    }

    private void loadOrder(String orderId) {
        homeViewModel.getOrderInfo(orderId, new ResultHandler<OrderInfo>() {
            @Override
            public void onSuccess(OrderInfo data) {
                loadOrderDetails(data);
            }

            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public OrderInfo onSuccess() {
                return null;
            }
        });
    }

    private void loadOrderDetails(OrderInfo order) {

        Button btnCancel = (Button) getView().findViewById(R.id.btnCancel);
        TextView txtOrderStatus = (TextView) getView().findViewById(R.id.txtOrderStatus);
        TextView txtCar = (TextView) getView().findViewById(R.id.txtCar);
        TextView txtCarSize = (TextView) getView().findViewById(R.id.txtCarSize);
        TextView txtPrice = (TextView) getView().findViewById(R.id.txtPrice);

        boolean isOperator = true;
        if(order.author.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            btnCancel.setText("Cancel");
            isOperator = false;
        }
        txtOrderStatus.setText("Order Status: " + order.status);
        txtCar.setText("Make: " + order.carMake);
        txtCarSize.setText("Size: " +order.carSize);
        txtPrice.setText("Price: " +"250");
        ConstraintLayout bottomLayout = (ConstraintLayout) getView().findViewById(R.id.orderLayout);
        currentOrder = order;
        if(isOperator) {
            if(order.status.equals("accepted")) {
                bottomLayout.setVisibility(View.VISIBLE);
                btnCancel.setText("Arrived");
            } else if(order.status.equals("arrived")) {
                bottomLayout.setVisibility(View.VISIBLE);
                btnCancel.setText("Finished");
            } else if(order.status.equals("finished")) {
                bottomLayout.setVisibility(View.INVISIBLE);
            }
        } else {
            if (order.status.equals("created")) {
                bottomLayout.setVisibility(View.VISIBLE);
                btnCancel.setText("Cancel");
            } else if (order.status.equals("finished")) {
                bottomLayout.setVisibility(View.INVISIBLE);
                UserSingleton.getInstance().setCurrentOrderId(null, getContext());
                currentOrder = null;
            } else if (order.status.equals("cancel")) {
                bottomLayout.setVisibility(View.INVISIBLE);


            }
        }
    }

    private void updateOrderInfo() {

        //We're the operator of this current order
        if(currentOrder.operator.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            if(currentOrder.status.equals("accepted")) {
                homeViewModel.updateOrderStatus(currentOrder.orderId, "arrived", new ResultHandler<Void>() {
                    @Override
                    public void onSuccess(Void data) {

                    }

                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public Void onSuccess() {
                         loadOrder(currentOrder.orderId);
                        return null;
                    }
                });
            } else if(currentOrder.status.equals("arrived")) {
                homeViewModel.updateOrderStatus(currentOrder.orderId, "finished", new ResultHandler<Void>() {
                    @Override
                    public void onSuccess(Void data) {

                    }

                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public Void onSuccess() {
                        loadOrder(currentOrder.orderId);
                        return null;
                    }
                });
                UserSingleton.getInstance().setCurrentOrderId(null, getContext());
                currentOrder = null;
            }
        } else { //we're the user
            homeViewModel.updateOrderStatus(currentOrder.orderId, "cancel", new ResultHandler<Void>() {
                @Override
                public void onSuccess(Void data) {

                }

                @Override
                public void onFailure(Exception e) {

                }

                @Override
                public Void onSuccess() {
                    loadOrder(currentOrder.orderId);
                    return null;
                }
            });
            UserSingleton.getInstance().setCurrentOrderId(null, getContext());
            currentOrder = null;
        }
    }

    @Override
    public void didCreateOrder(OrderInfo order) {
        loadOrder(order.orderId);
    }
}