package com.it123p.washkolang;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.it123p.washkolang.ui.createwash.CreateWashFragment;
import com.it123p.washkolang.ui.login.LoginFragment;

public class CreateWashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wash);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CreateWashFragment.newInstance())
                    .commitNow();
        }
    }
}