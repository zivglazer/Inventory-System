package service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Daily scheduling mechanism: once per day at a fixed time (default 17:49) it asks the
 * {@link IntegrationService} to convert every scheduled template whose delivery day is tomorrow into
 * a real, dispatched order. The time-of-day arithmetic and the conversion core ({@link #runNow()})
 * are unit-testable; {@link #start()} is only the thin daemon-timer wrapper around them.
 */
public class ScheduledOrderRunner {
    public static final int DEFAULT_HOUR   = 17;
    public static final int DEFAULT_MINUTE = 49;

    private final IntegrationService integrationService;
    private final int hour;
    private final int minute;
    private ScheduledExecutorService scheduler;

    public ScheduledOrderRunner(IntegrationService integrationService) {
        this(integrationService, DEFAULT_HOUR, DEFAULT_MINUTE);
    }

    public ScheduledOrderRunner(IntegrationService integrationService, int hour, int minute) {
        this.integrationService = integrationService;
        this.hour = hour;
        this.minute = minute;
    }

    /** The work performed each day; also callable directly (manual trigger / tests). */
    public Response<List<String>> runNow() {
        return integrationService.dispatchDueScheduledOrders(new Date());
    }

    /** Start a daemon timer that fires {@link #runNow()} every day at the configured time. */
    public synchronized void start() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scheduled-order-runner");
            t.setDaemon(true);
            return t;
        });
        long initialDelay = millisUntilNextRun(new Date());
        scheduler.scheduleAtFixedRate(this::runNow, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    /** Milliseconds from {@code from} until the next occurrence of the configured time of day. */
    public long millisUntilNextRun(Date from) {
        Calendar next = Calendar.getInstance();
        next.setTime(from);
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.getTime().after(from))
            next.add(Calendar.DAY_OF_MONTH, 1); // already past today -> schedule for tomorrow
        return next.getTimeInMillis() - from.getTime();
    }
}
