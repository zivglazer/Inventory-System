package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.DiscountDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code discount} table (product- or category-scoped rows). */
public class DiscountDAO {

    public void insert(DiscountDTO d) {
        String sql = "INSERT INTO discount(product_id, category_name, percentage, start_date, end_date) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            if (d.productId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, d.productId);
            ps.setString(2, d.categoryName);
            ps.setInt(3, d.percentage);
            ps.setString(4, d.startDate);
            ps.setString(5, d.endDate);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("discount insert failed", e);
        }
    }

    public List<DiscountDTO> findByProduct(int productId) {
        List<DiscountDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM discount WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("discount findByProduct failed", e);
        }
        return out;
    }

    public List<DiscountDTO> findByCategory(String categoryName) {
        List<DiscountDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM discount WHERE category_name=?")) {
            ps.setString(1, categoryName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("discount findByCategory failed", e);
        }
        return out;
    }

    public void deleteByProduct(int productId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM discount WHERE product_id=?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("discount deleteByProduct failed", e);
        }
    }

    private static DiscountDTO read(ResultSet rs) throws SQLException {
        DiscountDTO d = new DiscountDTO();
        d.id = rs.getInt("id");
        int pid = rs.getInt("product_id");
        d.productId = rs.wasNull() ? null : pid;
        d.categoryName = rs.getString("category_name");
        d.percentage = rs.getInt("percentage");
        d.startDate = rs.getString("start_date");
        d.endDate = rs.getString("end_date");
        return d;
    }
}
