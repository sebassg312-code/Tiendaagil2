package com.tiendaagil.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SaleCalculatorTest {
 @Test void calculatesSubtotalTaxAndTotal(){double sub=SaleCalculator.subtotal(3.50,2);double tax=SaleCalculator.tax(sub,.18);assertEquals(7.00,sub);assertEquals(1.26,tax);assertEquals(8.26,SaleCalculator.total(sub,tax));}
 @Test void calculatesChange(){assertEquals(1.74,SaleCalculator.change(10,8.26));}
 @Test void rejectsInsufficientPayment(){assertThrows(IllegalArgumentException.class,()->SaleCalculator.change(5,8));}
 @Test void rejectsInvalidQuantity(){assertThrows(IllegalArgumentException.class,()->SaleCalculator.subtotal(2,-1));}
}
