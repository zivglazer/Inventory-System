package domain.reports;

import domain.inventory.Product;
import domain.inventory.ShelfLocation;
import java.util.Date;
import java.util.List;

/** Snapshot of current inventory, optionally filtered by category list */
public class InventoryReport extends Report<Product> {
    private List<String> categories;

    public InventoryReport(int reportId, List<Product> data, List<String> categories, Date publishDate) {
        super(reportId, data, publishDate);
        this.categories = categories;
    }

    public List<String> getCategories() { return categories; }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Inventory Report - ").append(DATE_FORMAT.format(getPublishDate())).append("\n");
        if (categories != null && !categories.isEmpty()) {
            sb.append("Categories: ").append(String.join(", ", categories)).append("\n");
        }
        sb.append("Products:\n");
        for (Product p : getData()) {
            sb.append("- ").append(p.getName())
              .append(" (ID: ").append(p.getProductId()).append(")")
              .append(" sub category: ").append(p.getSubCategory())
              .append(" subsub category: ").append(p.getSubSubCategory())
              .append(" | Shelf: ").append(p.getShelfQuantity())
              .append(", Storage: ").append(p.getStorageItemsQuantity())
              .append(", Total: ").append(p.getTotalQuantity())
              .append(" | Min To Restock: ").append(p.getMinToRestock())
              .append(" | Location: ").append(p.getShelfLocation().getLabel())
              .append("\n");
        }
        return sb.toString();
    }
}
