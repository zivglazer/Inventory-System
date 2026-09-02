package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.ShelfLocationDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code shelf_location} table (small reference data). */
public class ShelfLocationDAO {

    public void insertIfAbsent(ShelfLocationDTO dto) {
        String sql = "INSERT OR IGNORE INTO shelf_location(id, label) VALUES (?, ?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setInt(1, dto.id);
            ps.setString(2, dto.label);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("shelf_location insert failed", e);
        }
    }

    public List<ShelfLocationDTO> findAll() {
        List<ShelfLocationDTO> all = new ArrayList<>();
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT id, label FROM shelf_location")) {
            while (rs.next()) all.add(new ShelfLocationDTO(rs.getInt("id"), rs.getString("label")));
        } catch (SQLException e) {
            throw new RuntimeException("shelf_location findAll failed", e);
        }
        return all;
    }
}
