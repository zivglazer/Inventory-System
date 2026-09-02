package domain.suppliers;

/**
 * A supplier's price agreement (כתב כמויות) for one product, with an optional bulk tier:
 * at or above {@code bulkThreshold} units the {@code bulkUnitPrice} applies. This is what
 * makes "cheapest supplier for a product AND quantity" (Req 29) a real comparison.
 */
public class Agreement {
    private final int supplierId;
    private final String productName; // the universal cross-module product identifier (not the store's id)
    private final int unitPrice;
    private final int bulkThreshold; // 0 = no bulk tier
    private final int bulkUnitPrice;

    public Agreement(int supplierId, String productName, int unitPrice) {
        this(supplierId, productName, unitPrice, 0, unitPrice);
    }

    public Agreement(int supplierId, String productName, int unitPrice, int bulkThreshold, int bulkUnitPrice) {
        if (unitPrice < 0 || bulkUnitPrice < 0)
            throw new IllegalArgumentException("Price cannot be negative");
        if (productName == null)
            throw new IllegalArgumentException("Product name cannot be null");
        this.supplierId = supplierId;
        this.productName = productName.toLowerCase(); // names are the universal id, normalised lower-case
        this.unitPrice = unitPrice;
        this.bulkThreshold = bulkThreshold;
        this.bulkUnitPrice = bulkUnitPrice;
    }

    public int getSupplierId()    { return supplierId; }
    public String getProductName() { return productName; }
    public int getUnitPrice()     { return unitPrice; }
    public int getBulkThreshold() { return bulkThreshold; }
    public int getBulkUnitPrice() { return bulkUnitPrice; }

    /** Per-unit price for the given quantity, applying the bulk tier when it kicks in. */
    public int unitPriceFor(int quantity) {
        return (bulkThreshold > 0 && quantity >= bulkThreshold) ? bulkUnitPrice : unitPrice;
    }

    /** Total price for the given quantity, applying the bulk tier when it kicks in. */
    public int totalPriceFor(int quantity) {
        return unitPriceFor(quantity) * quantity;
    }
}
