package domain.orders;

/**
 * A request to add one product line to an order: which product, how many, and the agreed
 * unit price. A plain value object so the orchestrator can build a multi-line order in one
 * call without leaking data-access DTOs above the controller. Invariants (quantity &gt; 0,
 * price &gt;= 0) are enforced by {@link OrderLine} when the line is actually added.
 */
public class OrderLineRequest {
    private final int productId;
    private final int quantity;
    private final int unitPrice;

    public OrderLineRequest(int productId, int quantity, int unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public int getQuantity()  { return quantity; }
    public int getUnitPrice() { return unitPrice; }
}
