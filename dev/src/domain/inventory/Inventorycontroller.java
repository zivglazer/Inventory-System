package domain.inventory;

import java.util.*;

import dataaccess.Database;
import dataaccess.dao.*;
import dataaccess.dto.*;
import dataaccess.mapper.*;

/**
 * Inventory orchestrator (the "conductor"). It no longer holds the catalog in memory:
 * the SQLite database is the system of record. Each operation loads only the domain
 * object(s) it needs via DAOs, runs the business logic on those domain objects, then
 * persists the result (write-through) through a Mapper + DAO. The only long-lived in-memory
 * state is the small {@link Shelf} reference data (shelf locations), kept in sync with the DB
 * so {@link Product#getShelfLocation()} resolves labels.
 */
public class Inventorycontroller {

    private static Inventorycontroller instance = null;

    private final CategoryDAO categoryDAO       = new CategoryDAO();
    private final ShelfLocationDAO shelfLocDAO  = new ShelfLocationDAO();
    private final ProductDAO productDAO         = new ProductDAO();
    private final ItemDAO itemDAO               = new ItemDAO();
    private final DiscountDAO discountDAO       = new DiscountDAO();
    private final SoldItemDAO soldItemDAO       = new SoldItemDAO();
    private final DefectItemDAO defectItemDAO   = new DefectItemDAO();

    private Warehouse warehouse;
    private Shelf shelf;

    private Inventorycontroller() {
        this.warehouse = new Warehouse(1, "Main");
        reloadShelf();
    }

    public static Inventorycontroller getInstance() {
        if (instance == null) instance = new Inventorycontroller();
        return instance;
    }

    // ── Reconstruction helpers (DTO → domain) ───────────────────────────────────

    /** Rebuild the full Product aggregate (items + discounts), or throw if it doesn't exist. */
    private Product loadProduct(int productId) {
        ProductDTO p = productDAO.find(productId);
        if (p == null)
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        return ProductMapper.toDomain(p, itemDAO.findByProduct(productId),
                                      discountDAO.findByProduct(productId));
    }

    /** A Category holding just this product plus the category's discounts (enough to price it). */
    private Category loadCategoryForProduct(int productId) {
        ProductDTO p = productDAO.find(productId);
        if (p == null)
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        Product product = ProductMapper.toDomain(p, itemDAO.findByProduct(productId),
                                                 discountDAO.findByProduct(productId));
        Discounts catDiscounts = new Discounts();
        for (DiscountDTO d : discountDAO.findByCategory(p.categoryName))
            catDiscounts.addDiscount(DiscountMapper.toDomain(d));
        List<Product> one = new ArrayList<>();
        one.add(product);
        return Category.rehydrate(p.categoryName, one, catDiscounts);
    }

    private void reloadShelf() {
        this.shelf = new Shelf();
        for (ShelfLocationDTO d : shelfLocDAO.findAll()) shelf.load(d.id, d.label);
    }

    // ── Product / item creation ─────────────────────────────────────────────────

    public void addNewProduct(int productId, String prodName, String manufacturer, String categoryName,
            String subCategory, String subSubCategory, int minToRestock, String shelfLocation,
            int priceWithoutDiscount) {
        if (productDAO.find(productId) != null)
            throw new IllegalArgumentException("Product ID " + productId + " already exists");
        if (prodName == null || manufacturer == null || categoryName == null || subCategory == null
                || subSubCategory == null || shelfLocation == null)
            throw new IllegalArgumentException("Some of the parameters are null");
        // The product name is the universal, unique identifier across the system and supplier
        // catalogs, so it must not collide (case-insensitive).
        if (productDAO.existsByName(prodName.toLowerCase()))
            throw new IllegalArgumentException("Product name '" + prodName + "' already exists");

        final String cat = categoryName.toLowerCase();
        boolean shelfExisted = shelf.getByLabel(shelfLocation) != null;
        ShelfLocation loc = shelf.getOrCreateByLabel(shelfLocation);     // domain: label de-dup

        // domain object first → runs price/min validation
        Product product = new Product(productId, prodName.toLowerCase(), manufacturer, subCategory,
                subSubCategory, loc.getShelfLocationId(), priceWithoutDiscount, minToRestock);

        final ShelfLocation newLoc = shelfExisted ? null : loc;
        Database.runInTransaction(() -> {
            categoryDAO.insertIfAbsent(new CategoryDTO(cat));
            if (newLoc != null)
                shelfLocDAO.insertIfAbsent(new ShelfLocationDTO(newLoc.getShelfLocationId(), newLoc.getLabel()));
            productDAO.insert(ProductMapper.toDTO(product, cat));
        });
    }

