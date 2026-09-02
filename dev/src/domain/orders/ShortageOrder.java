package domain.orders;

import java.util.Date;
import java.util.List;

/** A one-off order issued because a product fell below its minimum stock. */
public class ShortageOrder extends Order {

    public ShortageOrder(int orderId, int supplierId) {
        super(orderId, supplierId);
    }

    /** Rebuild from persisted state (used only by the data-access layer). */
    public static ShortageOrder rehydrate(int orderId, int supplierId, OrderStatus status,
            Date date, List<OrderLine> lines) {
        ShortageOrder o = new ShortageOrder(orderId, supplierId);
        o.restore(status, date, lines);
        return o;
    }

    @Override
    public String getType() { return "Shortage"; }
}
