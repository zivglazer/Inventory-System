package domain.inventory;

import java.util.Date;

public class Item {
    private int itemId;        // barcode
    private int costPrice;
    private Date expirationDate;

    public Item(int itemId, int price, Date expirationDate) {
        if (expirationDate == null)
            throw new IllegalArgumentException("Expiration date cannot be null");
        if (price < 0)
            throw new IllegalArgumentException("Cost price cannot be negative");
        this.itemId = itemId;
        this.costPrice = price;
        this.expirationDate = expirationDate;
    }
    public Item(Item item) {
        this.itemId = item.getItemId();
        this.costPrice = item.getCostPrice();
        this.expirationDate = item.getExpirationDate();
    }
    
    public int getItemId() { return itemId; }
    public int getCostPrice() { return costPrice; }
    public Date getExpirationDate() { return expirationDate; }
    public boolean isExpired() { return !expirationDate.after(new Date()); }
    public SoldItem sell(Date sellDate, double sellPrice) {
        if (isExpired()) throw new IllegalStateException("Cannot sell expired item");
        return new SoldItem(itemId, sellDate, sellPrice, costPrice);
    }
    public DefectItem markDefective(String reason) {
        return new DefectItem(this, reason);
    }
}
