package service;

import domain.suppliers.Agreement;
import domain.suppliers.Supplier;
import domain.suppliers.SupplierController;

import java.util.List;
import java.util.Map;

/** Facade over the (mock) Suppliers module, mirroring how InventoryService wraps Inventorycontroller. */
public class SupplierService {

    private final SupplierController controller;

    public SupplierService() {
        this.controller = SupplierController.getInstance();
    }

    // Req 29
    public Response<Integer> getCheapestSupplier(String productName, int quantity) {
        try { return new Response<>(controller.searchCheapestSupplier(productName, quantity), "Cheapest supplier found"); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 26 / 19
    public Response<List<Integer>> getDeliveryDays(int supplierId) {
        try { return new Response<>(controller.getDeliveryDays(supplierId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 21
    public Response<Supplier> getSupplier(int supplierId) {
        try { return new Response<>(controller.getSupplier(supplierId), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** Does the supplier carry this product (by name)? */
    public Response<Boolean> containInCatalog(int supplierId, String productName) {
        try { return new Response<>(controller.containInCatalog(supplierId, productName), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    /** The supplier's catalog entry (price agreement) for the product, by name. */
    public Response<Agreement> getProduct(int supplierId, String productName) {
        try { return new Response<>(controller.getProduct(supplierId, productName), ""); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }

    // Req 32
    public Response<Boolean> sendOrder(int supplierId, Map<String, Integer> productQuantities) {
        try { return new Response<>(controller.sendOrder(supplierId, productQuantities), "Order sent to supplier " + supplierId); }
        catch (Exception e) { return new Response<>(e.getMessage()); }
    }
}
