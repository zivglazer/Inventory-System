package dataaccess.dao;

import dataaccess.Database;
import dataaccess.cache.BoundedCache;
import dataaccess.dto.ProductDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access to the {@code product} table, with a bounded identity cache. */
public class ProductDAO {

    private final BoundedCache<Integer, ProductDTO> cache = new BoundedCache<>(256);

    public ProductDAO() {
        Database.registerCache(cache);
    }

    public void insert(ProductDTO d) {
        String sql = "INSERT INTO product(product_id, name, manufacturer, category_name, " +
                     "sub_category, sub_sub_category, shelf_location_id, supplier_id, " +
                     "min_to_restock, price_without_discount) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            bind(ps, d);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("product insert failed", e);
        }
        cache.put(d.productId, d);
    }

    public void update(ProductDTO d) {
        String sql = "UPDATE product SET name=?, manufacturer=?, category_name=?, sub_category=?, " +
                     "sub_sub_category=?, shelf_location_id=?, supplier_id=?, min_to_restock=?, " +
                     "price_without_discount=? WHERE product_id=?";
        try (PreparedStatement ps = Database.connection().prepareStatement(sql)) {
            ps.setString(1, d.name);
            ps.setString(2, d.manufacturer);
            ps.setString(3, d.categoryName);
            ps.setString(4, d.subCategory);
            ps.setString(5, d.subSubCategory);
            ps.setInt(6, d.shelfLocationId);
            ps.setInt(7, d.supplierId);
            ps.setInt(8, d.minToRestock);
            ps.setInt(9, d.priceWithoutDiscount);
            ps.setInt(10, d.productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("product update failed", e);
        }
        cache.put(d.productId, d);
    }

    public void delete(int productId) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("DELETE FROM product WHERE product_id=?")) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("product delete failed", e);
        }
        cache.remove(productId);
    }

    /** @return the product row, or {@code null} if none. */
    public ProductDTO find(int productId) {
        ProductDTO cached = cache.get(productId);
        if (cached != null) return cached;
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM product WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ProductDTO d = read(rs);
                cache.put(d.productId, d);
                return d;
            }
        } catch (SQLException e) {
            throw new RuntimeException("product find failed", e);
        }
    }

    public List<ProductDTO> findByCategory(String categoryName) {
        List<ProductDTO> out = new ArrayList<>();
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT * FROM product WHERE category_name=?")) {
            ps.setString(1, categoryName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("product findByCategory failed", e);
        }
        return out;
    }

    /** True if a product with this (already-normalised, lower-case) name exists. */
    public boolean existsByName(String name) {
        try (PreparedStatement ps = Database.connection()
                .prepareStatement("SELECT 1 FROM product WHERE name=? LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("product existsByName failed", e);
        }
    }

    public List<Integer> findAllIds() {
        List<Integer> ids = new ArrayList<>();
        try (Statement st = Database.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT product_id FROM product")) {
            while (rs.next()) ids.add(rs.getInt("product_id"));
        } catch (SQLException e) {
            throw new RuntimeException("product findAllIds failed", e);
        }
        return ids;
    }

    private static void bind(PreparedStatement ps, ProductDTO d) throws SQLException {
        ps.setInt(1, d.productId);
        ps.setString(2, d.name);
        ps.setString(3, d.manufacturer);
        ps.setString(4, d.categoryName);
        ps.setString(5, d.subCategory);
        ps.setString(6, d.subSubCategory);
        ps.setInt(7, d.shelfLocationId);
        ps.setInt(8, d.supplierId);
        ps.setInt(9, d.minToRestock);
        ps.setInt(10, d.priceWithoutDiscount);
    }

    private static ProductDTO read(ResultSet rs) throws SQLException {
        return new ProductDTO(
            rs.getInt("product_id"), rs.getString("name"), rs.getString("manufacturer"),
            rs.getString("category_name"), rs.getString("sub_category"), rs.getString("sub_sub_category"),
            rs.getInt("shelf_location_id"), rs.getInt("supplier_id"),
            rs.getInt("min_to_restock"), rs.getInt("price_without_discount"));
    }
}
