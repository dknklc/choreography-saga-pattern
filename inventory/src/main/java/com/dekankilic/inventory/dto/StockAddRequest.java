package com.dekankilic.inventory.dto;

import lombok.Data;

@Data
public class StockAddRequest {
    private String item;
    private int quantity;
}
