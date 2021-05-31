package com.it123p.washkolang.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.it123p.washkolang.model.UserInfo;

public class UserSingleton {
    UserInfo currentUser = null;

    private static final UserSingleton instance = new UserSingleton();
    private static final String PREF = "WASHKOLANGPREF";
    public static UserSingleton getInstance() {
        return instance;
    }
    private UserSingleton() { }

    public void setCurrentUser(UserInfo user) {
        this.currentUser = user;
    }
    public UserInfo getCurrentUser() {
        return currentUser;
    }

    public void setCurrentOrderId(String orderId, Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        if(orderId == null) {
            editor.remove("order");
            return;
        }

        editor.putString("order", orderId);
        editor.commit();

    }

    public String getCurrentOrderId(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("order", "");
    }

}
