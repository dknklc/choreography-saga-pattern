package com.dekankilic.inventory.repository;

import com.dekankilic.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByItem(String item);
}
