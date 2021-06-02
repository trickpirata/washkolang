package com.it123p.washkolang.ui.createwash;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.type.DateTime;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.ui.home.MapLocationData;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.utils.UserSingleton;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import javax.xml.transform.Result;

public class CreateWashViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    public String[] carSize = { "Small", "Medium", "Large", "XL"};

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser firebaseUser;
    private GeoQuery geoQuery;

    public CreateWashViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();
        firebaseUser = mAuth.getCurrentUser();
    }

    public void createOrder(OrderInfo order, ResultHandler<OrderInfo> handler) {
        FirebaseUser user = mAuth.getCurrentUser();
        order.author = user.getUid();
        order.status = "created";
        order.operator = "";

        DatabaseReference orderPush = mDatabase.child("orders").push();
        mDatabase.child("users").child(user.getUid()).child("orders").push().setValue(orderPush.getKey());

        orderPush.setValue(order).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                order.orderId = orderPush.getKey();
                handler.onSuccess(order);
                pushToOperators(order, orderPush.getKey());

            }
        });
    }

    private void pushToOperators(OrderInfo order, String orderId) {
        GeoLocation geoLocation = new GeoLocation(order.latitude, order.longitude);
        if(geoQuery == null) {
            DatabaseReference ref = mDatabase.child("area");
            GeoFire geoFire = new GeoFire(ref);
            GeoQuery geoQuery = geoFire.queryAtLocation(geoLocation, 10); //10kilometers
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    getFirebaseData(orderId, key);
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

    private void getFirebaseData(String orderId, String key) {
        //TODO: get token for notif
        DatabaseReference ref = mDatabase.child("users");
        mDatabase.child("users").child(key).child("token");

        mDatabase.child("users").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String token = (String) snapshot.child("token").getValue();

                //PUSH
                if (((String) snapshot.child("type").getValue()).equals("operator")) {
                    sendPushNotification("Someone is asking for a quick wash!", "Order", token, orderId);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    public void sendPushNotification(final String body, final String title, final String fcmToken, final String orderId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject json = new JSONObject();
                    JSONObject notificationJson = new JSONObject();
                    JSONObject dataJson = new JSONObject();
                    notificationJson.put("text", body);
                    notificationJson.put("title", title);
                    notificationJson.put("priority", "high");
                    dataJson.put("orderId", orderId);
                    dataJson.put("badge", 1);
                    dataJson.put("alert", "Alert");
                    json.put("notification", notificationJson);
                    json.put("data", dataJson);
                    json.put("to", fcmToken);
                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString());
                    Request request = new Request.Builder()
                            .header("Authorization", "key=AAAAUJHRkHE:APA91bGDK5r8yw_tPt5ro9rdj1yIXm8z8AEpcpq7AGd3gOJK8ZMJ0qjVBeLyo0_57iWhhnpYGLP6j3rL2xA7SDmW5GdaXK9GQM8W2mSl8aG_yjOFWKjUXIgLxXdnmZOhJUBnSAFw-hLg")
                            .url("https://fcm.googleapis.com/fcm/send")
                            .post(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    String finalResponse = response.body().string();
//                    Log.i("TAG", finalResponse);
                } catch (Exception e) {

//                    Log.i("TAG", e.getMessage());
                }
                return null;
            }
        }.execute();
    }
}