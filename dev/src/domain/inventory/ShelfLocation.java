package domain.inventory;

public class ShelfLocation {
    private final int shelfLocationId;
    private final String label;

    public ShelfLocation(int shelfLocationId, String label) {
        if (label == null || label.isBlank())
            throw new IllegalArgumentException("Shelf location label cannot be empty");
        this.shelfLocationId = shelfLocationId;
        this.label = label;
    }

    public int getShelfLocationId() { return shelfLocationId; }
    public String getLabel()        { return label; }

    @Override
    public String toString() { return label; }
}
