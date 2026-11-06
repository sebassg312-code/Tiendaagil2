package com.tiendaagil.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InventoryRulesTest {
 @Test void flagsStockAtMinimum(){assertTrue(InventoryRules.isLowStock(5,5));assertFalse(InventoryRules.isLowStock(6,5));}
 @Test void deductsStockAfterSale(){assertEquals(7,InventoryRules.stockAfterSale(10,3));}
 @Test void preventsOverselling(){assertThrows(IllegalArgumentException.class,()->InventoryRules.stockAfterSale(2,3));}
}
