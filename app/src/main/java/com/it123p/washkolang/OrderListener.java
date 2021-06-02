package com.it123p.washkolang;

import com.it123p.washkolang.model.OrderInfo;

public interface OrderListener {
    public void didCreateOrder(OrderInfo order);
}