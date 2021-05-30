package com.it123p.washkolang.ui.createwash;

public interface ResultHandler<T> {
    void onSuccess(T data);
    void onFailure(Exception e);

    T onSuccess();
}
