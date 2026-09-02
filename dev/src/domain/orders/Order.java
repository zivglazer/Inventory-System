package domain.orders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A purchase order placed with a supplier. Abstract base for the two order kinds. */
public abstract class Order {
    protected final int orderId;
    protected final int supplierId;
    protected OrderStatus status;
    protected Date date;
    protected final List<OrderLine> lines;

    protected Order(int orderId, int supplierId) {
        this.orderId = orderId;
        this.supplierId = supplierId;
        this.status = OrderStatus.CREATED;
        this.date = new Date();
        this.lines = new ArrayList<>();
    }

    public int getOrderId()           { return orderId; }
    public int getSupplierId()        { return supplierId; }
    public OrderStatus getStatus()    { return status; }
    public Date getDate()             { return date; }
    public List<OrderLine> getLines() { return lines; }

    /** Add a product line, merging into an existing line for the same product (keeping its price). */
    public void addLine(int productId, int quantity, int unitPrice) {
        for (OrderLine line : lines)
            if (line.getProductId() == productId) {
                line.setQuantity(line.getQuantity() + quantity);
                return;
            }
        lines.add(new OrderLine(productId, quantity, unitPrice));
    }

    /** True if a line for this product already exists on the order. */
    public boolean containsProduct(int productId) {
        for (OrderLine line : lines)
            if (line.getProductId() == productId) return true;
        return false;
    }

    public void setLineQuantity(int productId, int quantity) {
        for (OrderLine line : lines)
            if (line.getProductId() == productId) {
                line.setQuantity(quantity);
                return;
            }
        throw new IllegalArgumentException("Product " + productId + " is not in order " + orderId);
    }

    public void markSent()     { this.status = OrderStatus.SENT; this.date = new Date(); }

    public void markReceived() {
        if (status != OrderStatus.SENT)
            throw new IllegalStateException("Order " + orderId + " cannot be received from status " + status);
        this.status = OrderStatus.RECEIVED;
    }

    /**
     * Domain rule for receiving a delivery: the order must have been sent to the supplier, and every
     * delivered product must be one of the order's lines (inventory can only be replenished through a
     * genuine, placed supplier order). Throws if either condition is violated.
     */
    public void ensureCanReceive(Collection<Integer> deliveredProductIds) {
        if (status != OrderStatus.SENT)
            throw new IllegalStateException("Order " + orderId + " is not awaiting delivery (status " + status + ")");
        for (int productId : deliveredProductIds)
            if (!containsProduct(productId))
                throw new IllegalArgumentException("Product " + productId + " is not part of order " + orderId);
    }

    /** Re-apply persisted state after construction (used by the subclass rehydrate factories). */
    protected void restore(OrderStatus status, Date date, List<OrderLine> restoredLines) {
        this.status = status;
        this.date = date;
        this.lines.clear();
        this.lines.addAll(restoredLines);
    }

    /** product -> quantity view of the order's lines (used when notifying the supplier). */
    public Map<Integer, Integer> asProductQuantities() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (OrderLine line : lines)
            map.put(line.getProductId(), line.getQuantity());
        return map;
    }

    public abstract String getType();
}
