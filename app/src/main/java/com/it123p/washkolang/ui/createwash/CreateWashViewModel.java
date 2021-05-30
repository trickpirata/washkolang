package com.it123p.washkolang.ui.createwash;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.utils.Constants;

import javax.xml.transform.Result;

public class CreateWashViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    public String[] carSize = { "Small", "Medium", "Large", "XL"};

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser firebaseUser;
    public CreateWashViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();
        firebaseUser = mAuth.getCurrentUser();
    }

    public void createOrder(OrderInfo order, ResultHandler<Void> handler) {
        FirebaseUser user = mAuth.getCurrentUser();
        order.author = user.getUid();
        order.status = "created";

        mDatabase.child("orders").push().setValue(order).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                handler.onSuccess();
            }
        });
    }

    private void fetchNearby() {
        DatabaseReference locations = mDatabase.child("location");

    }
}