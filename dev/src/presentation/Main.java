package presentation;

import service.IntegrationService;
import service.InventoryService;
import service.OrderService;
import service.ScheduledOrderRunner;
import service.SupplierService;

public class Main {
    public static void main(String[] args) {
        InputReader reader = new InputReaderScanner();
        InventoryService inventoryService = new InventoryService();
        OrderService orderService = new OrderService();
        SupplierService supplierService = new SupplierService();
        IntegrationService integrationService =
                new IntegrationService(inventoryService, orderService, supplierService);

        int mode = 0;
        while (mode != 1 && mode != 2) {
            System.out.println("\n=== INVENTORY MANAGEMENT SYSTEM ===");
            System.out.println("Select startup mode:");
            System.out.println("  1. Live Mode");
            System.out.println("  2. Test Mode  (pre-loaded sample data)");
            System.out.print("Choose: ");
            mode = reader.readInt();
            if (mode != 1 && mode != 2)
                System.out.println("Invalid option. Please enter 1 or 2.");
        }

        if (mode == 2) {
            // Seed sample data only on a fresh database; otherwise the persisted data is kept.
            if (inventoryService.getCategories().getValue().isEmpty())
                TestDataLoader.load(inventoryService, orderService);
            else
                System.out.println("Database already contains data - keeping it (sample load skipped).");
        }

        // Daily mechanism: convert scheduled templates that are due tomorrow into real orders (17:49).
        new ScheduledOrderRunner(integrationService).start();

        new InventoryMenu(reader, inventoryService, orderService, integrationService).start();
    }
}
