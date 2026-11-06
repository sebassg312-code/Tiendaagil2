package com.tiendaagil.service;

/** Reglas puras de cálculo de venta, separadas de la interfaz gráfica. */
public final class SaleCalculator {
    private SaleCalculator() {}
    public static double subtotal(double unitPrice, int quantity) {
        if (!Double.isFinite(unitPrice) || unitPrice < 0 || quantity < 1) throw new IllegalArgumentException("Precio o cantidad inválidos");
        return round(unitPrice * quantity);
    }
    public static double tax(double subtotal, double rate) {
        if (!Double.isFinite(subtotal) || subtotal < 0 || rate < 0 || rate > 1) throw new IllegalArgumentException("Importe o tasa inválidos");
        return round(subtotal * rate);
    }
    public static double total(double subtotal, double tax) {
        if (subtotal < 0 || tax < 0) throw new IllegalArgumentException("Importes inválidos");
        return round(subtotal + tax);
    }
    public static double change(double received, double total) {
        if (received < total || total < 0) throw new IllegalArgumentException("Pago insuficiente");
        return round(received - total);
    }
    public static double round(double amount) { return Math.round(amount * 100.0) / 100.0; }
}
