package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.SoldItemDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code sold_item} table. */
public class SoldItemDAO {

    public void insert(SoldItemDTO d) {
        String sql = "INSERT INTO sold_item(item_id, sell_date, sell_price, cost_price) VALUES (?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, d.itemId);
            ps.setString(2, d.sellDate);
            ps.setDouble(3, d.sellPrice);
            ps.setInt(4, d.costPrice);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("sold_item insert failed", e);
        }
    }

    /** @return the sold-item row, or {@code null} if none. */
    public SoldItemDTO find(int itemId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM sold_item WHERE item_id=?")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("sold_item find failed", e);
        }
    }

    public List<SoldItemDTO> findAll() {
        List<SoldItemDTO> out = new ArrayList<>();
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM sold_item")) {
            while (rs.next()) out.add(read(rs));
        } catch (SQLException e) {
            throw new RuntimeException("sold_item findAll failed", e);
        }
        return out;
    }

    private static SoldItemDTO read(ResultSet rs) throws SQLException {
        return new SoldItemDTO(rs.getInt("item_id"), rs.getString("sell_date"),
                               rs.getDouble("sell_price"), rs.getInt("cost_price"));
    }
}
