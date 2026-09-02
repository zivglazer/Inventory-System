package domain.reports;

import domain.inventory.Item;
import java.util.Date;
import java.util.List;

public class ExpiredReport extends Report<Item> {
    private Date startDate;
    private Date endDate;

    public ExpiredReport(int reportId, List<Item> data, Date startDate, Date endDate) {
        super(reportId, data, new Date());
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Expired Items Report - ").append(DATE_FORMAT.format(getPublishDate())).append("\n");
        sb.append("Period: ").append(DATE_FORMAT.format(startDate)).append(" to ").append(DATE_FORMAT.format(endDate)).append("\n");
        sb.append("Expired items:\n");
        for (Item item : getData()) {
            sb.append("- Barcode: ").append(item.getItemId())
              .append(" | Expired: ").append(DATE_FORMAT.format(item.getExpirationDate()))
              .append("\n");
        }
        return sb.toString();
    }
}