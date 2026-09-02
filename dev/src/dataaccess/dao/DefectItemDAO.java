package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.DefectItemDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code defect_item} table. */
public class DefectItemDAO {

    public void insert(DefectItemDTO d) {
        String sql = "INSERT INTO defect_item(item_id, cost_price, expiration, reason, update_date) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.itemId);
            ps.setInt(2, d.costPrice);
            ps.setString(3, d.expiration);
            ps.setString(4, d.reason);
            ps.setString(5, d.updateDate);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("defect_item insert failed", e);
        }
    }

    public List<DefectItemDTO> findAll() {
        List<DefectItemDTO> out = new ArrayList<>();
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM defect_item")) {
            while (rs.next()) out.add(read(rs));
        } catch (SQLException e) {
            throw new RuntimeException("defect_item findAll failed", e);
        }
        return out;
    }

    private static DefectItemDTO read(ResultSet rs) throws SQLException {
        return new DefectItemDTO(rs.getInt("item_id"), rs.getInt("cost_price"),
                                 rs.getString("expiration"), rs.getString("reason"),
                                 rs.getString("update_date"));
    }
}
