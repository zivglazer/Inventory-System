package dataaccess.mapper;

import dataaccess.Dates;
import dataaccess.dto.DefectItemDTO;
import domain.inventory.DefectItem;
import domain.inventory.Item;

/** Translates between the domain {@link DefectItem} and a flat {@link DefectItemDTO} row. */
public final class DefectItemMapper {

    private DefectItemMapper() {}

    public static DefectItemDTO toDTO(DefectItem d) {
        return new DefectItemDTO(d.getItemId(), d.getCostPrice(),
                                 Dates.format(d.getExpirationDate()), d.getReason(),
                                 Dates.format(d.getUpdateDate()));
    }

    public static DefectItem toDomain(DefectItemDTO d) {
        Item base = new Item(d.itemId, d.costPrice, Dates.parse(d.expiration));
        return new DefectItem(base, d.reason, Dates.parse(d.updateDate));
    }
}
