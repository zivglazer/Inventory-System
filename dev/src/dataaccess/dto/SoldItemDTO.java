package dataaccess.dto;

/** A flat sold-item row. */
public class SoldItemDTO {
    public int itemId;
    public String sellDate;     // ISO-8601
    public double sellPrice;
    public int costPrice;

    public SoldItemDTO() {}

    public SoldItemDTO(int itemId, String sellDate, double sellPrice, int costPrice) {
        this.itemId = itemId;
        this.sellDate = sellDate;
        this.sellPrice = sellPrice;
        this.costPrice = costPrice;
    }
}
