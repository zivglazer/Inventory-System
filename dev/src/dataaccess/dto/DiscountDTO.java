package dataaccess.dto;

/**
 * A flat discount row. Exactly one of {@code productId} / {@code categoryName} is set,
 * marking whether the discount belongs to a product or a whole category.
 */
public class DiscountDTO {
    public Integer id;            // null before insert (AUTOINCREMENT)
    public Integer productId;     // null for a category discount
    public String categoryName;   // null for a product discount
    public int percentage;
    public String startDate;      // ISO-8601
    public String endDate;        // ISO-8601

    public DiscountDTO() {}

    public DiscountDTO(Integer productId, String categoryName, int percentage,
                       String startDate, String endDate) {
        this.productId = productId;
        this.categoryName = categoryName;
        this.percentage = percentage;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
