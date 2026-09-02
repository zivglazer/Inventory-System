package domain.inventory;

    import java.util.ArrayList;
    import java.util.Date;
    import java.util.Iterator;
    import java.util.LinkedList;
    import java.util.List;
    import java.util.Set;

    public class Product {
        /** Default minimum-to-restock threshold used when none is set at creation time. */
        public static final int DEFAULT_MIN_TO_RESTOCK = 10;

        private int productId;
        private int supplierId;
        private String name;
        private int priceWithoutDiscount;
        private String manufacturer;
        private String subCategory;
        private String subSubCategory;
        private int shelfLocationId;
        private int minToRestock;
        private Discounts discounts;
        private List<Item> shelfItems;
        private List<Item> storageItems;
        public List<Item> expiredItems;

        public Product(int productId,  String name, String manufacturer, String subCategory,
                String subSubCategory,
                int shelfLocationId, int priceWithoutDiscount, int minToRestock) {
            if (priceWithoutDiscount < 0)
                throw new IllegalArgumentException("Price cannot be negative");
            if (minToRestock < 0)
                throw new IllegalArgumentException("Minimum restock quantity cannot be negative");
            this.productId = productId;
            this.name = name;
            this.manufacturer = manufacturer;
            this.subCategory = subCategory;
            this.subSubCategory = subSubCategory;
            this.shelfLocationId = shelfLocationId;
            this.priceWithoutDiscount = priceWithoutDiscount;
            // 0 means "not set" -> fall back to the default threshold (spec rule).
            this.minToRestock = (minToRestock == 0) ? DEFAULT_MIN_TO_RESTOCK : minToRestock;
            this.discounts = new Discounts();
            this.shelfItems = new LinkedList<>();
            this.storageItems = new LinkedList<>();
            this.expiredItems = new LinkedList<>();
        }

        /**
         * Rebuild a Product from persisted state (used only by the data-access mappers).
         * Reuses the validating constructor, then re-attaches the stored items, discounts and
         * supplier id directly — without re-running creation-time side effects.
         */
        public static Product rehydrate(int productId, int supplierId, String name, String manufacturer,
                String subCategory, String subSubCategory, int shelfLocationId, int priceWithoutDiscount,
                int minToRestock, List<Item> shelfItems, List<Item> storageItems, List<Item> expiredItems,
                Discounts discounts) {
            Product p = new Product(productId, name, manufacturer, subCategory, subSubCategory,
                    shelfLocationId, priceWithoutDiscount, minToRestock);
            p.supplierId = supplierId;
            p.shelfItems = shelfItems;
            p.storageItems = storageItems;
            p.expiredItems = expiredItems;
            p.discounts = discounts;
            return p;
        }

        // Getters
        public int getProductId() {
            return productId;
        }

        public String getName() {
            return name;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getSubCategory() {
            return subCategory;
        }

        public String getSubSubCategory() {
            return subSubCategory;
        }

        public ShelfLocation getShelfLocation() {
            return Inventorycontroller.getInstance().getShelf().getShelfLocation(shelfLocationId);
        }

        public int getShelfLocationId() {
            return shelfLocationId;
        }

        public int getPriceWithoutDiscount() {
            return priceWithoutDiscount;
        }

        public int getSupplierId() {
            return supplierId;
        }

        public int getShelfQuantity() {
            return shelfItems.size();
        }

        public int getStorageItemsQuantity() {
            return storageItems.size();
        }

        public int getTotalQuantity() {
            return (getShelfQuantity() + getStorageItemsQuantity());
        }

        public Discounts getDiscounts() {
            return discounts;
        }

        public int getMinToRestock() {
            return minToRestock;
        }

        public int getId() {
            return productId;
        }

        public double getPrice() {
            return priceWithoutDiscount * (((100 - discounts.getBestActiveDiscount(new Date())) / 100));
        }
 

        public void addNewItem(int item_id, int supplier_id, int basePrice, Date expirationDate) {
            if (isItemInStorage(item_id) || isItemOnShelf(item_id))
                throw new IllegalArgumentException("Item " + item_id + " already exists");
            Item newItem = new Item(item_id, basePrice, expirationDate);
            if (expirationDate.before(new Date()))
                expiredItems.add(newItem);
            else 
                storageItems.add(newItem);
        }

        public Item removeItem(int item_id) {
            for (Item item : shelfItems)
                if (item.getItemId() == item_id) {
                    shelfItems.remove(item);
                    return item;
                }
            for (Item item : storageItems)
                if (item.getItemId() == item_id) {
                    storageItems.remove(item);
                    return item;
                }
            return null;
        }

        private boolean isItemInStorage(int item_id) {
            for (Item item : storageItems)
                if (item.getItemId() == item_id)
                    return true;
            return false;
        }

        public boolean isItemOnShelf(int item_id) {
            for (Item item : shelfItems)
                if (item.getItemId() == item_id)
                    return true;
            return false;
        }

        public void moveToShelf(int item_id) {
            if (!isItemInStorage(item_id))
                throw new IllegalArgumentException("Item " + item_id + " doesn't exists in storage");
            shelfItems.add(removeItem(item_id));
        }

        // Discount
        public void addDiscount(DiscountInfo discountInfo) {
            this.discounts.addDiscount(discountInfo);
        }

        public Item getItem(int itemId) {
            for (Item item : shelfItems)
                if (item.getItemId() == itemId)
                    return item;
            for (Item item : storageItems)
                if (item.getItemId() == itemId)
                    return item;
            return null;
        }

        /**
         * Quantity to order so that, once the delivery arrives, total stock sits exactly one unit
         * above the minimum threshold (spec shortage-replenishment rule). Never less than 1.
         */
        public int restockQuantity() {
            return Math.max(minToRestock - getTotalQuantity() + 1, 1);
        }

        public boolean needToRestock() {
            updateOutOfDateItems();
            return (storageItems.size() + shelfItems.size()) - expiredItems.size() < minToRestock;
        }

        private void updateOutOfDateItems() {
            for (Iterator<Item> it = storageItems.iterator(); it.hasNext(); ) {
                Item item = it.next();
                if (item.isExpired()) {
                    expiredItems.add(item);
                    it.remove();
                }
            }
            for (Iterator<Item> it = shelfItems.iterator(); it.hasNext(); ) {
                Item item = it.next();
                if (item.isExpired()) {
                    expiredItems.add(item);
                    it.remove();
                }
            }
        }

        public List<Integer> updateShelfAndStorageContents(List<Integer> itemOnShelfIDs, List<Integer> itemInStockIDs) {
            List<Integer> removeItems = new ArrayList<>();
            for (Item item : shelfItems)
                if (!itemOnShelfIDs.contains(item.getItemId()) && !itemInStockIDs.contains(item.getItemId()))
                    removeItems.add(item.getItemId());
            for (Item item : storageItems)
                if (!itemOnShelfIDs.contains(item.getItemId()) && !itemInStockIDs.contains(item.getItemId()))
                    removeItems.add(item.getItemId());
            shelfItems = getItemList(itemOnShelfIDs);
            storageItems = getItemList(itemInStockIDs);
            removeItemsById(removeItems);
            return removeItems;
        }

        public List<Item> getItemList(List<Integer> itemIDs) {
            for (int itemID : itemIDs)
                if (!isItemOnShelf(itemID) && !isItemInStorage(itemID))
                    throw new IllegalArgumentException("Item " + itemID + " doesn't exists");
            List<Item> items = new ArrayList<>();
            for (int itemID : itemIDs)
                items.add(getItem(itemID));
            return items;
        }

        private void removeItemsById(List<Integer> items) {
            for (int itemID : items)
                removeItem(itemID);
        }

        public List<Item> getItemsOutOfDate() {
            updateOutOfDateItems();
            List<Item> items = new ArrayList<>();
            for (Item item : expiredItems) {
                items.add(item);
            }
            return items;
        }

        public void updateMinToRestock(int minQuantity) {
            this.minToRestock = minQuantity;
        }

        public SoldItem sellItem(int itemId, Date sellDate, double sellPrice) {
            if (!isItemOnShelf(itemId))
                throw new IllegalArgumentException("Item " + itemId + " doesn't exists on shelf");
            Item item = removeItem(itemId);
            return item.sell(sellDate, sellPrice);
        }

        public DefectItem markItemDefective(int itemId, String reason) {
            if (!isItemOnShelf(itemId) && !isItemInStorage(itemId))
                throw new IllegalArgumentException("Item " + itemId + " doesn't exists");
            Item item = removeItem(itemId);
            return item.markDefective(reason);
        }

        // Restock
        public boolean needsRestock() {
            return getTotalQuantity() < minToRestock;
        }

        // public for tests
        public boolean inventoryCountStorage(int inStorage) {
            return inStorage == storageItems.size();
        }

        public boolean inventoryCountShelf(int inSelf) {
            return inSelf == shelfItems.size();
        }

        @Override
        public String toString() {
            return "=== Product " + productId + " ==="
                    + "\n Product Name  = " + name
                    + "\n Manufacturer  = " + manufacturer
                    + "\n Sub-Category  = " + subCategory
                    + "\n Sub-Sub-Cat   = " + subSubCategory
                    + "\n Min Quantity  = " + minToRestock
                    + "\n Shelf Location= " + getShelfLocation().getLabel()
                    + "\n Base Price    = " + priceWithoutDiscount
                    + "\n On Shelf      = " + shelfItems.size()
                    + "\n In Storage    = " + storageItems.size();
        }

    }
