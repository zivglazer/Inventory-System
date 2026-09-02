package domain.orders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dataaccess.Database;
import dataaccess.dao.OrderDAO;
import dataaccess.dao.OrderDeliveryDayDAO;
import dataaccess.dao.OrderLineDAO;
import dataaccess.dto.OrderDTO;
import dataaccess.dto.OrderLineDTO;
import dataaccess.mapper.OrderMapper;

/**
 * Owns every order (shortage orders + scheduled templates) placed with suppliers. The orders
 * live in the database; this controller is the orchestrator: it loads the domain order(s) an
 * operation needs, runs the business logic on them, then persists the result. Order ids are
 * system-allocated by the DB ({@code MAX(order_id)+1}).
 */
public class OrderController {

    private static OrderController instance = null;

    private final OrderDAO orderDAO            = new OrderDAO();
    private final OrderLineDAO lineDAO         = new OrderLineDAO();
    private final OrderDeliveryDayDAO dayDAO   = new OrderDeliveryDayDAO();

    private OrderController() {}

    public static OrderController getInstance() {
        if (instance == null) instance = new OrderController();
        return instance;
    }

    public void resetAll() {
        Database.clearTables(Arrays.asList("order_delivery_day", "order_line", "orders"));
        Database.clearAllCaches();
    }

    // ── Reconstruction ───────────────────────────────────────────────────────────
    private Order loadOrder(int orderId) {
        OrderDTO dto = orderDAO.find(orderId);
        if (dto == null)
            throw new IllegalArgumentException("Order " + orderId + " doesn't exist");
        List<Integer> days = OrderMapper.SCHEDULED.equals(dto.type) ? dayDAO.findByOrder(orderId) : null;
        return OrderMapper.toDomain(dto, lineDAO.findByOrder(orderId), days);
    }

    private void persistNew(Order order) {
        Database.runInTransaction(() -> {
            orderDAO.insert(OrderMapper.toDTO(order));
            for (OrderLine l : order.getLines())
                lineDAO.insert(new OrderLineDTO(order.getOrderId(), l.getProductId(), l.getQuantity(), l.getUnitPrice()));
            if (order instanceof ScheduledOrder)
                for (int day : ((ScheduledOrder) order).getDeliveryDays())
                    dayDAO.insert(order.getOrderId(), day);
        });
    }

    /** Re-write the order's lines to match the (already mutated) domain object. */
    private void replaceLines(Order order) {
        Database.runInTransaction(() -> {
            lineDAO.deleteByOrder(order.getOrderId());
            for (OrderLine l : order.getLines())
                lineDAO.insert(new OrderLineDTO(order.getOrderId(), l.getProductId(), l.getQuantity(), l.getUnitPrice()));
        });
    }

    /** Re-write the scheduled order's delivery days to match the domain object. */
    private void replaceDays(ScheduledOrder order) {
        Database.runInTransaction(() -> {
            dayDAO.deleteByOrder(order.getOrderId());
            for (int day : order.getDeliveryDays())
                dayDAO.insert(order.getOrderId(), day);
        });
    }

    // ── Creation ──────────────────────────────────────────────────────────────
    public int createShortageOrder(int supplierId, int productId, int quantity, int unitPrice) {
        return createShortageOrder(supplierId,
                Collections.singletonList(new OrderLineRequest(productId, quantity, unitPrice)));
    }

    /** Create one shortage order for a supplier holding all the given lines. */
    public int createShortageOrder(int supplierId, List<OrderLineRequest> lines) {
        ShortageOrder order = new ShortageOrder(orderDAO.nextId(), supplierId);
        for (OrderLineRequest line : lines)
            order.addLine(line.getProductId(), line.getQuantity(), line.getUnitPrice());
        persistNew(order);
        return order.getOrderId();
    }

    public int addScheduledOrder(int supplierId, List<Integer> deliveryDays) {
        ScheduledOrder order = new ScheduledOrder(orderDAO.nextId(), supplierId, deliveryDays);
        persistNew(order);
        return order.getOrderId();
    }

    /**
     * Materialise a real, dispatched order from a due scheduled template: one order for the template's
     * supplier holding the given (already priced-at-conversion) lines. Distinct from a shortage order
     * so it never offsets automatic procurement.
     */
    public int createDispatchedOrder(int supplierId, List<OrderLineRequest> lines) {
        DispatchedOrder order = new DispatchedOrder(orderDAO.nextId(), supplierId);
        for (OrderLineRequest line : lines)
            order.addLine(line.getProductId(), line.getQuantity(), line.getUnitPrice());
        persistNew(order);
        return order.getOrderId();
    }

