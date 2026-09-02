package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.AgreementDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code agreement} table. */
public class AgreementDAO {

    public void insert(AgreementDTO d) {
        String sql = "INSERT INTO agreement(supplier_id, product_name, unit_price, bulk_threshold, bulk_unit_price) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.supplierId);
            ps.setString(2, d.productName);
            ps.setInt(3, d.unitPrice);
            ps.setInt(4, d.bulkThreshold);
            ps.setInt(5, d.bulkUnitPrice);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("agreement insert failed", e);
        }
    }

    public List<AgreementDTO> findBySupplier(int supplierId) {
        List<AgreementDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM agreement WHERE supplier_id=? ORDER BY product_name")) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new AgreementDTO(rs.getInt("supplier_id"), rs.getString("product_name"),
                            rs.getInt("unit_price"), rs.getInt("bulk_threshold"), rs.getInt("bulk_unit_price")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("agreement findBySupplier failed", e);
        }
        return out;
    }

    public List<AgreementDTO> findByProduct(String productName) {
        List<AgreementDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM agreement WHERE product_name=? COLLATE NOCASE ORDER BY supplier_id")) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new AgreementDTO(rs.getInt("supplier_id"), rs.getString("product_name"),
                            rs.getInt("unit_price"), rs.getInt("bulk_threshold"), rs.getInt("bulk_unit_price")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("agreement findByProduct failed", e);
        }
        return out;
    }
}
