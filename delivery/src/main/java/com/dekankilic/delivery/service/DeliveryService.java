package com.dekankilic.delivery.service;

import com.dekankilic.delivery.dto.CustomerOrder;
import com.dekankilic.delivery.dto.DeliveryEvent;
import com.dekankilic.delivery.dto.PaymentEvent;
import com.dekankilic.delivery.model.Delivery;
import com.dekankilic.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final KafkaTemplate kafkaTemplateCompleted;
    private final KafkaTemplate kafkaTemplateFailed;

    @KafkaListener(topics = "prod.payment.completed", groupId = "delivery-group")
    public void handlePaymentEvent(PaymentEvent paymentEvent){
        CustomerOrder customerOrder = paymentEvent.getOrder();
        Delivery delivery = Delivery.builder()
                .address(customerOrder.getAddress())
                .orderId(customerOrder.getOrderId())
                .status("DELIVERY_COMPLETED")
                .build();

        try {
            deliveryRepository.save(delivery);

            DeliveryEvent deliveryEvent = DeliveryEvent.builder()
                    .order(customerOrder)
                    .type("DELIVERY_COMPLETED")
                    .build();
            kafkaTemplateCompleted.send("prod.delivery.completed", String.valueOf(customerOrder.getOrderId()), deliveryEvent);
        } catch (Exception ex){
            delivery.setOrderId(customerOrder.getOrderId());
            delivery.setStatus("DELIVERY_FAILED");
            deliveryRepository.save(delivery);

            PaymentEvent payEvent = PaymentEvent.builder()
                    .order(customerOrder)
                    .type("DELIVERY_FAILED")
                    .build();
            kafkaTemplateFailed.send("prod.delivery.failed", String.valueOf(customerOrder.getOrderId()), payEvent);
        }
    }

}
