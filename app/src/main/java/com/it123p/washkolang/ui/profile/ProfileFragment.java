package com.it123p.washkolang.ui.profile;

import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.type.DateTime;
import com.it123p.washkolang.R;
import com.it123p.washkolang.model.UserInfo;
import com.it123p.washkolang.ui.createwash.ResultHandler;
import com.it123p.washkolang.utils.UserSingleton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ProfileFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private ProfileViewModel mViewModel;
    private String selectedGender = "Rather not say";
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }
    private UserInfo currentProfile = new UserInfo();
    private final Calendar myCalendar = Calendar.getInstance();
    private EditText txtBirthday;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // TODO: Use the ViewModel
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI(view);
    }


    private void setupUI(View view) {
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        mViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        EditText txtFirstname = view.findViewById(R.id.txtFirstName);
        EditText txtLastname = view.findViewById(R.id.txtLastName);
        EditText txtEmail = view.findViewById(R.id.txtEmail);
        EditText txtNumber = view.findViewById(R.id.txtPhoneNumber);
        txtBirthday = view.findViewById(R.id.txtBirthday);
        Spinner spin = view.findViewById(R.id.spinGender);
        Button button = view.findViewById(R.id.btnUpdateProfile);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mViewModel.genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(this);

        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setTitle("Please wait.");

        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                myCalendar.set(Calendar.YEAR, i);
                myCalendar.set(Calendar.MONTH, i1);
                myCalendar.set(Calendar.DAY_OF_MONTH, i2);
                updateLabel();
            }
        };

        txtBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(getContext(), date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserInfo userInfo = currentProfile;
                userInfo.firstName = txtFirstname.getText().toString();
                userInfo.lastName = txtLastname.getText().toString();
                userInfo.email = txtEmail.getText().toString();

                String strDate = txtBirthday.getText().toString();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date date = dateFormat.parse(strDate);
                    userInfo.birthdate = date.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                userInfo.phonenumber = txtNumber.getText().toString();
                userInfo.gender = selectedGender;
                updateUser(userInfo);
            }
        });

        mViewModel.getUserInfo(FirebaseAuth.getInstance().getCurrentUser().getUid(), new ResultHandler<UserInfo>() {
            @Override
            public void onSuccess(UserInfo data) {
                progress.dismiss();

                txtFirstname.setText(data.firstName);
                txtLastname.setText(data.lastName);
                txtEmail.setText(data.email);
                if(data.birthdate > 0) {
                    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy");
                    txtBirthday.setText(format.format(new Date(data.birthdate)));
                }

                txtNumber.setText(data.phonenumber);
                int spinnerPosition = adapter.getPosition(data.gender);
                if(spinnerPosition == -1 ) {
                    spinnerPosition = mViewModel.genders.length - 1;
                    selectedGender = mViewModel.genders[mViewModel.genders.length - 1];
                }
                spin.setSelection(spinnerPosition);
                currentProfile = data;
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        selectedGender = mViewModel.genders[i];
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void updateLabel() {
        String myFormat = "MM/dd/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        txtBirthday.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateUser(UserInfo info) {
        mViewModel.updateUserInfo(FirebaseAuth.getInstance().getCurrentUser().getUid(), info, new ResultHandler<UserInfo>() {
            @Override
            public void onSuccess(UserInfo data) {

            }

            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public UserInfo onSuccess() {
                showSuccess();
                return null;
            }
        });
    }

    private void showSuccess() {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();

        alertDialog.setTitle("Success!");

        alertDialog.setMessage("Profile Updated!");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        alertDialog.show();
    }
}