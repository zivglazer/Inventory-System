package domain.reports;

import domain.inventory.Product;
import java.util.Date;
import java.util.List;

/** Contains products that are below minQuantity — what needs to be ordered */
public class OrderReport extends Report<Product> {

    public OrderReport(int reportId, List<Product> data, Date publishDate) {
        super(reportId, data, publishDate);
    }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Order Report - ").append(DATE_FORMAT.format(getPublishDate())).append("\n");
        sb.append("Products to restock:\n");
        for (Product p : getData()) {
            sb.append("- ").append(p.getName())
              .append(" (ID: ").append(p.getProductId()).append(")")
              .append(" - current: ").append(p.getTotalQuantity())
              .append(", minimum: ").append(p.getMinToRestock())
              .append("\n");
        }
        return sb.toString();
    }
}
