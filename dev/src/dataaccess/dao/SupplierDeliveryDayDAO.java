package dataaccess.dao;

import dataaccess.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code supplier_delivery_day} table. */
public class SupplierDeliveryDayDAO {

    public void insert(int supplierId, int day) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("INSERT INTO supplier_delivery_day(supplier_id, day) VALUES (?,?)")) {
            ps.setInt(1, supplierId);
            ps.setInt(2, day);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("supplier_delivery_day insert failed", e);
        }
    }

    public List<Integer> findBySupplier(int supplierId) {
        List<Integer> days = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT day FROM supplier_delivery_day WHERE supplier_id=? ORDER BY day")) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) days.add(rs.getInt("day"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("supplier_delivery_day findBySupplier failed", e);
        }
        return days;
    }
}
