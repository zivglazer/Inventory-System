package dataaccess.mapper;

import dataaccess.dto.DiscountDTO;
import dataaccess.dto.ItemDTO;
import dataaccess.dto.ProductDTO;
import domain.inventory.Discounts;
import domain.inventory.Item;
import domain.inventory.Product;

import java.util.ArrayList;
import java.util.List;

/** Translates between the rich domain {@link Product} aggregate and its flat rows. */
public final class ProductMapper {

    private ProductMapper() {}

    public static ProductDTO toDTO(Product p, String categoryName) {
        return new ProductDTO(p.getProductId(), p.getName(), p.getManufacturer(), categoryName,
                              p.getSubCategory(), p.getSubSubCategory(), p.getShelfLocationId(),
                              p.getSupplierId(), p.getMinToRestock(), p.getPriceWithoutDiscount());
    }

    /** Rebuild the full Product aggregate: route each item into its list and re-attach discounts. */
    public static Product toDomain(ProductDTO p, List<ItemDTO> items, List<DiscountDTO> discounts) {
        List<Item> shelf = new ArrayList<>();
        List<Item> storage = new ArrayList<>();
        List<Item> expired = new ArrayList<>();
        for (ItemDTO i : items) {
            Item item = ItemMapper.toDomain(i);
            if (ItemMapper.SHELF.equals(i.locationState)) shelf.add(item);
            else if (ItemMapper.EXPIRED.equals(i.locationState)) expired.add(item);
            else storage.add(item);
        }
        Discounts d = new Discounts();
        for (DiscountDTO dto : discounts) d.addDiscount(DiscountMapper.toDomain(dto));

        return Product.rehydrate(p.productId, p.supplierId, p.name, p.manufacturer,
                                 p.subCategory, p.subSubCategory, p.shelfLocationId,
                                 p.priceWithoutDiscount, p.minToRestock, shelf, storage, expired, d);
    }
}
