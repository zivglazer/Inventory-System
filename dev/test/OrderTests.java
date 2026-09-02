package test;

import domain.orders.Order;
import domain.orders.OrderController;
import domain.orders.OrderStatus;
import domain.orders.ScheduledOrder;
import domain.orders.ShortageOrder;
import service.OrderService;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;

public class OrderTests {

    private OrderService orderService;

    @Before
    public void setUp() {
        OrderController.getInstance().resetAll();
        orderService = new OrderService();
    }

    // ── ScheduledOrder domain ──────────────────────────────────────────────────

    @Test
    public void scheduledOrder_isDueOneDayBefore_matchesTomorrow() {
        ScheduledOrder o = new ScheduledOrder(1, 1, Arrays.asList(3)); // delivers Tuesday(3)
        assertTrue(o.isDueOneDayBefore(2));   // Monday  -> tomorrow is Tue (due)
        assertFalse(o.isDueOneDayBefore(3));  // Tuesday -> tomorrow is Wed (not due)
    }

    @Test(expected = IllegalArgumentException.class)
    public void scheduledOrder_invalidDeliveryDay_throws() {
        new ScheduledOrder(1, 1, Arrays.asList(8));
    }

    @Test
    public void removeDeliveryDay_lastOne_rejected() {
        int id = orderService.addScheduledOrder(1, Arrays.asList(3)).getValue();
        assertFalse(orderService.removeDeliveryDayFromScheduledOrder(id, 3).isSuccess());
    }

    @Test
    public void removeDeliveryDay_whenMultiple_succeeds() {
        int id = orderService.addScheduledOrder(1, Arrays.asList(1, 3)).getValue();
        assertTrue(orderService.removeDeliveryDayFromScheduledOrder(id, 1).isSuccess());
        assertEquals(Arrays.asList(3), orderService.getScheduledOrder(id).getValue().getDeliveryDays());
    }

    // ── Order lines ────────────────────────────────────────────────────────────

    @Test
    public void addProduct_sameProductTwice_mergesQuantity() {
        int id = orderService.addScheduledOrder(1, Arrays.asList(3)).getValue();
        orderService.addProductToScheduledOrder(id, 7, 2, 10);
        orderService.addProductToScheduledOrder(id, 7, 5, 10);
        ScheduledOrder o = orderService.getScheduledOrder(id).getValue();
        assertEquals(1, o.getLines().size());
        assertEquals(7, o.getLines().get(0).getQuantity());
    }

    @Test
    public void editQuantity_unknownProduct_returnsFailure() {
        int id = orderService.addScheduledOrder(1, Arrays.asList(3)).getValue();
        assertFalse(orderService.editProductQuantityInScheduledOrder(id, 999, 4).isSuccess());
    }

    // ── Order lifecycle / controller ────────────────────────────────────────────

    @Test
    public void shortageOrder_createdAndQueryable() {
        OrderController ctrl = OrderController.getInstance();
        int id = ctrl.createShortageOrder(1, 5, 4, 6);
        Order o = ctrl.getOrder(id);
        assertTrue(o instanceof ShortageOrder);
        assertEquals(OrderStatus.CREATED, o.getStatus());
        assertEquals("Shortage", o.getType());
    }

    @Test
    public void markSentThenReceived_transitionsStatus() {
        OrderController ctrl = OrderController.getInstance();
        int id = ctrl.createShortageOrder(1, 5, 4, 6);
        ctrl.markSent(id);
        assertEquals(OrderStatus.SENT, ctrl.getOrder(id).getStatus());
        ctrl.markReceived(id);
        assertEquals(OrderStatus.RECEIVED, ctrl.getOrder(id).getStatus());
    }

    @Test
    public void getAllOrders_includesBothKinds() {
        OrderController ctrl = OrderController.getInstance();
        ctrl.createShortageOrder(1, 5, 4, 6);
        orderService.addScheduledOrder(1, Arrays.asList(3));
        assertEquals(2, orderService.getAllOrders().getValue().size());
        assertEquals(1, orderService.getAllScheduledOrders().getValue().size());
    }
}
