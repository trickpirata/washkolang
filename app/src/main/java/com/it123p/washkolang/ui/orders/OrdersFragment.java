package com.it123p.washkolang.ui.orders;

import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.it123p.washkolang.R;
import com.it123p.washkolang.model.OrderInfo;
import com.it123p.washkolang.ui.createwash.ResultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class OrdersFragment extends Fragment {

    private OrdersViewModel mViewModel;
    private ListView listView;
    private TextView textView;
    private ArrayList<OrderInfo> orders = new ArrayList<>();
    public static OrdersFragment newInstance() {
        return new OrdersFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.orders_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        ListView listView = view.findViewById(R.id.listView);
        textView = view.findViewById(R.id.textView);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        mViewModel.getOrders(FirebaseAuth.getInstance().getCurrentUser().getUid(), new ResultHandler<OrderInfo>() {
            @Override
            public void onSuccess(OrderInfo data) {
                orders.add(data);
//                ArrayAdapter<OrderInfo> ad = new ArrayAdapter<OrderInfo> (getContext(), android.R.layout.simple_list_item_1,  android.R.id.text1, orders);  // pass List to ArrayAdapter

                ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_2, android.R.id.text1, orders) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text1 =  view.findViewById(android.R.id.text1);
                        TextView text2 =  view.findViewById(android.R.id.text2);

                        text1.setText(orders.get(position).toString());
                        text2.setText(dateFormat.format(new Date(orders.get(position).date)));

                        return view;
                    }
                };

                listView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public OrderInfo onSuccess() {
                return null;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                OrderInfo info = orders.get(i);
                showOrderInfo(info);
            }
        });
    }

    private void showOrderInfo(OrderInfo info) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();

        alertDialog.setTitle("Order Information");

        alertDialog.setMessage(info.information());

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        alertDialog.show();
    }
}