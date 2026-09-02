package dataaccess.dto;

/** A flat product row. No behavior — just the columns of the {@code product} table. */
public class ProductDTO {
    public int productId;
    public String name;
    public String manufacturer;
    public String categoryName;
    public String subCategory;
    public String subSubCategory;
    public int shelfLocationId;
    public int supplierId;
    public int minToRestock;
    public int priceWithoutDiscount;

    public ProductDTO() {}

    public ProductDTO(int productId, String name, String manufacturer, String categoryName,
                      String subCategory, String subSubCategory, int shelfLocationId,
                      int supplierId, int minToRestock, int priceWithoutDiscount) {
        this.productId = productId;
        this.name = name;
        this.manufacturer = manufacturer;
        this.categoryName = categoryName;
        this.subCategory = subCategory;
        this.subSubCategory = subSubCategory;
        this.shelfLocationId = shelfLocationId;
        this.supplierId = supplierId;
        this.minToRestock = minToRestock;
        this.priceWithoutDiscount = priceWithoutDiscount;
    }
}