    public void addNewItem(int itemId, int productId, int supplierId, int basePrice, Date expirationDate) {
        if (itemDAO.find(itemId) != null)
            throw new IllegalArgumentException("Item ID " + itemId + " already exists");
        if (productDAO.find(productId) == null)
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        Item item = new Item(itemId, basePrice, expirationDate);          // domain validation
        String state = item.isExpired() ? ItemMapper.EXPIRED : ItemMapper.STORAGE;
        itemDAO.insert(ItemMapper.toDTO(item, productId, state));
    }

    // ── Mutations routed through the domain ─────────────────────────────────────

    public void updateMinToRestock(int productId, int minToRestock) {
        if (minToRestock <= 0)
            throw new IllegalArgumentException("Minimum to restock cannot be negative");
        Product p = loadProduct(productId);
        p.updateMinToRestock(minToRestock);
        productDAO.update(ProductMapper.toDTO(p, productDAO.find(productId).categoryName));
    }

    public void moveToShelf(int itemId) {
        ItemDTO i = itemDAO.find(itemId);
        if (i == null)
            throw new IllegalArgumentException("Item ID " + itemId + " doesn't exists");
        Product p = loadProduct(i.productId);
        p.moveToShelf(itemId);                                            // domain: must be in storage
        itemDAO.updateState(itemId, ItemMapper.SHELF);
    }

    public boolean sellItem(int itemId) {
        ItemDTO i = itemDAO.find(itemId);
        if (i == null)
            throw new IllegalArgumentException("Item ID " + itemId + " doesn't exists");
        Category category = loadCategoryForProduct(i.productId);
        final SoldItem sold = category.sellItem(i.productId, new Date(), itemId);  // domain: price + sell rules
        if (sold == null) return false;
        Database.runInTransaction(() -> {
            itemDAO.delete(itemId);
            soldItemDAO.insert(SoldItemMapper.toDTO(sold));
        });
        return true;
    }

    public boolean markItemAsDefective(int itemId, String reason) {
        ItemDTO i = itemDAO.find(itemId);
        if (i == null)
            throw new IllegalArgumentException("Item ID " + itemId + " doesn't exists");
        Product p = loadProduct(i.productId);
        final DefectItem defect = p.markItemDefective(itemId, reason);    // domain: validate + remove
        if (defect == null) return false;
        Database.runInTransaction(() -> {
            itemDAO.delete(itemId);
            defectItemDAO.insert(DefectItemMapper.toDTO(defect));
        });
        return true;
    }

    public void removeItem(int itemId) {
        if (itemDAO.find(itemId) == null)
            throw new IllegalArgumentException("Item ID " + itemId + " doesn't exists");
        itemDAO.delete(itemId);
    }

    public void removeProduct(int productId) {
        if (productDAO.find(productId) == null)
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        Database.runInTransaction(() -> {
            itemDAO.deleteByProduct(productId);
            discountDAO.deleteByProduct(productId);
            productDAO.delete(productId);
        });
    }

    public void addDiscountForProduct(int productId, int discount, Date startDate, Date endDate) {
        if (productDAO.find(productId) == null)
            throw new IllegalArgumentException("Product ID " + productId + " doesn't exists");
        DiscountInfo info = new DiscountInfo(discount, startDate, endDate);   // domain validation
        discountDAO.insert(DiscountMapper.toProductDTO(productId, info));
    }

    public void addDiscountForCategory(String categoryName, int discount, Date startDate, Date endDate) {
        String cat = categoryName.toLowerCase();
        if (!categoryDAO.exists(cat))
            throw new IllegalArgumentException("Category Name " + cat + " doesn't exists");
        DiscountInfo info = new DiscountInfo(discount, startDate, endDate);   // domain validation
        discountDAO.insert(DiscountMapper.toCategoryDTO(cat, info));
    }

    public void inventoryCount(int productId, int inStorage, int onShelf) {
        Product p = loadProduct(productId);
        if (!p.inventoryCountStorage(inStorage))
            throw new IllegalArgumentException("Product ID " + productId + " count in storage is not equal to the given count");
        if (!p.inventoryCountShelf(onShelf))
            throw new IllegalArgumentException("Product ID " + productId + " count on shelf is not equal to the given count");
    }

