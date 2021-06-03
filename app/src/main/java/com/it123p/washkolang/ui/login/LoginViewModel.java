package com.it123p.washkolang.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.it123p.washkolang.model.UserInfo;
import com.it123p.washkolang.ui.createwash.ResultHandler;
import com.it123p.washkolang.utils.Constants;

public class LoginViewModel extends ViewModel {


    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


    public LoginViewModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();
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

    public void updateUserInfo(String userId, UserInfo userInfo, ResultHandler<UserInfo> handler) {
        mDatabase.child("users").child(userId).setValue(userInfo).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                handler.onSuccess();
            }
        });
    }
}