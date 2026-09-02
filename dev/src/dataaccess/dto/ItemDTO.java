package dataaccess.dto;

/** A flat item row. {@code locationState} is STORAGE | SHELF | EXPIRED. */
public class ItemDTO {
    public int itemId;
    public int productId;
    public int costPrice;
    public String expiration;     // ISO-8601
    public String locationState;

    public ItemDTO() {}

    public ItemDTO(int itemId, int productId, int costPrice, String expiration, String locationState) {
        this.itemId = itemId;
        this.productId = productId;
        this.costPrice = costPrice;
        this.expiration = expiration;
        this.locationState = locationState;
    }
}
