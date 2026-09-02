package dataaccess.mapper;

import dataaccess.dto.SupplierDTO;
import domain.suppliers.Agreement;
import domain.suppliers.Supplier;

import java.util.List;

/** Translates between the domain {@link Supplier} and a flat {@link SupplierDTO} row. */
public final class SupplierMapper {

    private SupplierMapper() {}

    public static SupplierDTO toDTO(Supplier s) {
        return new SupplierDTO(s.getSupplierId(), s.getName(), s.getAddress(),
                               s.getPhone(), s.getContactPerson());
    }

    public static Supplier toDomain(SupplierDTO d, List<Integer> deliveryDays, List<Agreement> catalog) {
        return new Supplier(d.supplierId, d.name, d.address, d.phone, d.contact, deliveryDays, catalog);
    }
}
