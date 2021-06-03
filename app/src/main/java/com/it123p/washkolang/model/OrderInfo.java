package com.it123p.washkolang.model;

public class OrderInfo {
    public String plateNumber;
    public String carMake;
    public String carModel;
    public String carColor;
    public String carSize;
    public String author;
    public String operator;
    public double latitude;
    public double longitude;
    public String address;
    public String status;
    public String orderId;
    public String price;
    public long date;
    public OrderInfo() {

    }

    @Override
    public String toString() {
//        return "OrderInfo{" +
//                "plateNumber='" + plateNumber + '\'' +
//                ", carMake='" + carMake + '\'' +
//                ", carModel='" + carModel + '\'' +
//                ", carColor='" + carColor + '\'' +
//                ", carSize='" + carSize + '\'' +
//                ", author='" + author + '\'' +
//                ", operator='" + operator + '\'' +
//                ", latitude=" + latitude +
//                ", longitude=" + longitude +
//                ", address='" + address + '\'' +
//                ", status='" + status + '\'' +
//                ", orderId='" + orderId + '\'' +
//                ", price='" + price + '\'' +
//                ", date=" + date +
//                '}';
        return "Order for " + carMake;
    }

    public String information() {
        return  "Plate Number: " + plateNumber + '\n' +
                "Car Make: " + carMake + '\n' +
                "Model: " + carModel + '\n' +
                "Color: " + carColor + '\n' +
                "Size: " + carSize + '\n' +
                "Address: " + address + '\n' +
                "Status: " + status + '\n' +
                "Price: " + price + '\n';
//                ", latitude=" + latitude +
//                ", longitude=" + longitude +
//                ", address='" + address + '\'' +
//                ", status='" + status + '\'' +
//                ", orderId='" + orderId + '\'' +
//                ", price='" + price + '\'' +
//                ", date=" + date +;
    }
}
