package test;

import domain.inventory.Inventorycontroller;
import domain.reports.Reportcontroller;
import domain.orders.Order;
import domain.orders.OrderController;
import domain.orders.OrderStatus;
import domain.orders.ScheduledOrder;
import domain.suppliers.SupplierController;
import service.IntegrationService;
import service.InventoryService;
import service.OrderService;
import service.Response;
import service.ScheduledOrderRunner;
import service.SupplierService;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IntegrationTests {

    private InventoryService inventoryService;
    private OrderService orderService;
    private IntegrationService integrationService;

    @Before
    public void setUp() {
        Inventorycontroller.getInstance().resetAll();
        Reportcontroller.getInstance().resetAll();
        OrderController.getInstance().resetAll();
        SupplierController.getInstance().resetAll();
        inventoryService = new InventoryService();
        orderService = new OrderService();
        integrationService = new IntegrationService(inventoryService, orderService, new SupplierService());
    }

    // ── autoOrderLowStock: Req 28 + 29 ───────────────────────────────────────

    @Test
    public void autoOrder_productBelowMin_oneSummaryEntry() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        // 0 items, min=5 → should be ordered
        Response<List<String>> r = integrationService.autoOrderLowStock();
        assertTrue(r.isSuccess());
        assertEquals(1, r.getValue().size());
    }

    @Test
    public void autoOrder_orderQuantityExceedsMin_correctQtyInSummary() {
        // min=5, totalQty=2 → orderQty = 5 - 2 + 1 = 4
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        inventoryService.addNewItem(101, 99, 3, futureDate(), 1);
        inventoryService.addNewItem(102, 99, 3, futureDate(), 1);
        Response<List<String>> r = integrationService.autoOrderLowStock();
        assertTrue(r.isSuccess());
        assertEquals(1, r.getValue().size());
        assertTrue(r.getValue().get(0).contains("Ordered 4"));
    }

    @Test
    public void autoOrder_sameSupplier_groupedIntoOneOrder() {
        inventoryService.addNewProduct(1, "Milk",   "Tnuva",   "dairy", "milk",   "whole", 5, "A1", 8);
        inventoryService.addNewProduct(2, "Yogurt", "Yotvata", "dairy", "yogurt", "plain", 5, "A2", 5);
        // both 0 items, both cheapest from supplier 1
        Response<List<String>> r = integrationService.autoOrderLowStock();
        assertTrue(r.isSuccess());
        assertEquals(2, r.getValue().size());                       // one summary entry per product
        assertEquals(1, orderService.getAllOrders().getValue().size()); // but ONE order (grouped per supplier)
        assertEquals(2, orderService.getAllOrders().getValue().get(0).getLines().size());
    }

    @Test
    public void autoOrder_runTwice_doesNotReorderPending() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        Response<List<String>> first = integrationService.autoOrderLowStock();
        assertEquals(1, first.getValue().size());                  // ordered the shortage once
        Response<List<String>> second = integrationService.autoOrderLowStock();
        assertTrue(second.isSuccess());
        assertTrue(second.getValue().isEmpty());                   // already on the way -> nothing
        assertEquals(1, orderService.getAllOrders().getValue().size()); // no new order
    }

    @Test
    public void autoOrder_ignoresScheduledOrders_whenComputingPending() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        // a scheduled order also carrying product 1 must NOT offset the auto order
        int sched = orderService.addScheduledOrder(1, Arrays.asList(3)).getValue();
        integrationService.addProductToOrder(sched, 1, 100);
        Response<List<String>> r = integrationService.autoOrderLowStock();
        assertEquals(1, r.getValue().size());                      // still orders, ignoring scheduled qty
    }

    // ── event-driven shortage ordering: re-check on each stock-reducing action ───

    @Test
    public void sellItem_dropsBelowMin_triggersShortageOrder() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 2, "A1", 8);
        inventoryService.addNewItem(101, 99, 3, futureDate(), 1);
        inventoryService.addNewItem(102, 99, 3, futureDate(), 1); // 2 items == min, not short yet
        inventoryService.moveToShelf(Arrays.asList(101, 102));

        Response<List<String>> sell = integrationService.sellItem(101); // now 1 < min=2
        assertTrue(sell.isSuccess());
        assertEquals(1, sell.getValue().size());                          // a shortage order was placed
        assertEquals(1, orderService.getAllOrders().getValue().size());
    }

    @Test
    public void markDefective_dropsBelowMin_triggersShortageOrder() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 2, "A1", 8);
        inventoryService.addNewItem(101, 99, 3, futureDate(), 1);
        inventoryService.addNewItem(102, 99, 3, futureDate(), 1);
        inventoryService.moveToShelf(Arrays.asList(101, 102));

        Response<List<String>> def = integrationService.markDefective(101, "broken"); // now 1 < min=2
        assertTrue(def.isSuccess());
        assertEquals(1, def.getValue().size());
        assertEquals(1, orderService.getAllOrders().getValue().size());
    }

    @Test
    public void removeItem_dropsBelowMin_triggersShortageOrder() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 2, "A1", 8);
        inventoryService.addNewItem(101, 99, 3, futureDate(), 1);
        inventoryService.addNewItem(102, 99, 3, futureDate(), 1);

        Response<List<String>> removed = integrationService.removeItem(101); // now 1 < min=2
        assertTrue(removed.isSuccess());
        assertEquals(1, removed.getValue().size());
        assertEquals(1, orderService.getAllOrders().getValue().size());
    }

    // ── receiveOrderItem: Req 31 ──────────────────────────────────────────────

    @Test
    public void receiveItem_validInput_success() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        Response<Boolean> r = integrationService.receiveOrderItem(101, 99, 3, futureDate(), 1);
        assertTrue(r.isSuccess());
        assertTrue(r.getValue());
    }

    @Test
    public void receiveItem_unknownProduct_returnsFailure() {
        Response<Boolean> r = integrationService.receiveOrderItem(101, 99, 3, futureDate(), 999);
        assertFalse(r.isSuccess());
        assertNotNull(r.getMessage());
    }

    @Test
    public void receiveItem_expiredItem_registeredAsExpired() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        integrationService.receiveOrderItem(101, 99, 3, pastDate(), 1); // expired on arrival
        Response<String> report = inventoryService.getExpiredReport();
        assertTrue(report.isSuccess());
        assertTrue(report.getValue().contains("101"));
    }

    // ── Scheduled templates: due one day before delivery -> converted into real orders ──

    @Test
    public void scheduledTemplate_dueTomorrow_convertedAndDispatched() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        Calendar c = Calendar.getInstance();
        Date today = c.getTime();
        int tomorrow = c.get(Calendar.DAY_OF_WEEK) % 7 + 1;
        int templateId = orderService.addScheduledOrder(1, Arrays.asList(tomorrow)).getValue();
        integrationService.addProductToOrder(templateId, 1, 5);

        Response<List<String>> r = integrationService.dispatchDueScheduledOrders(today);
        assertTrue(r.isSuccess());
        assertEquals(1, r.getValue().size()); // one template converted

        // the template is left untouched so it recurs next week
        assertEquals(OrderStatus.CREATED, orderService.getOrder(templateId).getValue().getStatus());
        // a brand-new real order was created and dispatched
        List<Order> orders = orderService.getAllOrders().getValue();
        assertEquals(2, orders.size());
        Order real = orders.stream().filter(o -> o.getOrderId() != templateId).findFirst().get();
        assertEquals(OrderStatus.SENT, real.getStatus());
        assertEquals("Dispatched", real.getType());
    }

    @Test
    public void scheduledTemplate_notDue_notConverted() {
        Calendar c = Calendar.getInstance();
        Date today = c.getTime();
        int todayDow = c.get(Calendar.DAY_OF_WEEK); // delivery today => not due "one day before"
        orderService.addScheduledOrder(1, Arrays.asList(todayDow));

        Response<List<String>> r = integrationService.dispatchDueScheduledOrders(today);
        assertTrue(r.isSuccess());
        assertTrue(r.getValue().isEmpty());
    }

    @Test
    public void scheduledConversion_pricesLinesFromSupplierAgreement() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        Calendar c = Calendar.getInstance();
        Date today = c.getTime();
        int tomorrow = c.get(Calendar.DAY_OF_WEEK) % 7 + 1;
        int templateId = orderService.addScheduledOrder(1, Arrays.asList(tomorrow)).getValue();
        integrationService.addProductToOrder(templateId, 1, 5); // supplier 1, "milk" -> agreed unit price 5

        integrationService.dispatchDueScheduledOrders(today);

        Order real = orderService.getAllOrders().getValue().stream()
                .filter(o -> o.getOrderId() != templateId).findFirst().get();
        assertEquals(5, real.getLines().get(0).getQuantity());
        assertEquals(5, real.getLines().get(0).getUnitPrice());   // priced ONLY at conversion
        // the template line itself stays unpriced
        assertEquals(0, orderService.getScheduledOrder(templateId).getValue().getLines().get(0).getUnitPrice());
    }

    @Test
    public void scheduledOrderRunner_runsAtConfiguredTime() {
        ScheduledOrderRunner runner = new ScheduledOrderRunner(integrationService, 17, 49);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 17); c.set(Calendar.MINUTE, 48);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        assertEquals(TimeUnit.MINUTES.toMillis(1), runner.millisUntilNextRun(c.getTime())); // 17:48 -> 17:49 today
        c.set(Calendar.MINUTE, 50);
        assertTrue(runner.millisUntilNextRun(c.getTime()) > TimeUnit.HOURS.toMillis(23));    // 17:50 -> 17:49 tomorrow
        assertTrue(runner.runNow().isSuccess());
    }

    @Test
    public void scheduledConversion_quantitiesComeFromTemplate_notInventory() {
        // scheduled procurement is independent of inventory levels: the converted order carries the
        // template's own quantities, with no inventory-based top-up (spec rules 5a/5b).
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 10, "A1", 8);
        Calendar c = Calendar.getInstance();
        Date today = c.getTime();
        int tomorrow = c.get(Calendar.DAY_OF_WEEK) % 7 + 1;
        int templateId = orderService.addScheduledOrder(1, Arrays.asList(tomorrow)).getValue();
        integrationService.addProductToOrder(templateId, 1, 2); // template asks for exactly 2

        integrationService.dispatchDueScheduledOrders(today);

        Order real = orderService.getAllOrders().getValue().stream()
                .filter(o -> o.getOrderId() != templateId).findFirst().get();
        assertEquals(2, real.getLines().get(0).getQuantity()); // exactly the template quantity
    }

    // ── addProductToOrder onto a scheduled template: catalog-checked, no duplicates, unpriced ──

    @Test
    public void addProductToOrder_inCatalog_addsUnpriced() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 5, "A1", 8);
        int id = orderService.addScheduledOrder(1, Arrays.asList(3)).getValue();
        Response<Boolean> r = integrationService.addProductToOrder(id, 1, 5);
        assertTrue(r.isSuccess());
        ScheduledOrder o = orderService.getScheduledOrder(id).getValue();
        assertEquals(1, o.getLines().size());
        assertEquals(5, o.getLines().get(0).getQuantity());
        assertEquals(0, o.getLines().get(0).getUnitPrice()); // priced only at conversion
    }

    @Test
    public void addProductToOrder_notInSupplierCatalog_fails() {
        // "ghost" exists in inventory but no supplier carries it -> the catalog check (by name) fails.
        inventoryService.addNewProduct(9, "Ghost", "NoOne", "misc", "none", "none", 5, "Z9", 1);
        int id = orderService.addScheduledOrder(1, Arrays.asList(3)).getValue();
        Response<Boolean> r = integrationService.addProductToOrder(id, 9, 3);
        assertFalse(r.isSuccess());
    }

    // ── Add delivery day must be a supplier delivery day: Req 26 ────────────

    @Test
    public void addDeliveryDay_nonSupplierDay_rejected() {
        int id = orderService.addScheduledOrder(1, Arrays.asList(1)).getValue();
        Response<Boolean> r = integrationService.addDeliveryDayToScheduledOrder(id, 2); // 2 not in 1,3,5
        assertFalse(r.isSuccess());
    }

    // ── Order lifecycle: receive completes the order and raises stock (Req 30/31) ──

    @Test
    public void receiveOrder_marksReceivedAndRaisesStockAboveMin() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 3, "A1", 8);
        Response<List<String>> auto = integrationService.autoOrderLowStock(); // creates order #1, qty 4
        assertEquals(1, auto.getValue().size());

        List<IntegrationService.ReceivedUnit> units = Arrays.asList(
            new IntegrationService.ReceivedUnit(101, 1, 3, futureDate()),
            new IntegrationService.ReceivedUnit(102, 1, 3, futureDate()),
            new IntegrationService.ReceivedUnit(103, 1, 3, futureDate()),
            new IntegrationService.ReceivedUnit(104, 1, 3, futureDate()));
        Response<Boolean> recv = integrationService.receiveOrder(1, units);
        assertTrue(recv.isSuccess());

        Response<Order> order = orderService.getOrder(1);
        assertTrue(order.isSuccess());
        assertEquals(OrderStatus.RECEIVED, order.getValue().getStatus());

        Response<List<String>> after = inventoryService.showProductToRestock();
        assertTrue(after.getValue().isEmpty()); // 4 items > min 3
    }

    @Test
    public void futureSupplying_trackedThenRemovedOnCompletion() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 3, "A1", 8);
        integrationService.autoOrderLowStock(); // order #1: product 1, qty = 3 - 0 + 1 = 4, SENT
        assertEquals(Integer.valueOf(4), orderService.getFutureSupplying().getValue().get(1));

        List<IntegrationService.ReceivedUnit> units = Arrays.asList(
            new IntegrationService.ReceivedUnit(101, 1, 3, futureDate()),
            new IntegrationService.ReceivedUnit(102, 1, 3, futureDate()),
            new IntegrationService.ReceivedUnit(103, 1, 3, futureDate()),
            new IntegrationService.ReceivedUnit(104, 1, 3, futureDate()));
        integrationService.receiveOrder(1, units);
        // delivered -> order RECEIVED -> dropped from futureSupplying
        assertNull(orderService.getFutureSupplying().getValue().get(1));
    }

    @Test
    public void receiveOrder_productNotOnOrder_rejected() {
        inventoryService.addNewProduct(1, "Milk", "Tnuva", "dairy", "milk", "whole", 3, "A1", 8);
        integrationService.autoOrderLowStock(); // creates + sends order #1 for product 1
        // delivering a product that is not a line on the order must be refused
        Response<Boolean> r = integrationService.receiveOrder(1, Arrays.asList(
            new IntegrationService.ReceivedUnit(201, 2, 3, futureDate())));
        assertFalse(r.isSuccess());
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
