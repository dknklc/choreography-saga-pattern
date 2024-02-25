package com.dekankilic.payment.dto;

import lombok.Data;

@Data
public class CustomerOrder {
    private String item;
    private int quantity;
    private double amount;
    private String paymentMethod;
    private long orderId;
    private String address;
}
