package com.dekankilic.payment.service;

import com.dekankilic.payment.dto.CustomerOrder;
import com.dekankilic.payment.dto.InventoryEvent;
import com.dekankilic.payment.dto.PaymentEvent;
import com.dekankilic.payment.model.Payment;
import com.dekankilic.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate kafkaTemplateCompleted;
    private final KafkaTemplate kafkaTemplateFailed;

    @KafkaListener(topics = "prod.inventory.updated", groupId = "payment-group")
    public void handleInventoryEvent(InventoryEvent inventoryEvent){
        CustomerOrder customerOrder = inventoryEvent.getOrder();
        Payment payment = Payment.builder()
                .amount(customerOrder.getAmount())
                .mode(customerOrder.getPaymentMethod())
                .orderId(customerOrder.getOrderId())
                .status("PAYMENT_COMPLETED")
                .build();

        try {
            paymentRepository.save(payment);

            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .order(customerOrder)
                    .type("PAYMENT_COMPLETED")
                    .build();
            kafkaTemplateCompleted.send("prod.payment.completed", String.valueOf(customerOrder.getOrderId()), paymentEvent);
        } catch (Exception ex){
            payment.setOrderId(customerOrder.getOrderId());
            payment.setStatus("PAYMENT_FAILED");
            paymentRepository.save(payment);

            InventoryEvent invEvent = InventoryEvent.builder()
                    .order(customerOrder)
                    .type("PAYMENT_FAILED")
                    .build();
            kafkaTemplateFailed.send("prod.payment.failed", String.valueOf(customerOrder.getOrderId()), invEvent);
        }
    }

    @KafkaListener(topics = "prod.delivery.failed", groupId = "payment-group")
    public void handlePaymentEvent(PaymentEvent paymentEvent){
        System.out.println("Reverse inventory event : " + paymentEvent);
        CustomerOrder customerOrder = paymentEvent.getOrder();

        try{
            Optional<Payment> payment = paymentRepository.findByOrderId(customerOrder.getOrderId());
            payment.ifPresent(p -> {
                p.setStatus("PAYMENT_CANCELED");
                paymentRepository.save(p);
            });
        }catch (Exception ex){
            System.out.println("Exception occured while reverting order details");
        }
    }
}
