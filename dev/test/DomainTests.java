package test;

import domain.inventory.*;
import domain.reports.Reportcontroller;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;
import java.util.Date;

public class DomainTests {

    @Before
    public void setUp() {
        Inventorycontroller.getInstance().resetAll();
        Reportcontroller.getInstance().resetAll();
    }

    // ── Item ──────────────────────────────────────────────────────────────────

    @Test(expected = IllegalStateException.class)
    public void item_sellExpired_throws() {
        new Item(1, 5, pastDate()).sell(new Date(), 10.0);
    }

    // ── Product ───────────────────────────────────────────────────────────────

    @Test
    public void product_addExpiredItem_doesNotGoToStorage() {
        Product p = product();
        p.addNewItem(101, 99, 3, pastDate());
        assertEquals(0, p.getStorageItemsQuantity());
    }

    @Test
    public void product_moveToShelf_movesFromStorage() {
        Product p = product();
        p.addNewItem(101, 99, 3, futureDate());
        p.moveToShelf(101);
        assertEquals(0, p.getStorageItemsQuantity());
        assertEquals(1, p.getShelfQuantity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void product_sellFromStorage_throws() {
        Product p = product();
        p.addNewItem(101, 99, 3, futureDate());
        p.sellItem(101, new Date(), 10.0); // still in storage
    }

    @Test
    public void product_needsRestock_whenBelowMin() {
        Product p = product(); // minRestock = 10, 0 items
        assertTrue(p.needToRestock());
    }

    @Test
    public void product_minNotSet_defaultsToTen() {
        Product p = new Product(1, "Milk", "Tnuva", "dairy", "milk", 1, 8, 0); // 0 = not set
        assertEquals(Product.DEFAULT_MIN_TO_RESTOCK, p.getMinToRestock());
    }

    // ── Discounts ─────────────────────────────────────────────────────────────

    @Test
    public void product_activeDiscount_reducesPrice() {
        Product p = product(); // price = 10
        p.addDiscount(new DiscountInfo(50, pastDate(), futureDate())); // 50% off
        assertEquals(5.0, p.getPrice(), 0.001);
    }

    // ── Inventorycontroller ───────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void controller_addDuplicateName_throws() {
        Inventorycontroller ctrl = Inventorycontroller.getInstance();
        ctrl.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        // same name (case-insensitive), different id -> name is the universal unique identifier
        ctrl.addNewProduct(2, "milk", "Osem", "dairy", "milk", "whole", 5, "A2", 8);
    }

    @Test
    public void controller_sellItem_recordsInSoldList() {
        Inventorycontroller ctrl = Inventorycontroller.getInstance();
        ctrl.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        ctrl.addNewItem(101, 1, 99, 3, futureDate());
        ctrl.moveToShelf(101);
        assertTrue(ctrl.sellItem(101));
        assertNotNull(ctrl.getSoldItemsById(101));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product product() {
        return new Product(1, "Milk", "Tnuva", "dairy", "milk", 1, 10, 10);
    }

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
