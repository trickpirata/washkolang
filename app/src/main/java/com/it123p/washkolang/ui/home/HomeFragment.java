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
import android.graphics.Color;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.GoogleApiClient;
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

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.auth.User;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.it123p.washkolang.MainActivity;
import com.it123p.washkolang.OrderListener;
import com.it123p.washkolang.R;
import com.it123p.washkolang.model.MarkerInfo;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.model.UserInfo;
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
    private boolean isOperator = false;
    private ProgressDialog progressDialog;
    private boolean didClickCustomLocation = false;
    private boolean didUserCancel = false;
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
        progressDialog = new ProgressDialog(getContext());
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
//        UserSingleton.getInstance().setCurrentOrderId(null, getContext());
        Button btnCancel = (Button) getView().findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(this);
        didClickCustomLocation = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        listener = this;
        orderListener = this;
        String currentOrder = UserSingleton.getInstance().getCurrentOrderId(getContext());
        if(currentOrder != null && !currentOrder.equals("") && !isOperator) {
            ((MainActivity)getActivity()).hideFabButton(true);
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
                if(isOperator) {
                    return;
                }
                didClickCustomLocation = true;
                Location targetLocation = new Location("");
                targetLocation.setLatitude(latLng.latitude);
                targetLocation.setLongitude(latLng.longitude);
                getAddress(targetLocation);
                updateMarker(latLng);

                homeViewModel.saveUserLocation(targetLocation,  getAddress(targetLocation));
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
                didUserCancel = true;
                updateOrderInfo();
                break;
        }
    }

    @Override
    public void didUpdateLocation(Location location) {
        if(didClickCustomLocation) {
            return;
        }
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        homeViewModel.saveUserLocation(location, getAddress(location));
        if (!runOnce && FirebaseAuth.getInstance().getCurrentUser() != null) {
            homeViewModel.getUserInfo(FirebaseAuth.getInstance().getCurrentUser().getUid(), new ResultHandler<UserInfo>() {
                @Override
                public void onSuccess(UserInfo data) {
                    if(data.type != null ) {
                        if(data.type.equals("operator")) {
                            isOperator = true;
                        }
                    }
                    displayOperators();
                }

                @Override
                public void onFailure(Exception e) {

                }

                @Override
                public UserInfo onSuccess() {
                    return null;
                }
            });
        }

        if(runOnce) {
            updateMarker(new LatLng(lat, lon));
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

        addUpdateMarker(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latLng.latitude, latLng.longitude), false);
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
                addUpdateMarker(key, location, false);
            }

            @Override
            public void onKeyExited(String key) {
                if(!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(key)) {
                    removeMarker(key);
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                addUpdateMarker(key, location, false);
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void addUpdateMarker(String key, GeoLocation location, boolean force) {
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
            boolean continueAdding = true;
            MarkerOptions marker = new MarkerOptions();
            if(!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(key) && !isOperator) { //logged in user not an operator and scanned key is other user
                marker.icon(BitmapDescriptorFactory.fromBitmap(
                        createCustomMarker(getContext(),R.drawable.carpool,"")));
            } else if(FirebaseAuth.getInstance().getCurrentUser().getUid().equals(key) && isOperator) {
                marker.icon(BitmapDescriptorFactory.fromBitmap(
                        createCustomMarker(getContext(),R.drawable.carpool,"")));
            } else if (!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(key) && isOperator && !force) { //logged in user is an operator and scanned key is other user
                continueAdding = false;
            }

            if (continueAdding) {
                marker.position(new LatLng(location.latitude, location.longitude));
                MarkerInfo markerInfo = new MarkerInfo();
                markerInfo.marker = mMap.addMarker(marker);
                markerInfo.userId = key;

                markerList.add(markerInfo);
            }

        }
    }

    private void removeMarker(String key) {
        for(int k = 0; k < markerList.size(); k++){
            MarkerInfo marker = markerList.get(k);
            if(marker.userId.equals(key)) { //already added, remove
                marker.marker.remove();
                markerList.remove(marker);
                break;
            }
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

                homeViewModel.acceptOrder(order.orderId, new ResultHandler<Void>() {
                    @Override
                    public void onSuccess(Void data) {

                    }

                    @Override
                    public void onFailure(Exception e) {
                        showPrompts("Another operator is already assigned to this order.");
                    }

                    @Override
                    public Void onSuccess() {
                        order.operator = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        order.status = "accepted";
                        UserSingleton.getInstance().setCurrentOrderId(order.orderId, getContext());
                        loadOrder(order.orderId);
                        return null;
                    }
                });


            } });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Decline", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                homeViewModel.updateOrderStatus(order.orderId, "cancel", new ResultHandler<Void>() {
                    @Override
                    public void onSuccess(Void data) {

                    }

                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public Void onSuccess() {
                        return null;
                    }
                });

            }});

        alertDialog.show();
    }


    private void loadOrder(String orderId) { //load accepted order
        homeViewModel.monitorOrder(orderId, new ResultHandler<OrderInfo>() {
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
            //show prompts to user
            if(order.status.equals("accepted")) {
                showPrompts("An operator accepted your carwash request and is on its way! Stay put!");
            } else if(order.status.equals("arrived")) {
                showPrompts("An operator arrived at the pinned location");
            } else if(order.status.equals("finished")) {
                showPrompts("An operator finished washing your car! Thanks!");
                ((MainActivity)getActivity()).hideFabButton(false);
            } else if(order.status.equals("cancel") && !didUserCancel) {
                showPrompts("An operator canceled your request :(");
                ((MainActivity)getActivity()).hideFabButton(false);
            }
        } else {
            if (currentOrder == null) {
                progressDialog.show();
                LatLng start = new LatLng(homeViewModel.lastKnownLocation.getLatitude(), homeViewModel.lastKnownLocation.getLongitude());
                LatLng end = new LatLng(order.latitude, order.longitude);
                requestDirections(start, end);

                // Start marker
                MarkerOptions options = new MarkerOptions();
                options.position(end);

                MarkerInfo info = new MarkerInfo();
                info.userId = order.orderId;
                info.marker = mMap.addMarker(options);

                markerList.add(info);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 16));
            }
//            addUpdateMarker(order.orderId, new GeoLocation(order.latitude, order.longitude), true);
        }

        currentOrder = order;


        txtOrderStatus.setText("Order Status: " + order.status);
        txtCar.setText("Make: " + order.carMake);
        txtCarSize.setText("Size: " +order.carSize);
        txtPrice.setText("Price: " +"250");
        ConstraintLayout bottomLayout = (ConstraintLayout) getView().findViewById(R.id.orderLayout);
        if(isOperator) {
            if(order.status.equals("accepted")) {
                bottomLayout.setVisibility(View.VISIBLE);
                btnCancel.setText("Arrived");
            } else if(order.status.equals("arrived")) {
                bottomLayout.setVisibility(View.VISIBLE);
                btnCancel.setText("Finished");
            } else if(order.status.equals("finished")) {
                bottomLayout.setVisibility(View.INVISIBLE);
                UserSingleton.getInstance().setCurrentOrderId(null, getContext());
                currentOrder = null;
            } else if(order.status.equals("cancel")) {
                bottomLayout.setVisibility(View.INVISIBLE);
                UserSingleton.getInstance().setCurrentOrderId(null, getContext());
                currentOrder = null;
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
                didUserCancel = false;
                ((MainActivity)getActivity()).hideFabButton(false);
            }
        }
    }

    private void showPrompts(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();

        alertDialog.setTitle("Status Update");

        alertDialog.setMessage(message);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        alertDialog.show();
    }
    List<Polyline> polylines = new ArrayList<Polyline>();
    private void requestDirections(LatLng start, LatLng end) {

        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey("AIzaSyB0anEb0AXLH9U4BT20UBcHVbpmXndegec")
                .build();
        List<LatLng> path = new ArrayList();
        DirectionsApiRequest req = DirectionsApi.getDirections(context, Double.toString(start.latitude) + "," + Double.toString(start.longitude) , Double.toString(end.latitude) + "," + Double.toString(end.longitude));
        try {
            DirectionsResult res = req.await();
            //Loop through legs and steps to get encoded polylines of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];

                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                            for (com.google.maps.model.LatLng coord1 : coords1) {
                                                path.add(new LatLng(coord1.lat, coord1.lng));
                                            }
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //Draw the polyline
                if (path.size() > 0) {
                    PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
                    polylines.add(mMap.addPolyline(opts));
                    progressDialog.dismiss();
                }
            }
        } catch(Exception ex) {
            progressDialog.dismiss();
            Log.e("ERROR", ex.getLocalizedMessage());
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

//                         loadOrder(currentOrder.orderId);
                        return null;
                    }
                });
            } else if(currentOrder.status.equals("arrived")) {
                for(Polyline line : polylines)
                {
                    line.remove();
                }

                polylines.clear();
                removeMarker(currentOrder.orderId);
                homeViewModel.updateOrderStatus(currentOrder.orderId, "finished", new ResultHandler<Void>() {
                    @Override
                    public void onSuccess(Void data) {

                    }

                    @Override
                    public void onFailure(Exception e) {

                    }

                    @Override
                    public Void onSuccess() {

//                        loadOrder(currentOrder.orderId);

                        return null;
                    }
                });
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
//                    loadOrder(currentOrder.orderId);
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