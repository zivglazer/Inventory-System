package domain.inventory;

import java.util.Date;

public class DiscountInfo {
    private int percentage;
    private Date startDate;
    private Date endDate;

    public DiscountInfo(int percentage, Date startDate, Date endDate) {
        if (percentage < 0 || percentage > 100)
            throw new IllegalArgumentException("Precantage must be between 0 and 100");
        if (startDate == null || endDate == null)
            throw new IllegalArgumentException("Dates cannot be null");
        if (startDate.after(endDate))
            throw new IllegalArgumentException("Start date must be before end date");
        this.percentage = percentage;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getPercentage() { return percentage; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public boolean isActiveOn(Date date) { return !date.before(startDate) && !date.after(endDate); }
}
