package com.dekankilic.order.service;

import com.dekankilic.order.dto.CustomerOrder;
import com.dekankilic.order.dto.OrderPlacedEvent;
import com.dekankilic.order.model.Order;
import com.dekankilic.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public CustomerOrder createOrder(CustomerOrder customerOrder){
        Order order = Order.builder()
                .item(customerOrder.getItem())
                .quantity(customerOrder.getQuantity())
                .status("PENDING")
                .amount(customerOrder.getAmount())
                .build();

        try {
            order = orderRepository.save(order);
            customerOrder.setOrderId(order.getId());

            OrderPlacedEvent orderPlacedEvent = OrderPlacedEvent.builder()
                    .order(customerOrder)
                    .type("ORDER_CREATED")
                    .build();
            this.kafkaTemplate.send("prod.order.placed", String.valueOf(order.getId()), orderPlacedEvent);
            return customerOrder;

        } catch (Exception ex){
            order.setStatus("FAILED");
            orderRepository.save(order);
            return customerOrder;
        }
    }

    @KafkaListener(topics = "prod.inventory.failed", groupId = "order-group")
    public void handleInventoryFailedEvent(OrderPlacedEvent orderPlacedEvent){
        System.out.println("Reverse order event : " + orderPlacedEvent);
        try{
            // OrderPlacedEvent orderPlacedEvent = new ObjectMapper().readValue(event, OrderPlacedEvent.class);
            Optional<Order> order = orderRepository.findById(orderPlacedEvent.getOrder().getOrderId());
            order.ifPresent(o -> {
                o.setStatus("FAILED");
                orderRepository.save(o);
            });

        } catch (Exception ex){
            System.out.println("Exception occured while reverting order details");
        }
    }
}
