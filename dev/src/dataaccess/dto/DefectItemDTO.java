package dataaccess.dto;

/** A flat defect-item row. */
public class DefectItemDTO {
    public int itemId;
    public int costPrice;
    public String expiration;   // ISO-8601
    public String reason;
    public String updateDate;   // ISO-8601, when it was marked defective

    public DefectItemDTO() {}

    public DefectItemDTO(int itemId, int costPrice, String expiration, String reason, String updateDate) {
        this.itemId = itemId;
        this.costPrice = costPrice;
        this.expiration = expiration;
        this.reason = reason;
        this.updateDate = updateDate;
    }
}
