package domain.orders;

import java.util.Date;
import java.util.List;

/**
 * A concrete, dispatched order produced when a {@link ScheduledOrder} comes due and is
 * converted by the scheduling mechanism. Unlike the template it is priced (prices are computed at
 * conversion time, never on the template) and unlike a {@link ShortageOrder} it is NOT counted as
 * expected inventory -- scheduled and automatic procurement stay independent.
 */
public class DispatchedOrder extends Order {

    public DispatchedOrder(int orderId, int supplierId) {
        super(orderId, supplierId);
    }

    /** Rebuild from persisted state (used only by the data-access layer). */
    public static DispatchedOrder rehydrate(int orderId, int supplierId, OrderStatus status,
            Date date, List<OrderLine> lines) {
        DispatchedOrder o = new DispatchedOrder(orderId, supplierId);
        o.restore(status, date, lines);
        return o;
    }

    @Override
    public String getType() { return "Dispatched"; }
}
