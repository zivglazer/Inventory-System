package dataaccess.dao;

import dataaccess.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code order_delivery_day} table (scheduled orders only). */
public class OrderDeliveryDayDAO {

    public void insert(int orderId, int day) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("INSERT INTO order_delivery_day(order_id, day) VALUES (?,?)")) {
            ps.setInt(1, orderId);
            ps.setInt(2, day);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order_delivery_day insert failed", e);
        }
    }

    public List<Integer> findByOrder(int orderId) {
        List<Integer> days = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT day FROM order_delivery_day WHERE order_id=? ORDER BY day")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) days.add(rs.getInt("day"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("order_delivery_day findByOrder failed", e);
        }
        return days;
    }

    public void deleteByOrder(int orderId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM order_delivery_day WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("order_delivery_day deleteByOrder failed", e);
        }
    }
}
