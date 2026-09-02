package dataaccess.mapper;

import dataaccess.Dates;
import dataaccess.dto.ItemDTO;
import domain.inventory.Item;

/** Translates between the domain {@link Item} and a flat {@link ItemDTO} row. */
public final class ItemMapper {

    public static final String STORAGE = "STORAGE";
    public static final String SHELF   = "SHELF";
    public static final String EXPIRED = "EXPIRED";

    private ItemMapper() {}

    public static ItemDTO toDTO(Item item, int productId, String locationState) {
        return new ItemDTO(item.getItemId(), productId, item.getCostPrice(),
                           Dates.format(item.getExpirationDate()), locationState);
    }

    public static Item toDomain(ItemDTO d) {
        return new Item(d.itemId, d.costPrice, Dates.parse(d.expiration));
    }
}
