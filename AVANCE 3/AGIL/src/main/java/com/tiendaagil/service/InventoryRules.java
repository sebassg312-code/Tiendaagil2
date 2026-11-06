package com.tiendaagil.service;

/** Reglas de inventario independientes de Swing para facilitar pruebas unitarias. */
public final class InventoryRules {
    private InventoryRules() {}
    public static boolean isLowStock(int stock, int minimum) {
        if (stock < 0 || minimum < 0) throw new IllegalArgumentException("Stock no puede ser negativo");
        return stock <= minimum;
    }
    public static void validateSaleQuantity(int stock, int quantity) {
        if (stock < 0 || quantity < 1 || quantity > stock) throw new IllegalArgumentException("Stock insuficiente o cantidad inválida");
    }
    public static int stockAfterSale(int stock, int quantity) {
        validateSaleQuantity(stock, quantity);
        return stock - quantity;
    }
}
