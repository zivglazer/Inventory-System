package dataaccess.dto;

/** A flat order-line row. */
public class OrderLineDTO {
    public int orderId;
    public int productId;
    public int quantity;
    public int unitPrice;

    public OrderLineDTO() {}

    public OrderLineDTO(int orderId, int productId, int quantity, int unitPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
