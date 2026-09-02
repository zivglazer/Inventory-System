package domain.orders;

/** Lifecycle of a purchase order placed with a supplier. */
public enum OrderStatus {
    CREATED,   // built, not yet sent to the supplier
    SENT,      // issued to the supplier
    RECEIVED   // goods arrived and were registered in inventory
}
