package test;

import domain.inventory.Inventorycontroller;
import domain.reports.Reportcontroller;
import domain.orders.OrderController;
import domain.suppliers.SupplierController;
import service.IntegrationService;
import service.InventoryService;
import service.OrderService;
import service.SupplierService;
import presentation.InputReader;
import presentation.InventoryMenu;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PresentationTests {

    private InventoryService service;
    private ByteArrayOutputStream captured;
    private PrintStream originalOut;

    @Before
    public void setUp() {
        Inventorycontroller.getInstance().resetAll();
        Reportcontroller.getInstance().resetAll();
        OrderController.getInstance().resetAll();
        SupplierController.getInstance().resetAll();
        service = new InventoryService();
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    public void invalidMenuChoice_showsInvalidMessage() {
        run("99", "6");
        assertContains("Invalid");
    }

    // ── Product menu ──────────────────────────────────────────────────────────

    @Test
    public void addProduct_validInput_showsSuccess() {
        run(
            "1",                          // Product Management
            "1",                          // Add New Product
            "42", "Milk", "Tnuva",
            "dairy", "milk", "whole",
            "5", "A1", "8",
            "0",                          // back
            "6"                           // exit
        );
        assertContains("successfully");
    }

    @Test
    public void sellItem_onShelf_showsSuccess() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        service.addNewItem(101, 99, 3, futureDate(), 1);
        service.moveToShelf(Arrays.asList(101));
        run(
            "1",   // Product Management
            "7",   // Sell Item
            "101",
            "0",   // back
            "6"    // exit
        );
        assertContains("sold");
    }

    // ── Report menu ───────────────────────────────────────────────────────────

    @Test
    public void inventoryReport_allCategories_showsProductName() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        run(
            "2",   // Reports
            "1",   // Inventory Report
            "",    // Enter = all categories
            "0",   // back
            "6"    // exit
        );
        assertContains("milk");
    }

    @Test
    public void defectReport_allTime_showsReason() {
        service.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        service.addNewItem(101, 99, 3, futureDate(), 1);
        service.moveToShelf(Arrays.asList(101));
        service.setDefectiveItem(101, "dented can");
        run(
            "2",   // Reports
            "2",   // Defective Items Report
            "1",   // All time
            "0",   // back
            "6"    // exit
        );
        assertContains("dented can");
    }

    // ── Order menu ──────────────────────────────────────────────────────────

    @Test
    public void orderMenu_createAndListScheduled_showsSupplier() {
        run(
            "5",      // Orders
            "2",      // Scheduled Orders
            "1",      // Create Scheduled Order
            "1",      // supplier id
            "1,3",    // delivery days (Sun, Tue)
            "2",      // List Scheduled Orders
            "0",      // back to Order menu
            "0",      // back to Inventory menu
            "6"       // exit
        );
        assertContains("Tnuva Logistics"); // supplier details resolved through the menu
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Run InventoryMenu with the given sequence of inputs. */
    private void run(String... inputs) {
        OrderService orderService = new OrderService();
        IntegrationService integrationService =
                new IntegrationService(service, orderService, new SupplierService());
        new InventoryMenu(listReader(inputs), service, orderService, integrationService).start();
    }

    private void assertContains(String expected) {
        String out = captured.toString();
        assertTrue("Expected output to contain \"" + expected + "\" but was:\n" + out,
            out.toLowerCase().contains(expected.toLowerCase()));
    }

    private InputReader listReader(String... inputs) {
        final List<String> list = new ArrayList<>(Arrays.asList(inputs));
        return new InputReader() {
            int index = 0;
            public String readString() {
                if (index >= list.size())
                    throw new IllegalStateException("Test ran out of input after " + list.size()
                        + " entries — the menu sequence must end with the Exit option."
                        + " Did the menu layout change?");
                return list.get(index++);
            }
            public int readInt() {
                try { return Integer.parseInt(readString()); }
                catch (NumberFormatException e) { return -1; }
            }
        };
    }

    private Date futureDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        return c.getTime();
    }
}
