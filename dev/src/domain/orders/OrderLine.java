package domain.orders;

/** A single product + quantity line within an order, with the supplier's agreed unit price. */
public class OrderLine {
    private final int productId;
    private int quantity;
    private final int unitPrice;

    public OrderLine(int productId, int quantity, int unitPrice) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        if (unitPrice < 0)
            throw new IllegalArgumentException("Unit price cannot be negative");
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getProductId() { return productId; }
    public int getQuantity()  { return quantity; }
    public int getUnitPrice() { return unitPrice; }

    public void setQuantity(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        this.quantity = quantity;
    }
}
