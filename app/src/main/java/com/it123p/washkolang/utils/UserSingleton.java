package com.it123p.washkolang.utils;

import com.it123p.washkolang.model.UserInfo;

public class UserSingleton {
    UserInfo currentUser = null;

    private static final UserSingleton instance = new UserSingleton();

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

}
