package presentation;

import service.InventoryService;
import service.OrderService;
import service.Response;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TestDataLoader {

    public static void load(InventoryService service, OrderService orderService) {
        Date futureExpiry = date(2027, 1, 1);
        Date pastExpiry   = date(2000, 1, 1);
        Date today        = new Date();
        Date discountEnd  = date(2027, 1, 1);

        // --- Products ---
        // Product names match the supplier-catalog agreement names (SupplierController.seed),
        // because the product NAME is the universal identifier shared with the Suppliers module.
        ok(service.addNewProduct(1, "Milk",     "Tnuva",   "dairy",     "milk",     "whole",  10, "A1", 8));
        ok(service.addNewProduct(2, "Yogurt",   "Yotvata", "dairy",     "yogurt",   "plain",   5, "A2", 5));
        ok(service.addNewProduct(3, "Cola",     "CocaCola","beverages", "soda",     "cola",    8, "B1", 7));
        ok(service.addNewProduct(4, "Chips",    "Tapuchips","snacks",   "chips",    "salted",  6, "C1", 12));
        ok(service.addNewProduct(5, "Pretzels", "Osem",    "snacks",    "pretzels", "classic", 20,"C2", 6));

        // --- Items ---
        ok(service.addNewItem(101, 99, 3, futureExpiry, 1)); // Milk - stays in storage
        ok(service.addNewItem(102, 99, 3, futureExpiry, 1)); // Milk - will be sold
        ok(service.addNewItem(103, 99, 3, futureExpiry, 1)); // Milk - will be defective
        ok(service.addNewItem(999, 99, 3, pastExpiry,   1)); // Milk - already expired
        ok(service.addNewItem(201, 99, 3, futureExpiry, 2)); // Yogurt
        ok(service.addNewItem(301, 99, 3, futureExpiry, 3)); // Cola
        ok(service.addNewItem(302, 99, 3, futureExpiry, 3)); // Cola
        ok(service.addNewItem(401, 99, 3, futureExpiry, 4)); // Chips
        ok(service.addNewItem(501, 99, 3, futureExpiry, 5)); // Pretzels - stays in storage (below min=20)

        // --- Move to shelf ---
        ok(service.moveToShelf(Arrays.asList(102, 103, 201, 301, 302, 401)));

        // --- Sell item 102 ---
        ok(service.itemSell(102));

        // --- Mark item 103 defective ---
        ok(service.setDefectiveItem(103, "packaging damaged"));

        // --- Discounts ---
        ok(service.addDiscountForProduct(1, 20, today, discountEnd));
        ok(service.addDiscountForCategory("snacks", 10, today, discountEnd));

        // --- A sample scheduled order (supplier delivers Sun/Tue/Thu) ---
        Response<Integer> scheduled = orderService.addScheduledOrder(1, Arrays.asList(1, 3, 5));
        ok(scheduled);
        if (scheduled.isSuccess()) {
            int orderId = scheduled.getValue();
            ok(orderService.addProductToScheduledOrder(orderId, 3, 12, 7));  // Cola  @ supplier-1 price
            ok(orderService.addProductToScheduledOrder(orderId, 4, 8, 10));  // Chips @ supplier-1 price
        }

        printReferenceSheet();
    }

    private static void printReferenceSheet() {
        System.out.println();
        System.out.println("=== TEST MODE - Sample Data Loaded ===");
        System.out.println("Categories       : dairy, beverages, snacks");
        System.out.println("Products         : 1=Milk, 2=Yogurt, 3=Cola, 4=Chips, 5=Pretzels");
        System.out.println("Items in storage : 101 (Milk), 501 (Pretzels)");
        System.out.println("Items on shelf   : 201 (Yogurt), 301, 302 (Cola), 401 (Chips)");
        System.out.println("Sold item        : 102 (Milk) - sold today");
        System.out.println("Defective item   : 103 (Milk) - packaging damaged");
        System.out.println("Expired item     : 999 (Milk) - expired 01-01-2000");
        System.out.println("Restock needed   : Product 5 (Pretzels) - 1 item, min=20");
        System.out.println("Active discounts : Product 1 (Milk) 20% | Category snacks 10%");
        System.out.println("Scheduled order   : #1 supplier 1, days Sun/Tue/Thu, products 3 & 4");
        System.out.println("======================================");
        System.out.println();
    }

    private static Date date(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static void ok(Response<?> response) {
        if (!response.isSuccess())
            System.out.println("[TestDataLoader] Warning: " + response.getMessage());
    }
}
