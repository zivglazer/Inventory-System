package domain.reports;

import domain.inventory.DefectItem;
import java.util.Date;
import java.util.List;

public class DefectReport extends Report<DefectItem> {
    private Date startDate;
    private Date endDate;

    public DefectReport(int reportId, List<DefectItem> data, Date startDate, Date endDate) {
        super(reportId, data, new Date());
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Defect Report - ").append(DATE_FORMAT.format(getPublishDate())).append("\n");
        sb.append("Period: ").append(DATE_FORMAT.format(startDate)).append(" to ").append(DATE_FORMAT.format(endDate)).append("\n");
        sb.append("Defective items:\n");
        for (DefectItem item : getData()) {
            sb.append("- Barcode: ").append(item.getItemId())
              .append(" | Reason: ").append(item.getReason())
              .append(" | Reported: ").append(DATE_FORMAT.format(item.getUpdateDate()))
              .append("\n");
        }
        return sb.toString();
    }
}
