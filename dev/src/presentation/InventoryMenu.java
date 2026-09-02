package presentation;

import service.IntegrationService;
import service.InventoryService;
import service.OrderService;
import service.Response;

import java.util.List;

public class InventoryMenu {

    private final InputReader reader;
    private final InventoryService service;
    private final IntegrationService integrationService;
    private final ProductMenu productMenu;
    private final ReportMenu reportMenu;
    private final CategoryMenu categoryMenu;
    private final InventoryCountMenu inventoryCountMenu;
    private final OrderMenu orderMenu;

    public InventoryMenu(InputReader reader, InventoryService service,
                         OrderService orderService, IntegrationService integrationService) {
        this.reader = reader;
        this.service = service;
        this.integrationService = integrationService;
        this.productMenu = new ProductMenu(reader, service, integrationService);
        this.reportMenu = new ReportMenu(reader, service);
        this.categoryMenu = new CategoryMenu(reader, service);
        this.inventoryCountMenu = new InventoryCountMenu(reader, service, integrationService);
        this.orderMenu = new OrderMenu(reader, orderService, integrationService);
    }

    public void start() {
        showLowStockAlerts();
        runAutoOrder();
        sendDueScheduledOrders();
        boolean running = true;
        while (running) {
            printMenu();
            int choice = reader.readInt();
            switch (choice) {
                case 1: productMenu.start();        break;
                case 2: reportMenu.start();         break;
                case 3: categoryMenu.start();       break;
                case 4: inventoryCountMenu.start(); break;
                case 5: orderMenu.start();          break;
                case 6: running = false;            break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void showLowStockAlerts() {
        var response = service.showProductToRestock();
        if (!response.isSuccess() || response.getValue().isEmpty()) return;
        System.out.println("\n*** ALERT: The following products are below minimum stock level ***");
        for (String product : response.getValue())
            System.out.println("  - " + product);
        System.out.println("*".repeat(55));
    }

    private void runAutoOrder() {
        Response<List<String>> result = integrationService.autoOrderLowStock();
        if (!result.isSuccess()) {
            System.out.println("Auto-order failed: " + result.getMessage());
            return;
        }
        if (result.getValue().isEmpty()) return;
        System.out.println("\n--- Auto-Order Summary (shortage orders placed) ---");
        for (String line : result.getValue())
            System.out.println("  " + line);
        System.out.println("-".repeat(51));
    }

    private void sendDueScheduledOrders() {
        Response<List<String>> result = integrationService.sendDueScheduledOrders();
        if (!result.isSuccess() || result.getValue().isEmpty()) return;
        System.out.println("\n--- Scheduled Orders Sent (due tomorrow) ---");
        for (String line : result.getValue())
            System.out.println("  " + line);
        System.out.println("-".repeat(43));
    }

    private void printMenu() {
        System.out.println("\n=== INVENTORY MANAGEMENT SYSTEM ===");
        System.out.println(" 1. Product Management");
        System.out.println(" 2. Reports");
        System.out.println(" 3. Category Management");
        System.out.println(" 4. Inventory Count");
        System.out.println(" 5. Orders");
        System.out.println(" 6. Exit");
        System.out.print("Choose: ");
    }
}
