package test;

import domain.inventory.Inventorycontroller;
import domain.reports.Reportcontroller;
import service.InventoryService;
import service.Response;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class ServiceTests {

    private InventoryService service;

    @Before
    public void setUp() {
        Inventorycontroller.getInstance().resetAll();
        Reportcontroller.getInstance().resetAll();
        service = new InventoryService();
    }

    // ── addNewProduct ─────────────────────────────────────────────────────────

    @Test
    public void addProduct_validInput_returnsSuccess() {
        Response<Boolean> r = service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        assertTrue(r.isSuccess());
        assertTrue(r.getValue());
    }

    @Test
    public void addProduct_duplicate_returnsFailure() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        Response<Boolean> r = service.addNewProduct(1, "Milk2", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        assertFalse(r.isSuccess());
        assertNotNull(r.getMessage());
    }

    // ── addNewItem ────────────────────────────────────────────────────────────

    @Test
    public void addItem_unknownProduct_returnsFailure() {
        Response<Boolean> r = service.addNewItem(101, 99, 3, futureDate(), 999);
        assertFalse(r.isSuccess());
    }

    // ── getProduct ────────────────────────────────────────────────────────────

    @Test
    public void getProduct_notExists_returnsFailure() {
        assertFalse(service.getProduct(999).isSuccess());
    }

    // ── moveToShelf / sellItem ────────────────────────────────────────────────

    @Test
    public void sellItem_onShelf_succeeds() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        service.addNewItem(101, 99, 3, futureDate(), 1);
        service.moveToShelf(Arrays.asList(101));
        assertTrue(service.itemSell(101).isSuccess());
    }

    // ── discounts / pricing ───────────────────────────────────────────────────

    @Test
    public void getPrice_withActiveDiscount_returnsDiscountedPrice() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 10);
        service.addDiscountForProduct(1, 20, pastDate(), futureDate()); // 20% off
        Response<Double> r = service.getPriceForProduct(1);
        assertTrue(r.isSuccess());
        assertEquals(8.0, r.getValue(), 0.001);
    }

    // ── inventoryCount ────────────────────────────────────────────────────────

    @Test
    public void inventoryCount_wrongStorageCount_returnsFailure() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        service.addNewItem(101, 99, 3, futureDate(), 1); // 1 in storage
        assertFalse(service.inventoryCount(1, 5, 0).isSuccess()); // claims 5
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    @Test
    public void inventoryReport_containsProductName() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        Response<String> r = service.getInventoryReport(Arrays.asList("dairy"));
        assertTrue(r.isSuccess());
        assertTrue(r.getValue().contains("milk"));
    }

    @Test
    public void orderReport_lowStockProduct_appearsInReport() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 10, "A1", 8); // 0 items, min=10
        Response<String> r = service.getOrderReport();
        assertTrue(r.isSuccess());
        assertTrue(r.getValue().contains("milk"));
    }

    @Test
    public void expiredReport_expiredItem_appearsInReport() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        service.addNewItem(101, 99, 3, pastDate(), 1); // expired
        Response<String> r = service.getExpiredReport();
        assertTrue(r.isSuccess());
        assertTrue(r.getValue().contains("101"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Date futureDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        return c.getTime();
    }

    private Date pastDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -1);
        return c.getTime();
    }
}
