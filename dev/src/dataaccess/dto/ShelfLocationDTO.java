package dataaccess.dto;

/** A flat shelf-location row. */
public class ShelfLocationDTO {
    public int id;
    public String label;

    public ShelfLocationDTO() {}

    public ShelfLocationDTO(int id, String label) {
        this.id = id;
        this.label = label;
    }
}
