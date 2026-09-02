package dataaccess.dao;

import dataaccess.Database;
import dataaccess.cache.BoundedCache;
import dataaccess.dto.OrderDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code orders} table, with a bounded identity cache. */
public class OrderDAO {

    private final BoundedCache<Integer, OrderDTO> cache = new BoundedCache<>(256);

    public OrderDAO() {
        Database.registerCache(cache);
    }

    /** Next system-allocated order id (MAX+1), 1 when the table is empty. */
    public int nextId() {
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(order_id),0)+1 AS nid FROM orders")) {
            rs.next();
            return rs.getInt("nid");
        } catch (SQLException e) {
            throw new RuntimeException("order nextId failed", e);
        }
    }

    public void insert(OrderDTO d) {
        String sql = "INSERT INTO orders(order_id, type, supplier_id, status, order_date) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.orderId);
            ps.setString(2, d.type);
            ps.setInt(3, d.supplierId);
            ps.setString(4, d.status);
            ps.setString(5, d.orderDate);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order insert failed", e);
        }
        cache.put(d.orderId, d);
    }

    /** Update the mutable fields (status + date). */
    public void update(OrderDTO d) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("UPDATE orders SET status=?, order_date=? WHERE order_id=?")) {
            ps.setString(1, d.status);
            ps.setString(2, d.orderDate);
            ps.setInt(3, d.orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order update failed", e);
        }
        cache.put(d.orderId, d);
    }

    public void delete(int orderId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM orders WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order delete failed", e);
        }
        cache.remove(orderId);
    }

    /** @return the order row, or {@code null} if none. */
    public OrderDTO find(int orderId) {
        OrderDTO cached = cache.get(orderId);
        if (cached != null) return cached;
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM orders WHERE order_id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                OrderDTO d = read(rs);
                cache.put(d.orderId, d);
                return d;
            }
        } catch (SQLException e) {
            throw new RuntimeException("order find failed", e);
        }
    }

    public List<Integer> findAllIds() {
        return idQuery("SELECT order_id FROM orders ORDER BY order_id", null);
    }

    public List<Integer> findIdsByType(String type) {
        return idQuery("SELECT order_id FROM orders WHERE type=? ORDER BY order_id", type);
    }

    private List<Integer> idQuery(String sql, String typeArg) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            if (typeArg != null) ps.setString(1, typeArg);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("order_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("order id query failed", e);
        }
        return ids;
    }

    private static OrderDTO read(ResultSet rs) throws SQLException {
        return new OrderDTO(rs.getInt("order_id"), rs.getString("type"), rs.getInt("supplier_id"),
                            rs.getString("status"), rs.getString("order_date"));
    }
}
