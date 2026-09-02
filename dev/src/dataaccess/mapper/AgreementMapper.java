package dataaccess.mapper;

import dataaccess.dto.AgreementDTO;
import domain.suppliers.Agreement;

/** Translates between the domain {@link Agreement} and a flat {@link AgreementDTO} row. */
public final class AgreementMapper {

    private AgreementMapper() {}

    public static AgreementDTO toDTO(Agreement a) {
        return new AgreementDTO(a.getSupplierId(), a.getProductName(), a.getUnitPrice(),
                                a.getBulkThreshold(), a.getBulkUnitPrice());
    }

    public static Agreement toDomain(AgreementDTO d) {
        return new Agreement(d.supplierId, d.productName, d.unitPrice, d.bulkThreshold, d.bulkUnitPrice);
    }
}
