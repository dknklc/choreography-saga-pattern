package com.dekankilic.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class InventoryEvent {
    private CustomerOrder order;
    private String type;
}
