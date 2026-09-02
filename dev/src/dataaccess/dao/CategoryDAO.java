package dataaccess.dao;

import dataaccess.Database;
import dataaccess.dto.CategoryDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code category} table. */
public class CategoryDAO {

    public void insertIfAbsent(CategoryDTO dto) {
        String sql = "INSERT OR IGNORE INTO category(name) VALUES (?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setString(1, dto.name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("category insert failed", e);
        }
    }

    public boolean exists(String name) {
        String sql = "SELECT 1 FROM category WHERE name = ?";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("category exists failed", e);
        }
    }

    public List<String> findAllNames() {
        List<String> names = new ArrayList<>();
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM category")) {
            while (rs.next()) names.add(rs.getString("name"));
        } catch (SQLException e) {
            throw new RuntimeException("category findAll failed", e);
        }
        return names;
    }
}
