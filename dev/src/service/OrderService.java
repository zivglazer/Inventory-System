package service;

import domain.orders.Order;
import domain.orders.OrderController;
import domain.orders.OrderLineRequest;
import domain.orders.ScheduledOrder;

import java.util.List;
import java.util.Map;

/**
 * Facade over the order subsystem. Pure order management — it has no knowledge of
 * inventory or suppliers. Cross-module workflows live in {@link IntegrationService}.
 */
public class OrderService {

    private final OrderController controller;

    public OrderService() {
        this.controller = OrderController.getInstance();
    }

    // Req 19
    public Response<Integer> addScheduledOrder(int supplierId, List<Integer> deliveryDays) {
        try {
            return new Response<>(controller.addScheduledOrder(supplierId, deliveryDays), "Scheduled order added");
        } catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 20
    public Response<Boolean> removeScheduledOrder(int orderId) {
        try {
            controller.getScheduledOrder(orderId); // validate it exists and is scheduled
            return new Response<>(controller.removeOrder(orderId), "Scheduled order removed");
        } catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 21 / 27
    public Response<ScheduledOrder> getScheduledOrder(int orderId) {
        try { return new Response<>(controller.getScheduledOrder(orderId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    public Response<List<ScheduledOrder>> getAllScheduledOrders() {
        try { return new Response<>(controller.getAllScheduledOrders(), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 23 (order-module path: caller supplies the agreed unit price)
    public Response<Boolean> addProductToScheduledOrder(int orderId, int productId, int quantity, int unitPrice) {
        try { controller.addProductInOrder(orderId, productId, quantity, unitPrice);
              return new Response<>(true, "Product added to scheduled order"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** The supplier an order is placed with (used by the cross-module add-product flow). */
    public Response<Integer> getOrderSupplier(int orderId) {
        try { return new Response<>(controller.getOrderSupplier(orderId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** True if the order already has a line for this product. */
    public Response<Boolean> containInOrder(int orderId, int productId) {
        try { return new Response<>(controller.containInOrder(orderId, productId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 24
    public Response<Boolean> editProductQuantityInScheduledOrder(int orderId, int productId, int quantity) {
        try { controller.editScheduledQuantity(orderId, productId, quantity);
              return new Response<>(true, "Quantity updated"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 25
    public Response<Boolean> removeDeliveryDayFromScheduledOrder(int orderId, int day) {
        try { controller.removeDeliveryDay(orderId, day);
              return new Response<>(true, "Delivery day removed"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    public Response<List<Order>> getAllOrders() {
        try { return new Response<>(controller.getAllOrders(), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    public Response<Order> getOrder(int orderId) {
        try { return new Response<>(controller.getOrder(orderId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // ── Used by IntegrationService (orchestrator) ────────────────────────────
    // Req 28: order record for a shortage (cheapest supplier chosen by the orchestrator).
    public Response<Integer> createShortageOrder(int supplierId, int productId, int quantity, int unitPrice) {
        try { return new Response<>(controller.createShortageOrder(supplierId, productId, quantity, unitPrice), "Shortage order created"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** Create one shortage order for a supplier holding all the given lines. */
    public Response<Integer> createShortageOrder(int supplierId, List<OrderLineRequest> lines) {
        try { return new Response<>(controller.createShortageOrder(supplierId, lines), "Shortage order created"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** Materialise a real order from a due scheduled template (lines already priced at conversion). */
    public Response<Integer> createDispatchedOrder(int supplierId, List<OrderLineRequest> lines) {
        try { return new Response<>(controller.createDispatchedOrder(supplierId, lines), "Dispatched order created"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** Quantity of a product already on the way on open SHORTAGE orders (derived expected inventory). */
    public Response<Integer> getPendingQuantity(int productId) {
        try { return new Response<>(controller.getPendingQuantity(productId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** futureSupplying / Expected Inventory: product -> quantity already on the way on open shortage orders. */
    public Response<Map<Integer, Integer>> getFutureSupplying() {
        try { return new Response<>(controller.getFutureSupplying(), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** Raw delivery-day add (no supplier validation — that is the orchestrator's job, Req 26). */
    public Response<Boolean> addDeliveryDayToScheduledOrder(int orderId, int day) {
        try { controller.addDeliveryDay(orderId, day);
              return new Response<>(true, "Delivery day added"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    public Response<Boolean> markSent(int orderId) {
        try { controller.markSent(orderId); return new Response<>(true, "Order sent"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    public Response<Boolean> markReceived(int orderId) {
        try { controller.markReceived(orderId); return new Response<>(true, "Order received"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }
}
