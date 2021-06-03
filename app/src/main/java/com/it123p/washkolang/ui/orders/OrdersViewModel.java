package com.it123p.washkolang.ui.orders;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.firebase.geofire.GeoQuery;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.ui.createwash.ResultHandler;
import com.it123p.washkolang.utils.Constants;

import java.util.ArrayList;

public class OrdersViewModel extends ViewModel {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser firebaseUser;

    public OrdersViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();
        firebaseUser = mAuth.getCurrentUser();
    }

    public void getOrders(String userId, ResultHandler<OrderInfo> handler) {
        mDatabase.child("users").child(userId).child("orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot val : snapshot.getChildren()) {
                    String orderId = val.getValue().toString();
                    if(orderId != null) {
                        //get order data
                        mDatabase.child("orders").child(orderId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                            @Override
                            public void onSuccess(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    OrderInfo info = dataSnapshot.getValue(OrderInfo.class);
                                    handler.onSuccess(info);
                                }
                            }
                        });
                    }

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}