package presentation;

import service.IntegrationService;
import service.InventoryService;
import service.Response;

import java.util.ArrayList;
import java.util.List;

public class InventoryCountMenu {

    private final InputReader reader;
    private final InventoryService service;
    private final IntegrationService integrationService;

    public InventoryCountMenu(InputReader reader, InventoryService service, IntegrationService integrationService) {
        this.reader = reader;
        this.service = service;
        this.integrationService = integrationService;
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = reader.readInt();
            switch (choice) {
                case 1: regularCount();          break;
                case 2: updateInventory();       break;
                case 3: showProductsToRestock(); break;
                case 0: running = false;         break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== INVENTORY COUNT MENU ===");
        System.out.println(" 1. Regular Count (verify amounts)");
        System.out.println(" 2. Update Product Inventory (reconcile)");
        System.out.println(" 3. Show Products to Restock");
        System.out.println(" 0. Back");
        System.out.print("Choose: ");
    }

    private void regularCount() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();
        System.out.print("Count in storage: ");
        int inStorage = reader.readInt();
        System.out.print("Count on shelf: ");
        int onShelf = reader.readInt();

        Response<Boolean> response = service.inventoryCount(productId, inStorage, onShelf);

        if (response.isSuccess())
            System.out.println("Inventory count verified successfully.");
        else
            System.out.println("Mismatch: " + response.getMessage());
    }

    private void updateInventory() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();

        System.out.print("Enter shelf item IDs (comma-separated, or - for none): ");
        String shelfInput = reader.readString();
        List<Integer> shelfItems = shelfInput.equals("-") ? new ArrayList<>() : parseIdList(shelfInput);

        System.out.print("Enter storage item IDs (comma-separated, or - for none): ");
        String storageInput = reader.readString();
        List<Integer> storageItems = storageInput.equals("-") ? new ArrayList<>() : parseIdList(storageInput);

        Response<List<Integer>> response = service.updateShelfAndStorageContents(productId, shelfItems, storageItems);

        if (response.isSuccess()) {
            List<Integer> missing = response.getValue();
            if (missing.isEmpty()) {
                System.out.println("Inventory reconciled successfully. No missing items.");
            } else {
                System.out.println("Inventory reconciled. Missing/unrecognized item IDs: " + missing);
            }
            // A reconciliation can remove items and push the product below its minimum.
            Response<List<String>> auto = integrationService.autoOrderForProduct(productId);
            if (auto.isSuccess() && !auto.getValue().isEmpty()) {
                System.out.println("--- Auto-order triggered (stock below minimum) ---");
                for (String line : auto.getValue())
                    System.out.println("  -> " + line);
            }
        } else {
            System.out.println("Error: " + response.getMessage());
        }
    }

    private void showProductsToRestock() {
        Response<List<String>> response = service.showProductToRestock();

        if (!response.isSuccess()) {
            System.out.println("Error: " + response.getMessage());
            return;
        }

        List<String> products = response.getValue();
        if (products.isEmpty()) {
            System.out.println("All products are sufficiently stocked.");
            return;
        }

        System.out.println("\n--- Products Needing Restock ---");
        for (String product : products) {
            System.out.println(product);
            System.out.println();
        }
    }

    private List<Integer> parseIdList(String input) {
        List<Integer> ids = new ArrayList<>();
        for (String part : input.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid ID: " + part.trim());
            }
        }
        return ids;
    }
}
