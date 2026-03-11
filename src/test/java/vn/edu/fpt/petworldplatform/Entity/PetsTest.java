package vn.edu.fpt.petworldplatform.Entity;

import org.junit.jupiter.api.Test;
import vn.edu.fpt.petworldplatform.entity.Pets;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PetsTest {

    @Test
    void testCalculateSalePrice() {

        Pets pet = new Pets();

        pet.setPrice(new BigDecimal("100"));
        pet.setDiscountPercent(10);

        pet.onCreate();

        assertEquals(new BigDecimal("90"), pet.getSalePrice());
    }

    @Test
    void testSalePriceNoDiscount() {

        Pets pet = new Pets();

        pet.setPrice(new BigDecimal("100"));
        pet.setDiscountPercent(0);

        pet.onCreate();

        assertEquals(new BigDecimal("100"), pet.getSalePrice());
    }

    @Test
    void testSalePrice50Percent() {

        Pets pet = new Pets();

        pet.setPrice(new BigDecimal("200"));
        pet.setDiscountPercent(50);

        pet.onCreate();

        assertEquals(new BigDecimal("100"), pet.getSalePrice());
    }

    @Test
    void testDefaultAvailable() {

        Pets pet = new Pets();
        pet.setPrice(new BigDecimal("100"));

        pet.onCreate();

        assertTrue(pet.getIsAvailable());
    }


}