    public List<Integer> updateShelfAndStorageContents(int productId, List<Integer> itemOnShelf, List<Integer> itemOnStock) {
        Product p = loadProduct(productId);
        final List<Integer> removed = p.updateShelfAndStorageContents(itemOnShelf, itemOnStock);  // domain logic
        Database.runInTransaction(() -> {
            for (int id : itemOnShelf) itemDAO.updateState(id, ItemMapper.SHELF);
            for (int id : itemOnStock) itemDAO.updateState(id, ItemMapper.STORAGE);
            for (int id : removed)     itemDAO.delete(id);
        });
        return removed;
    }

    // ── Queries ─────────────────────────────────────────────────────────────────

    public Product getProduct(int productId) {
        return loadProduct(productId);
    }

    public int getProductId(int itemId) {
        ItemDTO i = itemDAO.find(itemId);
        if (i == null)
            throw new IllegalArgumentException("Item ID " + itemId + " doesn't exist");
        return i.productId;
    }

    public double getPriceForProduct(int productId) {
        return loadCategoryForProduct(productId).getFinalPrice(productId);
    }

    public boolean checkMinToRestock(int productId) {
        return loadProduct(productId).needToRestock();
    }

    public boolean checkRestockByItemId(int itemId) {
        ItemDTO i = itemDAO.find(itemId);
        if (i == null)
            throw new IllegalArgumentException("Item ID " + itemId + " doesn't exists");
        return loadProduct(i.productId).needToRestock();
    }

    public List<Integer> getProductsToRestock() {
        List<Integer> out = new ArrayList<>();
        for (int id : productDAO.findAllIds())
            if (loadProduct(id).needToRestock()) out.add(id);
        return out;
    }

    public List<String> getProductsNeedingRestock() {
        List<String> out = new ArrayList<>();
        for (int id : productDAO.findAllIds()) {
            Product p = loadProduct(id);
            if (p.needToRestock()) out.add(p.toString());
        }
        return out;
    }

    public List<Item> getExpiredItems() {
        List<Item> out = new ArrayList<>();
        for (ItemDTO d : itemDAO.findAll()) {
            Item it = ItemMapper.toDomain(d);
            if (it.isExpired()) out.add(it);
        }
        return out;
    }

    public List<SoldItem> getSoldItemsByDate(Date startDate, Date endDate) {
        List<SoldItem> out = new ArrayList<>();
        for (SoldItemDTO d : soldItemDAO.findAll()) {
            SoldItem s = SoldItemMapper.toDomain(d);
            if (s.inRangeDates(startDate, endDate)) out.add(s);
        }
        return out;
    }

    public SoldItem getSoldItemsById(int itemId) {
        SoldItemDTO d = soldItemDAO.find(itemId);
        return d == null ? null : SoldItemMapper.toDomain(d);
    }

    public List<DefectItem> getDefectiveItems() {
        List<DefectItem> out = new ArrayList<>();
        for (DefectItemDTO d : defectItemDAO.findAll()) out.add(DefectItemMapper.toDomain(d));
        return out;
    }

    public List<DefectItem> getAllDefectiveItems() {
        return getDefectiveItems();
    }

    public List<DefectItem> getDefectiveItemsByDateRange(Date startDate, Date endDate) {
        List<DefectItem> out = new ArrayList<>();
        for (DefectItemDTO d : defectItemDAO.findAll()) {
            DefectItem di = DefectItemMapper.toDomain(d);
            if (di.inRangeDates(startDate, endDate)) out.add(di);
        }
        return out;
    }

    public List<String> getAllCategories() {
        return categoryDAO.findAllNames();
    }

    public Category getCategory(String categoryName) {
        return loadCategory(categoryName);
    }

    private Category loadCategory(String categoryName) {
        String key = categoryName.toLowerCase();
        if (!categoryDAO.exists(key)) return null;
        List<Product> products = new ArrayList<>();
        for (ProductDTO p : productDAO.findByCategory(key))
            products.add(ProductMapper.toDomain(p, itemDAO.findByProduct(p.productId),
                                                discountDAO.findByProduct(p.productId)));
        Discounts catDiscounts = new Discounts();
        for (DiscountDTO d : discountDAO.findByCategory(key))
            catDiscounts.addDiscount(DiscountMapper.toDomain(d));
        return Category.rehydrate(key, products, catDiscounts);
    }

    public Warehouse getWarehouse() { return warehouse; }
    public Shelf getShelf()         { return shelf; }

    // ── Test support: wipe only the inventory tables, leaving other modules intact ──
    public void resetAll() {
        Database.clearTables(Arrays.asList(
            "item", "sold_item", "defect_item", "discount", "product", "shelf_location", "category"));
        Database.clearAllCaches();
        this.warehouse = new Warehouse(1, "Main");
        this.shelf = new Shelf();
    }
}
