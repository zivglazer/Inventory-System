package dataaccess.dto;

/** A flat price-agreement row. */
public class AgreementDTO {
    public int supplierId;
    public String productName;
    public int unitPrice;
    public int bulkThreshold;   // 0 = no bulk tier
    public int bulkUnitPrice;

    public AgreementDTO() {}

    public AgreementDTO(int supplierId, String productName, int unitPrice, int bulkThreshold, int bulkUnitPrice) {
        this.supplierId = supplierId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.bulkThreshold = bulkThreshold;
        this.bulkUnitPrice = bulkUnitPrice;
    }
}
