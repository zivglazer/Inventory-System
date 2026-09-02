package test;

import domain.suppliers.SupplierController;
import service.Response;
import service.SupplierService;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SupplierTests {

    private SupplierService service;

    @Before
    public void setUp() {
        SupplierController.getInstance().resetAll();
        service = new SupplierService();
    }

    @Test
    public void cheapest_picksLowestPriceSupplier() {
        // "milk": supplier 1 = 5/unit, supplier 2 = 6/unit
        assertEquals(Integer.valueOf(1), service.getCheapestSupplier("milk", 3).getValue());
    }

    @Test
    public void cheapest_bulkTierChangesWinner() {
        // "pretzels": supplier 1 = 6/unit; supplier 2 = 7/unit but 4/unit at qty >= 20
        assertEquals(Integer.valueOf(1), service.getCheapestSupplier("pretzels", 5).getValue());   // small qty -> s1
        assertEquals(Integer.valueOf(2), service.getCheapestSupplier("pretzels", 20).getValue());  // bulk    -> s2
    }

    @Test
    public void cheapest_unknownProduct_fallsBackToDefaultSupplier() {
        Response<Integer> r = service.getCheapestSupplier("nonexistent", 1);
        assertTrue(r.isSuccess());
        assertEquals(Integer.valueOf(1), r.getValue());
    }

    @Test
    public void deliveryDays_returnsSupplierDays() {
        assertEquals(Arrays.asList(1, 3, 5), service.getDeliveryDays(1).getValue());
    }

    @Test
    public void getSupplier_unknown_returnsFailure() {
        assertFalse(service.getSupplier(99).isSuccess());
    }

    @Test
    public void sendOrder_knownSupplier_succeeds() {
        Map<String, Integer> qtys = new HashMap<>();
        qtys.put("milk", 5);
        assertTrue(service.sendOrder(1, qtys).isSuccess());
    }
}
