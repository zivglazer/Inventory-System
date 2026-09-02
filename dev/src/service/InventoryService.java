package service;

import domain.inventory.*;
import domain.reports.Report;
import domain.reports.Reportcontroller;
import java.util.*;

public class InventoryService  {

    private final Inventorycontroller controller;
    private final Reportcontroller reportController;

    public InventoryService() {
        this.controller = Inventorycontroller.getInstance();
        this.reportController = Reportcontroller.getInstance();
    }

    // --- Product Management ---

    
    public Response<Boolean> addNewProduct(int productId, String name, String manufacturer,
            String categoryName, String subCategory, String subSubCategory,
            int minToRestock, String shelfLocation, int priceWithoutDiscount) {
        try {
            controller.addNewProduct(productId, name, manufacturer, categoryName,
                subCategory, subSubCategory, minToRestock, shelfLocation, priceWithoutDiscount);
            return new Response<>(true, "Product added successfully");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> addNewItem(int itemId, int supplierId, int basePrice,
            Date expirationDate, int productId) {
        try {
            controller.addNewItem(itemId, productId, supplierId, basePrice, expirationDate);
            return new Response<>(true, "Item added successfully");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> updateMinToRestock(int productId, int minToRestock) {
        try {
            controller.updateMinToRestock(productId, minToRestock);
            return new Response<>(true, "Min restock updated");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Product> getProduct(int productId) {
        try {
            return new Response<>(controller.getProduct(productId), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    /** Which product an item belongs to (needed before a sell/defect/remove deletes the item). */
    public Response<Integer> getProductIdByItem(int itemId) {
        try {
            return new Response<>(controller.getProductId(itemId), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> removeItem(int itemId) {
        try {
            controller.removeItem(itemId);
            return new Response<>(true, "Item removed");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> moveToShelf(List<Integer> itemIds) {
        try {
            for (int itemId : itemIds)
                controller.moveToShelf(itemId);
            return new Response<>(true, "Items moved to shelf");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }


    public Response<Boolean> removeProduct(int productId) {
        try {
            controller.removeProduct(productId);
            return new Response<>(true, "Product removed");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
     }

     
    public Response<Boolean> setDefectiveItem(int itemId, String reason) {
        try {
            boolean result = controller.markItemAsDefective(itemId, reason);
            if (result)
                return new Response<>(true, "Item marked as defective");
            return new Response<>("Item could not be marked as defective");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> addDiscountForProduct(int productId, int discount,
            Date startDate, Date endDate) {
        try {
            controller.addDiscountForProduct(productId, discount, startDate, endDate);
            return new Response<>(true, "Discount added to product");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> itemSell(int itemId) {
        try {
            boolean result = controller.sellItem(itemId);
            if (result)
                return new Response<>(true, "Item sold");
            return new Response<>("Item could not be sold");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Double> getPriceForProduct(int productId) {
        try {
            return new Response<>(controller.getPriceForProduct(productId), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    // --- Category Management ---

    
    public Response<List<String>> getCategories() {
        try {
            return new Response<>(controller.getAllCategories(), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Category> getCategory(String categoryName) {
        try {
            Category cat = controller.getCategory(categoryName);
            if (cat == null)
                return new Response<>("Category '" + categoryName + "' not found");
            return new Response<>(cat, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> addDiscountForCategory(String categoryName, int discount,
            Date startDate, Date endDate) {
        try {
            controller.addDiscountForCategory(categoryName, discount, startDate, endDate);
            return new Response<>(true, "Discount added to category");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    // --- Inventory Count ---

    
    public Response<Boolean> inventoryCount(int productId, int inStorage, int onShelf) {
        try {
            controller.inventoryCount(productId, inStorage, onShelf);
            return new Response<>(true, "Inventory count verified");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<List<Integer>> updateShelfAndStorageContents(int productId,
            List<Integer> itemsOnShelf, List<Integer> itemsInStock) {
        try {
            List<Integer> missing = controller.updateShelfAndStorageContents(
                productId, itemsOnShelf, itemsInStock);
            String msg = missing.isEmpty() ? "" :
                "Warning: " + missing.size() + " items could not be reconciled";
            return new Response<>(missing, msg);
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<List<Integer>> getProductIdsToRestock() {
        try {
            return new Response<>(controller.getProductsToRestock(), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public Response<List<String>> showProductToRestock() {
        try {
            return new Response<>(controller.getProductsNeedingRestock(), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> checkMinToRestock(int productId) {
        try {
            return new Response<>(controller.checkMinToRestock(productId), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<Boolean> checkRestockByItemId(int itemId) {
        try {
            return new Response<>(controller.checkRestockByItemId(itemId), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    // --- Sold Items ---

    
    public Response<List<SoldItem>> getSoldItemsByDate(Date startDate, Date endDate) {
        try {
            return new Response<>(controller.getSoldItemsByDate(startDate, endDate), "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    
    public Response<SoldItem> getSoldItemById(int itemId) {
        try {
            SoldItem item = controller.getSoldItemsById(itemId);
            if (item == null)
                return new Response<>("Sold item with ID " + itemId + " not found");
            return new Response<>(item, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    // --- Reports ---

    
    public Response<String> getInventoryReport(List<String> categoryNames) {
        Response<Report<?>> r = reportController.generateInventoryReport(categoryNames);
        if (!r.isSuccess()) return new Response<>(r.getMessage());
        return new Response<>(r.getValue().getSummary(), "");
    }

    
    public Response<String> getDefectReport() {
        Response<Report<?>> r = reportController.generateDefectReport();
        if (!r.isSuccess()) return new Response<>(r.getMessage());
        return new Response<>(r.getValue().getSummary(), "");
    }

    
    public Response<String> getDefectReport(Date startDate, Date endDate) {
        Response<Report<?>> r = reportController.generateDefectReport(startDate, endDate);
        if (!r.isSuccess()) return new Response<>(r.getMessage());
        return new Response<>(r.getValue().getSummary(), "");
    }

    
    public Response<String> getExpiredReport() {
        Response<Report<?>> r = reportController.generateExpiredReport();
        if (!r.isSuccess()) return new Response<>(r.getMessage());
        return new Response<>(r.getValue().getSummary(), "");
    }

    
    public Response<String> getExpiredReport(Date startDate, Date endDate) {
        Response<Report<?>> r = reportController.generateExpiredReport(startDate, endDate);
        if (!r.isSuccess()) return new Response<>(r.getMessage());
        return new Response<>(r.getValue().getSummary(), "");
    }

    
    public Response<String> getOrderReport() {
        Response<Report<?>> r = reportController.generateOrderReport();
        if (!r.isSuccess()) return new Response<>(r.getMessage());
        return new Response<>(r.getValue().getSummary(), "");
    }

    
    public Response<Report<?>> getReportById(int reportId) {
        return reportController.getReportById(reportId);
    }

    
    public Response<List<Report<?>>> getAllReports() {
        return reportController.getAllReports();
    }
}
