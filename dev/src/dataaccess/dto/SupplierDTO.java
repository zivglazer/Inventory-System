package dataaccess.dto;

/** A flat supplier row (delivery days live in a separate table). */
public class SupplierDTO {
    public int supplierId;
    public String name;
    public String address;
    public String phone;
    public String contact;

    public SupplierDTO() {}

    public SupplierDTO(int supplierId, String name, String address, String phone, String contact) {
        this.supplierId = supplierId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.contact = contact;
    }
}
