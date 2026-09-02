package domain.suppliers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dataaccess.Database;
import dataaccess.dao.AgreementDAO;
import dataaccess.dao.SupplierDAO;
import dataaccess.dao.SupplierDeliveryDayDAO;
import dataaccess.dto.AgreementDTO;
import dataaccess.dto.SupplierDTO;
import dataaccess.mapper.AgreementMapper;
import dataaccess.mapper.SupplierMapper;

/**
 * Mock Suppliers module — but with real logic in the domain and now real persistence, exactly
 * like {@code Inventorycontroller}. Suppliers + their price agreements live in the database;
 * this controller loads the domain objects an operation needs and runs the business logic
 * (cheapest-supplier comparison, delivery days, send order) on them.
 */
public class SupplierController {
    private static SupplierController instance = null;

    private final SupplierDAO supplierDAO          = new SupplierDAO();
    private final SupplierDeliveryDayDAO dayDAO     = new SupplierDeliveryDayDAO();
    private final AgreementDAO agreementDAO         = new AgreementDAO();

    private SupplierController() {
        if (supplierDAO.isEmpty()) seed();   // production: seed only on a fresh database
    }

    public static SupplierController getInstance() {
        if (instance == null)
            instance = new SupplierController();
        return instance;
    }

    public void resetAll() {
        Database.clearTables(Arrays.asList("agreement", "supplier_delivery_day", "supplier"));
        Database.clearAllCaches();
        seed();
    }

    /**
     * Pre-loaded mock data (the supplier module "already has" suppliers and agreements). Agreements
     * are keyed by product NAME -- the universal identifier shared with Inventory -- so these names
     * must match the supermarket's product names (see {@code TestDataLoader}).
     */
    private void seed() {
        addSupplier(new Supplier(1, "Tnuva Logistics",   "1 Dairy Rd",  "03-1111111", "Dani", Arrays.asList(1, 3, 5)));
        addSupplier(new Supplier(2, "Osem Distribution", "2 Snack Blvd", "03-2222222", "Rina", Arrays.asList(2, 4)));

        // Supplier 1 is generally cheapest; supplier 2 wins "pretzels" only in bulk (>=20 units).
        addAgreement(new Agreement(1, "milk", 5));
        addAgreement(new Agreement(1, "yogurt", 4));
        addAgreement(new Agreement(1, "cola", 6));
        addAgreement(new Agreement(1, "chips", 10));
        addAgreement(new Agreement(1, "pretzels", 6));
        addAgreement(new Agreement(2, "milk", 6));
        addAgreement(new Agreement(2, "cola", 7));
        addAgreement(new Agreement(2, "pretzels", 7, 20, 4)); // bulk: 4/unit at qty >= 20
    }

    public void addSupplier(Supplier supplier) {
        Database.runInTransaction(() -> {
            supplierDAO.insert(SupplierMapper.toDTO(supplier));
            for (int day : supplier.getDeliveryDays())
                dayDAO.insert(supplier.getSupplierId(), day);
        });
    }

    public void addAgreement(Agreement agreement) {
        if (supplierDAO.find(agreement.getSupplierId()) == null)
            throw new IllegalArgumentException("Supplier " + agreement.getSupplierId() + " doesn't exist");
        agreementDAO.insert(AgreementMapper.toDTO(agreement));
    }

    public Supplier getSupplier(int supplierId) {
        SupplierDTO dto = supplierDAO.find(supplierId);
        if (dto == null)
            throw new IllegalArgumentException("Supplier " + supplierId + " doesn't exist");
        List<Agreement> catalog = new ArrayList<>();
        for (AgreementDTO a : agreementDAO.findBySupplier(supplierId))
            catalog.add(AgreementMapper.toDomain(a));
        return SupplierMapper.toDomain(dto, dayDAO.findBySupplier(supplierId), catalog);
    }

    /** Req: does this supplier carry the product (by name)? Delegates to the Supplier aggregate. */
    public boolean containInCatalog(int supplierId, String productName) {
        return getSupplier(supplierId).containsProduct(productName);
    }

    /** The supplier's catalog entry (price agreement) for the product, by name. */
    public Agreement getProduct(int supplierId, String productName) {
        return getSupplier(supplierId).getProduct(productName);
    }

    public List<Integer> getDeliveryDays(int supplierId) {
        return getSupplier(supplierId).getDeliveryDays();
    }

    /**
     * Req 29: cheapest supplier for a product + quantity, by the price agreements (the domain
     * {@link Agreement#totalPriceFor} does the bulk-tier math). Falls back to the default
     * (lowest-id) supplier when no agreement covers the product.
     */
    public int searchCheapestSupplier(String productName, int quantity) {
        int bestSupplier = -1;
        int bestPrice = Integer.MAX_VALUE;
        for (AgreementDTO dto : agreementDAO.findByProduct(productName)) {
            Agreement agreement = AgreementMapper.toDomain(dto);
            int total = agreement.totalPriceFor(quantity);
            if (total < bestPrice) {
                bestPrice = total;
                bestSupplier = agreement.getSupplierId();
            }
        }
        if (bestSupplier != -1) return bestSupplier;

        int defaultSupplier = supplierDAO.minId();
        if (defaultSupplier == -1)
            throw new IllegalStateException("No suppliers available");
        return defaultSupplier;
    }

    /** Req 32: hand the order's products (by name) to the supplier; the mock validates and acknowledges. */
    public boolean sendOrder(int supplierId, Map<String, Integer> productQuantities) {
        getSupplier(supplierId); // validates the supplier exists
        return true;
    }
}
