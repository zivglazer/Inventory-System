package domain.inventory;

import java.util.Date;

public class SoldItem {
    private double sellPrice;
    private int costPrice;
    private int itemId;
    private Date sellDate;

    public SoldItem(int itemId, Date sellDate, double sellPrice, int costPrice) {
        this.itemId = itemId;
        this.sellDate = sellDate;
        this.sellPrice = sellPrice;
        this.costPrice = costPrice;
    }

    public int getItemId() { return itemId; }
    public double getSellPrice() { return sellPrice; }
    public int getCostPrice() { return costPrice; }
    public Date getSellDate() { return sellDate; }

    public boolean inRangeDates(Date startDate, Date endDate) {
        return !sellDate.before(startDate) && !sellDate.after(endDate);
    }
}
