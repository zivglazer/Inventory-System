package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.OrderLineDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code order_line} table. */
public class OrderLineDAO {

    public void insert(OrderLineDTO d) {
        String sql = "INSERT INTO order_line(order_id, product_id, quantity, unit_price) VALUES (?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.orderId);
            ps.setInt(2, d.productId);
            ps.setInt(3, d.quantity);
            ps.setInt(4, d.unitPrice);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order_line insert failed", e);
        }
    }

    public List<OrderLineDTO> findByOrder(int orderId) {
        List<OrderLineDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM order_line WHERE order_id=? ORDER BY product_id")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new OrderLineDTO(rs.getInt("order_id"), rs.getInt("product_id"),
                                             rs.getInt("quantity"), rs.getInt("unit_price")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("order_line findByOrder failed", e);
        }
        return out;
    }

    public void deleteByOrder(int orderId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM order_line WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order_line deleteByOrder failed", e);
        }
    }
}
