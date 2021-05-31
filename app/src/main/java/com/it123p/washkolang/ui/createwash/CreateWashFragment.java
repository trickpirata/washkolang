package com.it123p.washkolang.ui.createwash;

import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.it123p.washkolang.R;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.ui.home.MapLocationData;
import com.it123p.washkolang.utils.Constants;
import com.it123p.washkolang.utils.UserSingleton;

public class CreateWashFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "CreateWashFragment";

    private CreateWashViewModel mViewModel;

    private String selectedSize = "Small";

    public static CreateWashFragment newInstance() {
        return new CreateWashFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.create_wash_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CreateWashViewModel.class);

        Spinner spin = (Spinner) view.findViewById(R.id.spinCarSize);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mViewModel.carSize);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(this);

        Button btnBook = (Button) view.findViewById(R.id.btnBook);
        btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createOrder();
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
//        Toast.makeText(getContext(), "Selected Car: "+ mViewModel.carSize[position] ,Toast.LENGTH_SHORT).show();
        selectedSize = mViewModel.carSize[position];
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO - Custom Code
    }

    private void createOrder() {

        Location location = getActivity().getIntent().getParcelableExtra("location");
        String address = getActivity().getIntent().getExtras().getString("address");
        //Get values
        EditText txtPlate = (EditText) getView().findViewById(R.id.txtPlateNumber);
        EditText txtCarMake = (EditText) getView().findViewById(R.id.txtCarMake);
        EditText txtCarModel = (EditText) getView().findViewById(R.id.txtCarModel);
        EditText txtCarColor = (EditText) getView().findViewById(R.id.txtCarColor);

        OrderInfo order = new OrderInfo();
        order.plateNumber = txtPlate.getText().toString();
        order.carMake = txtCarMake.getText().toString();
        order.carModel = txtCarModel.getText().toString();
        order.carColor = txtCarColor.getText().toString();
        order.carSize = selectedSize;
        order.latitude = location.getLatitude();
        order.longitude = location.getLongitude();
        order.address = address;

        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setTitle("Please wait.");
        progress.show();
        mViewModel.createOrder(order, new ResultHandler<OrderInfo>() {
            @Override
            public void onSuccess(OrderInfo data) {
                UserSingleton.getInstance().setCurrentOrderId(data.orderId, getContext());
                progress.dismiss();
//                getActivity().finish();
            }

            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public OrderInfo onSuccess() {
                return null;
            }
        });
        Log.e("Selected Location" , location.toString());
    }



}