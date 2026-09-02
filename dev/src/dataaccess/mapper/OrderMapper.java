package dataaccess.mapper;

import dataaccess.Dates;
import dataaccess.dto.OrderDTO;
import dataaccess.dto.OrderLineDTO;
import domain.orders.Order;
import domain.orders.OrderLine;
import domain.orders.OrderStatus;
import domain.orders.ScheduledOrder;
import domain.orders.DispatchedOrder;
import domain.orders.ShortageOrder;

import java.util.ArrayList;
import java.util.List;

/** Translates between the domain {@link Order} aggregate and its flat rows. */
public final class OrderMapper {

    public static final String SHORTAGE   = "SHORTAGE";
    public static final String SCHEDULED  = "SCHEDULED";  // a recurring ScheduledOrder template
    public static final String DISPATCHED = "DISPATCHED"; // a template materialised into a real, sent order

    private OrderMapper() {}

    public static OrderDTO toDTO(Order o) {
        String type = SHORTAGE;
        if (o instanceof ScheduledOrder)        type = SCHEDULED;
        else if (o instanceof DispatchedOrder)  type = DISPATCHED;
        return new OrderDTO(o.getOrderId(), type, o.getSupplierId(),
                            o.getStatus().name(), Dates.format(o.getDate()));
    }

    /** Rebuild the order (and its lines) from rows. {@code days} is used only for scheduled orders. */
    public static Order toDomain(OrderDTO o, List<OrderLineDTO> lineRows, List<Integer> days) {
        List<OrderLine> lines = new ArrayList<>();
        for (OrderLineDTO l : lineRows) lines.add(new OrderLine(l.productId, l.quantity, l.unitPrice));

        OrderStatus status = OrderStatus.valueOf(o.status);
        if (SCHEDULED.equals(o.type))
            return ScheduledOrder.rehydrate(o.orderId, o.supplierId, status, Dates.parse(o.orderDate), lines, days);
        if (DISPATCHED.equals(o.type))
            return DispatchedOrder.rehydrate(o.orderId, o.supplierId, status, Dates.parse(o.orderDate), lines);
        return ShortageOrder.rehydrate(o.orderId, o.supplierId, status, Dates.parse(o.orderDate), lines);
    }
}
