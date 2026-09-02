package dataaccess.mapper;

import dataaccess.Dates;
import dataaccess.dto.SoldItemDTO;
import domain.inventory.SoldItem;

/** Translates between the domain {@link SoldItem} and a flat {@link SoldItemDTO} row. */
public final class SoldItemMapper {

    private SoldItemMapper() {}

    public static SoldItemDTO toDTO(SoldItem s) {
        return new SoldItemDTO(s.getItemId(), Dates.format(s.getSellDate()),
                               s.getSellPrice(), s.getCostPrice());
    }

    public static SoldItem toDomain(SoldItemDTO d) {
        return new SoldItem(d.itemId, Dates.parse(d.sellDate), d.sellPrice, d.costPrice);
    }
}
