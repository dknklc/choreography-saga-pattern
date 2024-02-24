package com.dekankilic.inventory.controller;

import com.dekankilic.inventory.dto.StockAddRequest;
import com.dekankilic.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventories")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<String> addStockToInventory(@RequestBody StockAddRequest stockAddRequest){
        inventoryService.addStock(stockAddRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Stock added to Inventory");
    }

}
