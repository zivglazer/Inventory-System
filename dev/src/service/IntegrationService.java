package service;

import domain.inventory.Product;
import domain.orders.Order;
import domain.orders.OrderLine;
import domain.orders.OrderLineRequest;
import domain.orders.ScheduledOrder;
import domain.suppliers.Agreement;
import domain.suppliers.Supplier;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the cross-module workflows between Inventory, the Order subsystem,
 * and the (mock) Suppliers module. This is the only place that spans modules.
 */
public class IntegrationService {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final SupplierService supplierService;

    public IntegrationService(InventoryService inventoryService,
                              OrderService orderService,
                              SupplierService supplierService) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.supplierService = supplierService;
    }

    // ── Req 28 + 29: shortage orders for every product below minimum ──────────
    public Response<List<String>> autoOrderLowStock() {
        Response<List<Integer>> idsResponse = inventoryService.getProductIdsToRestock();
        if (!idsResponse.isSuccess())
            return new Response<>(idsResponse.getMessage());
        return autoOrderForProducts(idsResponse.getValue());
    }

    /**
     * Re-check a single product right after its stock changed (a sale, defect, removal or count
     * correction) and place a shortage order if it is now below its minimum. Reuses the same
     * expected-inventory netting as the full scan, so calling it repeatedly never duplicates an
     * order that is already on the way.
     */
    public Response<List<String>> autoOrderForProduct(int productId) {
        Response<Boolean> need = inventoryService.checkMinToRestock(productId);
        if (!need.isSuccess()) return new Response<>(need.getMessage());
        if (!need.getValue()) return new Response<>(new ArrayList<>(), ""); // not short -> nothing to do
        return autoOrderForProducts(Collections.singletonList(productId));
    }

    /** Shared core: place shortage orders for an explicit set of products. */
    private Response<List<String>> autoOrderForProducts(List<Integer> productIds) {
        if (productIds.isEmpty())
            return new Response<>(new ArrayList<>(), "No products need restocking");

        List<String> summary = new ArrayList<>();

        // Pass 1: decide net quantity + cheapest supplier per product, grouped by supplier.
        Map<Integer, List<OrderLineRequest>> linesBySupplier = new LinkedHashMap<>();
        Map<Integer, String> productNames = new LinkedHashMap<>();
        for (int productId : productIds) {
            Response<Product> productResponse = inventoryService.getProduct(productId);
            if (!productResponse.isSuccess()) {
                summary.add("ERROR: Could not fetch product " + productId + " - " + productResponse.getMessage());
                continue;
            }
            Product product = productResponse.getValue();
            int needed = product.restockQuantity(); // domain owns the shortage-replenishment rule

            // ExpectedInventory (derived): subtract what is already on the way on open SHORTAGE orders.
            Response<Integer> pendingResponse = orderService.getPendingQuantity(productId);
            int pending = pendingResponse.isSuccess() ? pendingResponse.getValue() : 0;
            int net = needed - pending;
            if (net <= 0) continue; // already enough on the way

            // Suppliers identify products by NAME (the universal id), not the store's productId.
            Response<Integer> supplierResponse = supplierService.getCheapestSupplier(product.getName(), net);
            if (!supplierResponse.isSuccess()) {
                summary.add("ERROR: No supplier found for product " + productId + " - " + supplierResponse.getMessage());
                continue;
            }
            int supplierId = supplierResponse.getValue();

            // agreed (bulk-aware) unit price; 0 when there is no agreement / fallback supplier
            Response<Agreement> priceResponse = supplierService.getProduct(supplierId, product.getName());
            int unitPrice = priceResponse.isSuccess() ? priceResponse.getValue().unitPriceFor(net) : 0;

            linesBySupplier.computeIfAbsent(supplierId, k -> new ArrayList<>())
                           .add(new OrderLineRequest(productId, net, unitPrice));
            productNames.put(productId, product.getName());
        }

        // Pass 2: one order per supplier, holding all of that supplier's lines.
        for (Map.Entry<Integer, List<OrderLineRequest>> entry : linesBySupplier.entrySet()) {
            int supplierId = entry.getKey();
            List<OrderLineRequest> lines = entry.getValue();

            Response<Integer> orderResponse = orderService.createShortageOrder(supplierId, lines);
            if (!orderResponse.isSuccess()) {
                summary.add("ERROR: Could not place order for supplier " + supplierId + " - " + orderResponse.getMessage());
                continue;
            }
            int orderId = orderResponse.getValue();
            orderService.markSent(orderId);

            Map<String, Integer> productQuantities = new LinkedHashMap<>();
            for (OrderLineRequest line : lines)
                productQuantities.put(productNames.get(line.getProductId()), line.getQuantity());
            supplierService.sendOrder(supplierId, productQuantities); // Req 32 (supplier speaks names)

            // one summary entry per product line
            for (OrderLineRequest line : lines)
                summary.add("Ordered " + line.getQuantity() + " x \"" + productNames.get(line.getProductId())
                        + "\" (productId=" + line.getProductId() + ") from supplierId=" + supplierId
                        + " [orderId=" + orderId + "]");
        }
        return new Response<>(summary, "Auto-order complete");
    }

    // ── Stock-reducing actions: perform the inventory change, then re-check that product ──
    // The product id is captured BEFORE the mutation, because selling/defecting/removing deletes
    // the item and the product link with it. The returned value carries any shortage-order summary
    // lines so the caller can display them; isSuccess()/message mirror the inventory mutation.
    public Response<List<String>> sellItem(int itemId) {
        Response<Integer> productId = inventoryService.getProductIdByItem(itemId);
        Response<Boolean> done = inventoryService.itemSell(itemId);
        if (!done.isSuccess()) return new Response<>(done.getMessage());
        return new Response<>(orderAfterStockDrop(productId), done.getMessage());
    }

    public Response<List<String>> markDefective(int itemId, String reason) {
        Response<Integer> productId = inventoryService.getProductIdByItem(itemId);
        Response<Boolean> done = inventoryService.setDefectiveItem(itemId, reason);
        if (!done.isSuccess()) return new Response<>(done.getMessage());
        return new Response<>(orderAfterStockDrop(productId), done.getMessage());
    }

    public Response<List<String>> removeItem(int itemId) {
        Response<Integer> productId = inventoryService.getProductIdByItem(itemId);
        Response<Boolean> done = inventoryService.removeItem(itemId);
        if (!done.isSuccess()) return new Response<>(done.getMessage());
        return new Response<>(orderAfterStockDrop(productId), done.getMessage());
    }

    /** Targeted shortage re-check for a product captured before the mutation (best-effort). */
    private List<String> orderAfterStockDrop(Response<Integer> productId) {
        if (!productId.isSuccess()) return new ArrayList<>();
        Response<List<String>> order = autoOrderForProduct(productId.getValue());
        return order.isSuccess() ? order.getValue() : new ArrayList<>();
    }

    // ── Convert scheduled templates due tomorrow into real, priced, dispatched orders ─────
    public Response<List<String>> dispatchDueScheduledOrders() {
        return dispatchDueScheduledOrders(new Date());
    }

    /**
     * For every scheduled template whose delivery day is tomorrow, create ONE real order for the
     * template's supplier: each line is priced from that supplier's agreement at conversion time
     * (never on the template), routed entirely through the template's supplier (no cheapest-supplier
     * search). The template itself is left untouched so it recurs on its next delivery day.
     */
    public Response<List<String>> dispatchDueScheduledOrders(Date today) {
        Response<List<ScheduledOrder>> all = orderService.getAllScheduledOrders();
        if (!all.isSuccess()) return new Response<>(all.getMessage());

        int todayDow = dayOfWeek(today);
        List<String> summary = new ArrayList<>();
        for (ScheduledOrder template : all.getValue()) {
            if (!template.isDueOneDayBefore(todayDow)) continue;
            if (template.getLines().isEmpty()) continue; // empty template -> nothing to order

            int supplierId = template.getSupplierId();
            List<OrderLineRequest> lines = new ArrayList<>();
            for (OrderLine line : template.getLines()) {
                int unitPrice = priceFromAgreement(supplierId, line.getProductId(), line.getQuantity());
                lines.add(new OrderLineRequest(line.getProductId(), line.getQuantity(), unitPrice));
            }

            Response<Integer> orderResponse = orderService.createDispatchedOrder(supplierId, lines);
            if (!orderResponse.isSuccess()) {
                summary.add("ERROR: could not convert template #" + template.getOrderId()
                        + " - " + orderResponse.getMessage());
                continue;
            }
            int orderId = orderResponse.getValue();
            orderService.markSent(orderId);
            supplierService.sendOrder(supplierId, quantitiesByName(lines)); // Req 32 (supplier speaks names)
            summary.add("Converted scheduled template #" + template.getOrderId()
                    + " into order #" + orderId + " for supplierId=" + supplierId
                    + " (" + lines.size() + " products)");
        }
        return new Response<>(summary, "Scheduled orders dispatched");
    }

    /** Back-compat alias: dispatching due scheduled orders now means converting their templates. */
    public Response<List<String>> sendDueScheduledOrders()            { return dispatchDueScheduledOrders(new Date()); }
    public Response<List<String>> sendDueScheduledOrders(Date today)  { return dispatchDueScheduledOrders(today); }

    // ── Req 30 + 31: receive a delivered order into inventory ─────────────────
    public Response<Boolean> receiveOrder(int orderId, List<ReceivedUnit> units) {
        Response<Order> orderResponse = orderService.getOrder(orderId);
        if (!orderResponse.isSuccess()) return new Response<>(orderResponse.getMessage());
        Order order = orderResponse.getValue();

        // The domain decides whether this delivery is admissible (sent order + products belong to it);
        // the orchestrator only asks and relays the failure.
        List<Integer> deliveredProducts = new ArrayList<>();
        for (ReceivedUnit unit : units) deliveredProducts.add(unit.productId);
        try {
            order.ensureCanReceive(deliveredProducts);
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }

        for (ReceivedUnit unit : units) {
            Response<Boolean> r = inventoryService.addNewItem(
                unit.itemId, order.getSupplierId(), unit.costPrice, unit.expirationDate, unit.productId);
            if (!r.isSuccess())
                return new Response<>("Failed to register item " + unit.itemId + " - " + r.getMessage());
        }
        orderService.markReceived(orderId);
        return new Response<>(true, "Order " + orderId + " received (" + units.size() + " items)");
    }

    /** Single delivered item, independent of an order record (kept for the simple receive path). */
    public Response<Boolean> receiveOrderItem(int itemId, int supplierId, int costPrice,
                                              Date expirationDate, int productId) {
        return inventoryService.addNewItem(itemId, supplierId, costPrice, expirationDate, productId);
    }

    // ── Req 19: create a scheduled order, only for a real supplier and its own delivery days ───
    public Response<Integer> addScheduledOrder(int supplierID, List<Integer> deliveryDays) {
        // 1. the supplier must exist (have an active contract)
        Response<Supplier> supplierResponse = supplierService.getSupplier(supplierID);
        if (!supplierResponse.isSuccess()) return new Response<>(supplierResponse.getMessage());

        // 2. every requested delivery day must be one of the supplier's own days (and at least one)
        Response<List<Integer>> daysResponse = supplierService.getDeliveryDays(supplierID);
        if (!daysResponse.isSuccess()) return new Response<>(daysResponse.getMessage());
        if (deliveryDays == null || deliveryDays.isEmpty())
            return new Response<>("At least one delivery day is required");
        for (int day : deliveryDays)
            if (!daysResponse.getValue().contains(day))
                return new Response<>("Day " + day + " is not a delivery day of supplier " + supplierID);

        // 3. both validations passed -> delegate to the unchanged Orders-module flow
        return orderService.addScheduledOrder(supplierID, deliveryDays);
    }

    // ── Add a product to a scheduled template: catalog-checked, no duplicates, UNPRICED ──
    public Response<Boolean> addProductToOrder(int orderId, int productId, int quantity) {
        // 1. which supplier does this order belong to?
        Response<Integer> supplierResponse = orderService.getOrderSupplier(orderId);
        if (!supplierResponse.isSuccess()) return new Response<>(supplierResponse.getMessage());
        int supplierId = supplierResponse.getValue();

        // 2. a product may not appear twice in a scheduled template
        Response<Boolean> contains = orderService.containInOrder(orderId, productId);
        if (!contains.isSuccess()) return new Response<>(contains.getMessage());
        if (contains.getValue())
            return new Response<>("Product " + productId + " is already on order " + orderId
                    + " - use edit quantity instead");

        // 3. the template's supplier must carry this product -- matched by its universal NAME,
        //    so the product must exist in inventory to resolve that name.
        Response<Product> prod = inventoryService.getProduct(productId);
        if (!prod.isSuccess()) return new Response<>("Product " + productId + " does not exist");
        String productName = prod.getValue().getName();
        Response<Boolean> inCatalog = supplierService.containInCatalog(supplierId, productName);
        if (!inCatalog.isSuccess()) return new Response<>(inCatalog.getMessage());
        if (!inCatalog.getValue())
            return new Response<>("Supplier " + supplierId + " does not carry product '" + productName + "'");

        // 4. prices are NOT computed on the template; the line stays unpriced (0) until the template
        //    is converted into a real order, at which point it is priced from the supplier agreement.
        return orderService.addProductToScheduledOrder(orderId, productId, quantity, 0);
    }

    // ── Req 26: add a delivery day, only if it's one of the supplier's days ───
    public Response<Boolean> addDeliveryDayToScheduledOrder(int orderId, int day) {
        Response<ScheduledOrder> orderResponse = orderService.getScheduledOrder(orderId);
        if (!orderResponse.isSuccess()) return new Response<>(orderResponse.getMessage());

        Response<List<Integer>> daysResponse = supplierService.getDeliveryDays(orderResponse.getValue().getSupplierId());
        if (!daysResponse.isSuccess()) return new Response<>(daysResponse.getMessage());
        if (!daysResponse.getValue().contains(day))
            return new Response<>("Day " + day + " is not a delivery day of this supplier");

        return orderService.addDeliveryDayToScheduledOrder(orderId, day);
    }

    // ── Req 21: supplier details for display ──────────────────────────────────
    public Response<Supplier> getSupplier(int supplierId) {
        return supplierService.getSupplier(supplierId);
    }

    /**
     * The product's universal NAME for a store productId, for display. Orders store the store's
     * internal productId; this resolves it to the name shared across modules (or fails if the
     * product is no longer in inventory).
     */
    public Response<String> getProductName(int productId) {
        Response<Product> p = inventoryService.getProduct(productId);
        return p.isSuccess() ? new Response<>(p.getValue().getName(), "") : new Response<>(p.getMessage());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** The universal product name for a store productId, or null if it is not in inventory. */
    private String productNameOf(int productId) {
        Response<Product> p = inventoryService.getProduct(productId);
        return p.isSuccess() ? p.getValue().getName() : null;
    }

    /** Bulk-aware agreed unit price from the supplier (by name); 0 when unknown/uncovered. */
    private int priceFromAgreement(int supplierId, int productId, int quantity) {
        String name = productNameOf(productId);
        if (name == null) return 0;
        Response<Agreement> r = supplierService.getProduct(supplierId, name);
        return r.isSuccess() ? r.getValue().unitPriceFor(quantity) : 0;
    }

    /** Order lines as a name->quantity map for the supplier (which identifies products by name). */
    private Map<String, Integer> quantitiesByName(List<OrderLineRequest> lines) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (OrderLineRequest line : lines) {
            String name = productNameOf(line.getProductId());
            if (name != null) map.put(name, line.getQuantity());
        }
        return map;
    }

    private static int dayOfWeek(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DAY_OF_WEEK); // Sunday=1 .. Saturday=7
    }

    /** A physical unit arriving in a delivery (barcode + product + cost + expiry). */
    public static class ReceivedUnit {
        public final int itemId;
        public final int productId;
        public final int costPrice;
        public final Date expirationDate;

        public ReceivedUnit(int itemId, int productId, int costPrice, Date expirationDate) {
            this.itemId = itemId;
            this.productId = productId;
            this.costPrice = costPrice;
            this.expirationDate = expirationDate;
        }
    }
}
