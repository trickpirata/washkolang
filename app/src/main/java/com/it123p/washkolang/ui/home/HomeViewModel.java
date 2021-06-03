package com.it123p.washkolang.ui.home;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.it123p.washkolang.R;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.model.UserInfo;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.utils.UserSingleton;
import com.it123p.washkolang.ui.createwash.ResultHandler;

import de.hdodenhof.circleimageview.CircleImageView;

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
        if(firebaseUser == null) {
            return;
        }
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

    public void getUserInfo(String userId, ResultHandler<UserInfo> handler) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserInfo info = snapshot.getValue(UserInfo.class);
                handler.onSuccess(info);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getOrderInfo(String orderId, ResultHandler<OrderInfo> handler) {
        mDatabase.child("orders").child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    OrderInfo order = snapshot.getValue(OrderInfo.class);
                    order.orderId = snapshot.getKey();
                    handler.onSuccess(order);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void monitorOrder(String orderId, ResultHandler<OrderInfo> handler) {
        mDatabase.child("orders").child(orderId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                OrderInfo order = snapshot.getValue(OrderInfo.class);
                order.orderId = snapshot.getKey();
                handler.onSuccess(order);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void removeOrderChildListener(String orderId, ChildEventListener listener) {
        mDatabase.child("orders").child(orderId).removeEventListener(listener);
    }

    public void getMapInfo(Location currentLocation, GeoQueryEventListener handler) {
        GeoLocation geoLocation = new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude());
        if(geoQuery == null) {
            DatabaseReference ref = mDatabase.child("area");
            GeoFire geoFire = new GeoFire(ref);
            geoQuery = geoFire.queryAtLocation(geoLocation, 10); //10kilometers
            geoQuery.addGeoQueryEventListener(handler);
        }
    }

    public void declineOrder(String orderId) {
        mDatabase.child("orders").child(orderId).child("status").setValue("cancel"); //set to cancel
    }

    public void acceptOrder(String orderId, ResultHandler<Void> handler) {
        mDatabase.child("orders").child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    OrderInfo info = snapshot.getValue(OrderInfo.class);
                    if(info.status.equals("accepted")) {
                        handler.onFailure(null);

                    } else {
                        mDatabase.child("orders").child(orderId).child("operator").setValue(FirebaseAuth.getInstance().getCurrentUser().getUid()); //set order operator
                        mDatabase.child("orders").child(orderId).child("status").setValue("accepted"); //set to accept
                        mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("orders").push().setValue(orderId); //add to orderhistory
                        handler.onSuccess();

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void updateOrderStatus(String orderId, String status, ResultHandler<Void> handler) {
        mDatabase.child("orders").child(orderId).child("status").setValue(status).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                handler.onSuccess();
            }
        });
    }

    public static Bitmap createCustomMarker(Context context, @DrawableRes int resource, String _name) {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

        CircleImageView markerImage = (CircleImageView) marker.findViewById(R.id.user_dp);
        markerImage.setImageResource(resource);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);

        return bitmap;
    }

}