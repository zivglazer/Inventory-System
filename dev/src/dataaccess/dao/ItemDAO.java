package dataaccess.dao;

import dataaccess.Database;
import dataaccess.cache.BoundedCache;
import dataaccess.dto.ItemDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code item} table, with a bounded identity cache. */
public class ItemDAO {

    private final BoundedCache<Integer, ItemDTO> cache = new BoundedCache<>(512);

    public ItemDAO() {
        Database.registerCache(cache);
    }

    public void insert(ItemDTO d) {
        String sql = "INSERT INTO item(item_id, product_id, cost_price, expiration, location_state) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.itemId);
            ps.setInt(2, d.productId);
            ps.setInt(3, d.costPrice);
            ps.setString(4, d.expiration);
            ps.setString(5, d.locationState);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("item insert failed", e);
        }
        cache.put(d.itemId, d);
    }

    public void updateState(int itemId, String locationState) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("UPDATE item SET location_state=? WHERE item_id=?")) {
            ps.setString(1, locationState);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("item updateState failed", e);
        }
        cache.remove(itemId);
    }

    public void delete(int itemId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM item WHERE item_id=?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("item delete failed", e);
        }
        cache.remove(itemId);
    }

    public void deleteByProduct(int productId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM item WHERE product_id=?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("item deleteByProduct failed", e);
        }
        cache.clear();
    }

    /** @return the item row, or {@code null} if none. */
    public ItemDTO find(int itemId) {
        ItemDTO cached = cache.get(itemId);
        if (cached != null) return cached;
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM item WHERE item_id=?")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ItemDTO d = read(rs);
                cache.put(d.itemId, d);
                return d;
            }
        } catch (SQLException e) {
            throw new RuntimeException("item find failed", e);
        }
    }

    public List<ItemDTO> findByProduct(int productId) {
        List<ItemDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM item WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("item findByProduct failed", e);
        }
        return out;
    }

    public List<ItemDTO> findAll() {
        List<ItemDTO> out = new ArrayList<>();
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM item")) {
            while (rs.next()) out.add(read(rs));
        } catch (SQLException e) {
            throw new RuntimeException("item findAll failed", e);
        }
        return out;
    }

    private static ItemDTO read(ResultSet rs) throws SQLException {
        return new ItemDTO(rs.getInt("item_id"), rs.getInt("product_id"), rs.getInt("cost_price"),
                           rs.getString("expiration"), rs.getString("location_state"));
    }
}
