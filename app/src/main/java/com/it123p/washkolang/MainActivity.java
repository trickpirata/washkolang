package com.it123p.washkolang;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.it123p.washkolang.ui.home.HomeFragment;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.model.UserInfo;
import com.it123p.washkolang.utils.UserSingleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

//import com.google.android.gms.


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;

    private FirebaseAuth mAuth;

    //Location
    private FusedLocationProviderClient mFusedLocationClient;
    private int PERMISSION_ID = 44;

    // UI
    private TextView txtName;
    private TextView txtEmail;
    private ProfilePictureView imgUser;
    private NavController navController;
    private FloatingActionButton btnCreateOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btnCreateOrder = findViewById(R.id.fab);
        btnCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToCreateWash();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        txtName = (TextView) headerView.findViewById(R.id.txtName);
        txtEmail = (TextView) headerView.findViewById(R.id.txtEmail);
        imgUser = (ProfilePictureView) headerView.findViewById(R.id.imageView);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_profile, R.id.nav_orders, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        setup();
        checkIfLoggedIn();
        UserSingleton.getInstance().setCurrentOrderId(null, MainActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void hideFabButton(boolean hide) {
        if(hide) {
            btnCreateOrder.setVisibility(View.INVISIBLE);
        } else {
            btnCreateOrder.setVisibility(View.VISIBLE);
        }
    }
    private void setup() {
        mAuth = FirebaseAuth.getInstance();
        //Setup map
        //for map
        setupLocation();
        monitorAvailabilty();
    }

    private void setupLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestNewLocationData();
    }

    private void checkIfLoggedIn() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Please wait.");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
            getFCMToken();
            progress.show();
            //Load user
            DatabaseReference users = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference("users");

            //get specific user
            users.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String fullName = (String) snapshot.child("firstName").getValue() + " " + (String) snapshot.child("lastName").getValue();
                    txtName.setText(fullName);
                    txtEmail.setText((String) snapshot.child("email").getValue());
                    imgUser.setProfileId((String) snapshot.child("facebookId").getValue());
                    if (snapshot.child("type").exists()) {
                        if (((String) snapshot.child("type").getValue()).equals("operator")) {
                            btnCreateOrder.setVisibility(View.INVISIBLE);
                        }
                    }

                    UserInfo userInfo = new UserInfo();
                    userInfo.firstName = (String) snapshot.child("firstName").getValue();
                    userInfo.lastName = (String) snapshot.child("lastName").getValue();

                    if(snapshot.child("type").exists()) {
                        String type = (String) snapshot.child("type").getValue();
                        userInfo.type = type;
                    }
                    UserSingleton.getInstance().setCurrentUser(snapshot.getValue(UserInfo.class));

                    progress.dismiss();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progress.dismiss();
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {

                            updateLocation(location);
//                            fragment.updateUserLocation(location);
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    private void monitorAvailabilty() {
        DatabaseReference database = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference("availability");

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    if(((String)snapshot.child("status").getValue()).equals("false")) {

                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

                        alertDialog.setTitle("Closed");

                        alertDialog.setMessage("Reason" + (String)snapshot.child("reason").getValue());

                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {

                                finishAffinity();

                            } });


                        alertDialog.show();

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        database.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {


            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()){
                    if(((String)snapshot.child("status").getValue()).equals("false")) {

                        AlertDialog alertDialog = new AlertDialog.Builder(getApplicationContext()).create();

                        alertDialog.setTitle("Closed");

                        alertDialog.setMessage("Reason" + (String)snapshot.child("reason").getValue());

                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {

                                finishAffinity();

                            } });


                        alertDialog.show();

                    }

                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();

            updateLocation(mLastLocation);
//            fragment.updateUserLocation(mLastLocation);
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    // If everything is alright then
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               getLastLocation();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        DatabaseReference users = FirebaseDatabase.getInstance(Constants.FIREBASE_DB_URL).getReference("users");
                        DatabaseReference databaseUser = users.child(mAuth.getUid());

                        databaseUser.child("token").setValue(token);
                        // Log and toast
//                        String msg = getString(R.string.msg_token_fmt, token);
//                        Log.d(TAG, msg);
//                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void updateLocation(Location location) {
        if(AccessToken.getCurrentAccessToken() == null) {
            return;
        }
        Fragment navHostFragment = getSupportFragmentManager().getPrimaryNavigationFragment();
        Fragment fragment = navHostFragment.getChildFragmentManager().getFragments().get(0);

        if (fragment != null && fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).listener.didUpdateLocation(location);
        }
    }

    private void goToCreateWash() {
        Fragment navHostFragment = getSupportFragmentManager().getPrimaryNavigationFragment();
        Fragment fragment = navHostFragment.getChildFragmentManager().getFragments().get(0);

        if (fragment != null && fragment instanceof HomeFragment) {
            HomeFragment homeFragment = ((HomeFragment) fragment);
            Intent intent = new Intent(this, CreateWashActivity.class);
            intent.putExtra("location", homeFragment.getSelectedLocation());
            intent.putExtra("address", homeFragment.getSelectedAddress());

            startActivity(intent);
        }
    }
}