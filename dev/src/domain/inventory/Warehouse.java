package domain.inventory;

public class Warehouse {
    private final int warehouseId;
    private final String name;

    public Warehouse(int warehouseId, String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Warehouse name cannot be empty");
        this.warehouseId = warehouseId;
        this.name = name;
    }

    public int getWarehouseId() { return warehouseId; }
    public String getName()     { return name; }

    @Override
    public String toString() {
        return "Warehouse " + warehouseId + " (" + name + ")";
    }
}
