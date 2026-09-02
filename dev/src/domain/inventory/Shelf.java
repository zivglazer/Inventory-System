package domain.inventory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Shelf {
    private final Map<Integer, ShelfLocation> shelfLocations;
    private final Map<String, Integer> labelToId;
    private int nextId;

    public Shelf() {
        this.shelfLocations = new HashMap<>();
        this.labelToId = new HashMap<>();
        this.nextId = 1;
    }

    public ShelfLocation getShelfLocation(int id) {
        return shelfLocations.get(id);
    }

    /** Load an existing shelf location from persisted state, keeping {@code nextId} ahead of it. */
    public void load(int id, String label) {
        shelfLocations.put(id, new ShelfLocation(id, label));
        labelToId.put(label.toLowerCase(), id);
        if (id >= nextId) nextId = id + 1;
    }

    public ShelfLocation getByLabel(String label) {
        Integer id = labelToId.get(label.toLowerCase());
        return id == null ? null : shelfLocations.get(id);
    }

    public ShelfLocation getOrCreateByLabel(String label) {
        String key = label.toLowerCase();
        Integer id = labelToId.get(key);
        if (id != null)
            return shelfLocations.get(id);
        ShelfLocation loc = new ShelfLocation(nextId, label);
        shelfLocations.put(nextId, loc);
        labelToId.put(key, nextId);
        nextId++;
        return loc;
    }

    public Collection<ShelfLocation> getAll() {
        return Collections.unmodifiableCollection(shelfLocations.values());
    }
}
