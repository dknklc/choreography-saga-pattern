package com.dekankilic.inventory.service;

import com.dekankilic.inventory.dto.CustomerOrder;
import com.dekankilic.inventory.dto.OrderPlacedEvent;
import com.dekankilic.inventory.dto.InventoryEvent;
import com.dekankilic.inventory.dto.StockAddRequest;
import com.dekankilic.inventory.model.Inventory;
import com.dekankilic.inventory.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate kafkaTemplate;

    @KafkaListener(topics = "prod.order.placed", groupId = "inventory-group")
    public void handleOrderPlacedEvent(OrderPlacedEvent orderPlacedEvent) throws Exception{
        // OrderPlacedEvent orderPlacedEvent = new ObjectMapper().readValue(event, OrderPlacedEvent.class);
        CustomerOrder customerOrder = orderPlacedEvent.getOrder();
        InventoryEvent inventoryEvent = InventoryEvent.builder()
                .order(orderPlacedEvent.getOrder())
                .type("INVENTORY_UPDATED")
                .build();

        try {
            // Iterable<Inventory> inventories = inventoryRepository.findByItem(customerOrder.getItem());
            Optional<Inventory> optionalInventory = inventoryRepository.findByItem(customerOrder.getItem()).stream().filter(inventory -> inventory.getQuantity() > customerOrder.getQuantity()).findAny();
            if(optionalInventory.isPresent()){
                optionalInventory.get().setQuantity(optionalInventory.get().getQuantity() - customerOrder.getQuantity());
                inventoryRepository.save(optionalInventory.get());

                kafkaTemplate.send("prod.inventory.updated", String.valueOf(customerOrder.getOrderId()), inventoryEvent);

            } else {
                System.out.println("Stock not exist so reverting the order");
                throw new Exception("Stock not available");
            }

        } catch (Exception ex){
            OrderPlacedEvent ordPlacedEvent = new OrderPlacedEvent();
            ordPlacedEvent.setOrder(customerOrder);
            ordPlacedEvent.setType("INVENTORY_FAILED");
            kafkaTemplate.send("prod.inventory.failed", String.valueOf(customerOrder.getOrderId()), ordPlacedEvent);
        }
    }

    public void addStock(StockAddRequest stockAddRequest){
        Optional<Inventory> inventory = inventoryRepository.findByItem(stockAddRequest.getItem()).stream().findFirst();
        if(inventory.isPresent()){
            inventory.get().setQuantity(inventory.get().getQuantity() + stockAddRequest.getQuantity());
            inventoryRepository.save(inventory.get());
        }else {
            Inventory newInv = Inventory.builder()
                    .item(stockAddRequest.getItem())
                    .quantity(stockAddRequest.getQuantity())
                    .build();
            inventoryRepository.save(newInv);
        }

    }

    @KafkaListener(topics = "prod.payment.failed", groupId = "inventory-group")
    public void handlePaymentFailedEvent(InventoryEvent inventoryEvent){
        System.out.println("Reverse inventory event : " + inventoryEvent);
        CustomerOrder customerOrder = inventoryEvent.getOrder();

        try{
            Optional<Inventory> inventory = inventoryRepository.findByItem(customerOrder.getItem()).stream().findFirst();
            inventory.ifPresent(inv -> {
                inv.setQuantity(inv.getQuantity() + customerOrder.getQuantity());
                inventoryRepository.save(inventory.get());
            });
        } catch (Exception ex){
            System.out.println("Exception occured while reverting order details");
        }
    }

}
