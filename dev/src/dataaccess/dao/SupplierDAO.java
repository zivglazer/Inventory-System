package dataaccess.dao;

import dataaccess.Database;
import dataaccess.cache.BoundedCache;
import dataaccess.dto.SupplierDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** SQL access to the {@code supplier} table, with a bounded identity cache. */
public class SupplierDAO {

    private final BoundedCache<Integer, SupplierDTO> cache = new BoundedCache<>(128);

    public SupplierDAO() {
        Database.registerCache(cache);
    }

    public void insert(SupplierDTO d) {
        String sql = "INSERT INTO supplier(supplier_id, name, address, phone, contact) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.supplierId);
            ps.setString(2, d.name);
            ps.setString(3, d.address);
            ps.setString(4, d.phone);
            ps.setString(5, d.contact);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("supplier insert failed", e);
        }
        cache.put(d.supplierId, d);
    }

    /** @return the supplier row, or {@code null} if none. */
    public SupplierDTO find(int supplierId) {
        SupplierDTO cached = cache.get(supplierId);
        if (cached != null) return cached;
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM supplier WHERE supplier_id=?")) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                SupplierDTO d = read(rs);
                cache.put(d.supplierId, d);
                return d;
            }
        } catch (SQLException e) {
            throw new RuntimeException("supplier find failed", e);
        }
    }

    public boolean isEmpty() {
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM supplier LIMIT 1")) {
            return !rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("supplier isEmpty failed", e);
        }
    }

    /** Lowest supplier id (the default supplier), or -1 when there are none. */
    public int minId() {
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT MIN(supplier_id) AS m, COUNT(*) AS c FROM supplier")) {
            rs.next();
            return rs.getInt("c") == 0 ? -1 : rs.getInt("m");
        } catch (SQLException e) {
            throw new RuntimeException("supplier minId failed", e);
        }
    }

    private static SupplierDTO read(ResultSet rs) throws SQLException {
        return new SupplierDTO(rs.getInt("supplier_id"), rs.getString("name"), rs.getString("address"),
                               rs.getString("phone"), rs.getString("contact"));
    }
}
