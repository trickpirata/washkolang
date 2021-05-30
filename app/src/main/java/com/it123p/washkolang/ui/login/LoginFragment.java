package com.it123p.washkolang.ui.login;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.it123p.washkolang.MainActivity;
import com.it123p.washkolang.utils.*;
import com.it123p.washkolang.model.UserInfo;

import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.LoginActivity;
import com.it123p.washkolang.R;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

public class LoginFragment extends Fragment {

    private static final String TAG = "FacebookLogin";

    //UI
    private LoginButton loginButton;
    ProgressDialog progress;

    private LoginViewModel mViewModel;
    private CallbackManager callbackManager;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private static final String EMAIL = "public_profile";


    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        // TODO: Use the ViewModel

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference();

        progress = new ProgressDialog(getActivity());
        loginButton = (LoginButton) view.findViewById(R.id.btnFacebookLogin);
        loginButton.setPermissions(Arrays.asList("email", "public_profile"));
        loginButton.setFragment(this);
        callbackManager = CallbackManager.Factory.create();

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.e("Facebook" , exception.getLocalizedMessage());
            }
        });

        boolean loggedOut = AccessToken.getCurrentAccessToken() == null;

        if (!loggedOut) {
            progress.show();
            getUserProfile(AccessToken.getCurrentAccessToken());
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (currentAccessToken == null && currentUser != null) {
            currentUser.delete();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, UI will update with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            Toast.makeText(getActivity(), "Authentication Succeeded.", Toast.LENGTH_SHORT).show();
                            getUserProfile(token);
                        } else {
                            // If sign-in fails, a message will display to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(getActivity(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getUserProfile(AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken, new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.d(TAG, object.toString());
                        saveToDB(object);
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "first_name,last_name,email,id");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void saveToDB(JSONObject object) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        DatabaseReference databaseUser = mDatabase.child("users").child(mAuth.getUid());

        databaseUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    //we're new
                    addUser(object, "customer");
                } else { //user is present
                    addUser(object, (String) snapshot.child("type").getValue());
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }
                    goToMain();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void goToMain() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
    }

    private void addUser(JSONObject object, String currentType) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        try {
            UserInfo user = new UserInfo();
            user.authId = firebaseUser.getUid();
            user.facebookId = object.getString("id");;
            user.email = object.getString("email");
            user.firstName = object.getString("first_name");
            user.lastName = object.getString("last_name");
            String image_url = "https://graph.facebook.com/" + user.facebookId  + "/picture?type=normal";
            user.profileUrl = image_url;
            user.type = currentType;

            user.isOnline = true;
            mDatabase.child("users").child(user.authId).setValue(user);
        } catch (JSONException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        if (progress.isShowing()) {
            progress.dismiss();
        }
        goToMain();
    }
}