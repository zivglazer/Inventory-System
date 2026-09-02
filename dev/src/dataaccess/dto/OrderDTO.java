package dataaccess.dto;

/** A flat order row. {@code type} is SHORTAGE | SCHEDULED | DISPATCHED (single-table inheritance). */
public class OrderDTO {
    public int orderId;
    public String type;
    public int supplierId;
    public String status;       // CREATED | SENT | RECEIVED
    public String orderDate;    // ISO-8601

    public OrderDTO() {}

    public OrderDTO(int orderId, String type, int supplierId, String status, String orderDate) {
        this.orderId = orderId;
        this.type = type;
        this.supplierId = supplierId;
        this.status = status;
        this.orderDate = orderDate;
    }
}
