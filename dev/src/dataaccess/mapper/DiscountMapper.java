package dataaccess.mapper;

import dataaccess.Dates;
import dataaccess.dto.DiscountDTO;
import domain.inventory.DiscountInfo;

/** Translates between the domain {@link DiscountInfo} and a flat {@link DiscountDTO} row. */
public final class DiscountMapper {

    private DiscountMapper() {}

    public static DiscountDTO toProductDTO(int productId, DiscountInfo info) {
        return new DiscountDTO(productId, null, info.getPercentage(),
                               Dates.format(info.getStartDate()), Dates.format(info.getEndDate()));
    }

    public static DiscountDTO toCategoryDTO(String categoryName, DiscountInfo info) {
        return new DiscountDTO(null, categoryName, info.getPercentage(),
                               Dates.format(info.getStartDate()), Dates.format(info.getEndDate()));
    }

    public static DiscountInfo toDomain(DiscountDTO d) {
        return new DiscountInfo(d.percentage, Dates.parse(d.startDate), Dates.parse(d.endDate));
    }
}
