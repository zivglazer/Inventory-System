package domain.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Category {
    private String categoryName;
    private Map<Integer, Product> products = new HashMap<>(); // product_id → Product
    private Discounts discounts = new Discounts();

    public Category(String categoryName) {
        this.categoryName = categoryName;
    }

    /** Rebuild a Category from persisted state (used only by the data-access layer). */
    public static Category rehydrate(String categoryName, List<Product> products, Discounts discounts) {
        Category c = new Category(categoryName);
        for (Product p : products) c.products.put(p.getProductId(), p);
        c.discounts = discounts;
        return c;
    }

    // Getters
    public String getName() {
        return categoryName;
    }

    public double getFinalPrice(int productId) {
        double discount = discounts.getBestActiveDiscount(new Date());
        return getProduct(productId).getPrice() * (100 - discount) / 100;
    }
    
    public SoldItem sellItem(int productId, Date sellDate,int itemId) {
        return getProduct(productId).sellItem(itemId, sellDate, getFinalPrice(productId));
    }
    public Product getProduct(int productId) {
        if (!products.containsKey(productId))
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        return products.get(productId);
    }

    public void addNewItem(int item_id, int supplier_id, int basePrice, Date expirationDate, int product_id) {
        if (!products.containsKey(product_id))
            throw new IllegalArgumentException("Prodact Name " + product_id + " doesn't exists");
        products.get(product_id).addNewItem(item_id, supplier_id, basePrice, expirationDate);
    }

    public void addProduct(int product_id, String productName, String manufacturer,
            String subCategory, String subSubCategory,
            int minToRestock, int shelfLocationId, int priceWithoutDiscount) {
        if (products.containsKey(product_id))
            throw new IllegalArgumentException("Product " + product_id + " already exists");
        products.put(product_id,
                new Product(product_id, productName, manufacturer, subCategory, subSubCategory, shelfLocationId, priceWithoutDiscount, minToRestock));
    }

    public List<Item> getExpiredItems() {
        List<Item> expiredItems = new ArrayList<>();
        for (Product product : products.values()) {
            expiredItems.addAll(product.getItemsOutOfDate());
        }
        return expiredItems;
    }

    public void removeProduct(int productId) {
        products.remove(productId);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public List<Product> getProducts() {
        return getAllProducts();
    }

    public void addDiscountForCategory(int percentage, Date startDate, Date endDate) {

        discounts.addDiscount(new DiscountInfo(percentage, startDate, endDate));
    }

    public void addDiscountForProduct(int product_id, int discount, Date startDate, Date endDate) {
        if (!products.containsKey(product_id))
            throw new IllegalArgumentException("Product ID " + product_id + " doesn't exists");
        products.get(product_id).addDiscount(new DiscountInfo(discount, startDate, endDate));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(categoryName).append("\n");
        sb.append("Products:\n");
        for (Product product : products.values()) {
            sb.append(product.toString()).append("\n");
        }
        return sb.toString();
    }

    public void moveItemToShelf(int productId, int item_id) {
        if (!products.containsKey(productId))
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        products.get(productId).moveToShelf(item_id);
    }

    public Discounts getDiscounts() {
        return discounts;
    }
 
    public DefectItem markItemDefective(int productId, int itemId, String reason) {
        if (!products.containsKey(productId))
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        return products.get(productId).markItemDefective(itemId, reason);
    }
}
