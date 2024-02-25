package com.dekankilic.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentEvent {
    private CustomerOrder order;
    private String type;
}
