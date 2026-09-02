package domain.suppliers;

import java.util.ArrayList;
import java.util.List;

/** A supplier the supermarket can order from. (Mock module standing in for the supplier team's domain.) */
public class Supplier {
    private final int supplierId;
    private final String name;
    private final String address;
    private final String phone;
    private final String contactPerson;
    private final List<Integer> deliveryDays; // 1=Sunday .. 7=Saturday
    private final List<Agreement> catalog;    // the products this supplier carries (with prices)

    /** Build a supplier with an empty catalog (agreements attached separately when persisting). */
    public Supplier(int supplierId, String name, String address, String phone,
                    String contactPerson, List<Integer> deliveryDays) {
        this(supplierId, name, address, phone, contactPerson, deliveryDays, new ArrayList<>());
    }

    /** Build a supplier together with its catalog (used when loading from persistence). */
    public Supplier(int supplierId, String name, String address, String phone,
                    String contactPerson, List<Integer> deliveryDays, List<Agreement> catalog) {
        this.supplierId = supplierId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.contactPerson = contactPerson;
        this.deliveryDays = new ArrayList<>(deliveryDays);
        this.catalog = new ArrayList<>(catalog);
    }

    public int getSupplierId()       { return supplierId; }
    public String getName()          { return name; }
    public String getAddress()       { return address; }
    public String getPhone()         { return phone; }
    public String getContactPerson() { return contactPerson; }
    public List<Integer> getDeliveryDays() { return deliveryDays; }

    /** True if this supplier carries the product (has a price agreement for it), matched by name. */
    public boolean containsProduct(String productName) {
        for (Agreement a : catalog)
            if (a.getProductName().equalsIgnoreCase(productName)) return true;
        return false;
    }

    /** The supplier's catalog entry (price agreement) for the product, matched by name. */
    public Agreement getProduct(String productName) {
        for (Agreement a : catalog)
            if (a.getProductName().equalsIgnoreCase(productName)) return a;
        throw new IllegalArgumentException(
            "Supplier " + supplierId + " does not carry product '" + productName + "'");
    }
}