    // ── Lookup ────────────────────────────────────────────────────────────────
    public Order getOrder(int orderId) {
        return loadOrder(orderId);
    }

    /** The supplier this order is placed with. */
    public int getOrderSupplier(int orderId) {
        return loadOrder(orderId).getSupplierId();
    }

    /** True if the order already has a line for this product. */
    public boolean containInOrder(int orderId, int productId) {
        return loadOrder(orderId).containsProduct(productId);
    }

    /**
     * Total quantity of a product already on the way on open SHORTAGE orders (CREATED/SENT,
     * not yet RECEIVED) -- the derived "expected inventory". Scheduled/dispatched orders are
     * intentionally excluded so automatic inventory orders and scheduled orders stay
     * independent (no quantity offsetting between the two processes).
     */
    public int getPendingQuantity(int productId) {
        return getFutureSupplying().getOrDefault(productId, 0);
    }

    /**
     * The "futureSupplying" tracker (Expected Inventory): product -> total quantity already on the
     * way on open SHORTAGE orders (CREATED/SENT, not yet RECEIVED). Scheduled/dispatched orders are
     * intentionally excluded so automatic and scheduled procurement stay independent. It is derived
     * straight from the orders, so a delivered order (status RECEIVED) is automatically dropped from
     * the tracker on completion -- there is no separate store that could drift out of sync.
     */
    public Map<Integer, Integer> getFutureSupplying() {
        Map<Integer, Integer> expected = new LinkedHashMap<>();
        for (int id : orderDAO.findIdsByType(OrderMapper.SHORTAGE)) {
            Order order = loadOrder(id);
            if (order.getStatus() == OrderStatus.RECEIVED) continue;
            for (OrderLine line : order.getLines())
                expected.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }
        return expected;
    }

    public ScheduledOrder getScheduledOrder(int orderId) {
        Order order = loadOrder(orderId);
        if (!(order instanceof ScheduledOrder))
            throw new IllegalArgumentException("Order " + orderId + " is not a scheduled order");
        return (ScheduledOrder) order;
    }

    public List<Order> getAllOrders() {
        List<Order> result = new ArrayList<>();
        for (int id : orderDAO.findAllIds()) result.add(loadOrder(id));
        return result;
    }

    public List<ScheduledOrder> getAllScheduledOrders() {
        List<ScheduledOrder> result = new ArrayList<>();
        for (int id : orderDAO.findIdsByType(OrderMapper.SCHEDULED))
            result.add((ScheduledOrder) loadOrder(id));
        return result;
    }

    public List<ScheduledOrder> getDueScheduledOrders(int todayDayOfWeek) {
        List<ScheduledOrder> result = new ArrayList<>();
        for (ScheduledOrder order : getAllScheduledOrders())
            if (order.isDueOneDayBefore(todayDayOfWeek))
                result.add(order);
        return result;
    }

    // ── Mutation ────────────────────────────────────────────────────────────────
    public boolean removeOrder(int orderId) {
        if (orderDAO.find(orderId) == null) return false;
        Database.runInTransaction(() -> {
            dayDAO.deleteByOrder(orderId);
            lineDAO.deleteByOrder(orderId);
            orderDAO.delete(orderId);
        });
        return true;
    }

    public void addProductInOrder(int orderId, int productId, int quantity, int unitPrice) {
        ScheduledOrder order = getScheduledOrder(orderId);
        order.addLine(productId, quantity, unitPrice);   // domain: merge same product
        replaceLines(order);
    }

    public void editScheduledQuantity(int orderId, int productId, int quantity) {
        ScheduledOrder order = getScheduledOrder(orderId);
        order.setLineQuantity(productId, quantity);  // domain: throws if product not in order
        replaceLines(order);
    }

    public void addDeliveryDay(int orderId, int day) {
        ScheduledOrder order = getScheduledOrder(orderId);
        order.addDeliveryDay(day);                   // domain: validate 1..7 + dedup
        replaceDays(order);
    }

    public void removeDeliveryDay(int orderId, int day) {
        ScheduledOrder order = getScheduledOrder(orderId);
        order.removeDeliveryDay(day);                // domain: must keep at least one day
        replaceDays(order);
    }

    public void markSent(int orderId) {
        Order order = loadOrder(orderId);
        order.markSent();
        orderDAO.update(OrderMapper.toDTO(order));
    }

    public void markReceived(int orderId) {
        Order order = loadOrder(orderId);
        order.markReceived();
        orderDAO.update(OrderMapper.toDTO(order));
    }
}
