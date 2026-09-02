package domain.orders;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A recurring order <b>template</b> tied to a supplier's fixed delivery days (1=Sunday .. 7=Saturday).
 * It is never sent on its own and is left unpriced; instead, one day before each delivery day the
 * scheduling mechanism converts it into a concrete {@link DispatchedOrder} (see
 * {@code IntegrationService.dispatchDueScheduledOrders}). The template stays in place so it recurs.
 */
public class ScheduledOrder extends Order {
    private final List<Integer> deliveryDays;

    public ScheduledOrder(int orderId, int supplierId, List<Integer> deliveryDays) {
        super(orderId, supplierId);
        if (deliveryDays == null || deliveryDays.isEmpty())
            throw new IllegalArgumentException("A scheduled order needs at least one delivery day");
        this.deliveryDays = new ArrayList<>(new LinkedHashSet<>(deliveryDays));
        for (int day : this.deliveryDays) validateDay(day);
    }

    /** Rebuild from persisted state (used only by the data-access layer). */
    public static ScheduledOrder rehydrate(int orderId, int supplierId, OrderStatus status,
            Date date, List<OrderLine> lines, List<Integer> deliveryDays) {
        ScheduledOrder o = new ScheduledOrder(orderId, supplierId, deliveryDays);
        o.restore(status, date, lines);
        return o;
    }

    public List<Integer> getDeliveryDays() { return deliveryDays; }

    public void addDeliveryDay(int day) {
        validateDay(day);
        if (!deliveryDays.contains(day))
            deliveryDays.add(day);
    }

    /** Req 25: a scheduled order must keep at least one delivery day. */
    public void removeDeliveryDay(int day) {
        if (deliveryDays.size() <= 1)
            throw new IllegalStateException("A scheduled order must keep at least one delivery day");
        if (!deliveryDays.remove((Integer) day))
            throw new IllegalArgumentException("Day " + day + " is not a delivery day of order " + orderId);
    }

    /** True if a delivery day falls on the day AFTER today (orders are sent one day before delivery). */
    public boolean isDueOneDayBefore(int todayDayOfWeek) {
        int tomorrow = todayDayOfWeek % 7 + 1; // 7 (Sat) wraps to 1 (Sun)
        return deliveryDays.contains(tomorrow);
    }

    private void validateDay(int day) {
        if (day < 1 || day > 7)
            throw new IllegalArgumentException("Delivery day must be 1..7 (got " + day + ")");
    }

    @Override
    public String getType() { return "Scheduled"; }
}
