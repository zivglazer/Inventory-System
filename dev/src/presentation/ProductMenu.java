package presentation;

import service.IntegrationService;
import service.InventoryService;
import service.Response;
import domain.inventory.Product;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductMenu {

    private final InputReader reader;
    private final InventoryService service;
    private final IntegrationService integrationService;
    private final SimpleDateFormat dateFormat;

    public ProductMenu(InputReader reader, InventoryService service, IntegrationService integrationService) {
        this.reader = reader;
        this.service = service;
        this.integrationService = integrationService;
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.dateFormat.setLenient(false);
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = reader.readInt();
            switch (choice) {
                case 1:  addNewProduct();           break;
                case 2:  addItem();                 break;
                case 3:  updateMinRestock();         break;
                case 4:  moveToShelf();              break;
                case 5:  markDefective();            break;
                case 6:  addProductDiscount();       break;
                case 7:  sellItem();                 break;
                case 8:  getProductPrice();          break;
                case 9:  getProductInfo();           break;
                case 10: removeItem();               break;
                case 11: removeProduct();            break;
                case 0:  running = false;            break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== PRODUCT MENU ===");
        System.out.println(" 1.  Add New Product");
        System.out.println(" 2.  Add Item to Product");
        System.out.println(" 3.  Update Minimum Restock Level");
        System.out.println(" 4.  Move Items to Shelf");
        System.out.println(" 5.  Mark Item as Defective");
        System.out.println(" 6.  Add Product Discount");
        System.out.println(" 7.  Sell Item");
        System.out.println(" 8.  Get Product Price");
        System.out.println(" 9.  Get Product Info");
        System.out.println(" 10. Remove Item");
        System.out.println(" 11. Remove Product");
        System.out.println(" 0.  Back");
        System.out.print("Choose: ");
    }

    private void addNewProduct() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();
        System.out.print("Name: ");
        String name = reader.readString();
        System.out.print("Manufacturer: ");
        String manufacturer = reader.readString();
        System.out.print("Category: ");
        String category = reader.readString();
        System.out.print("Sub-Category: ");
        String subCategory = reader.readString();
        System.out.print("Sub-Sub-Category: ");
        String subSubCategory = reader.readString();
        System.out.print("Min to Restock: ");
        int minToRestock = reader.readInt();
        System.out.print("Shelf Location: ");
        String shelfLocation = reader.readString();
        System.out.print("Price (without discount): ");
        int price = reader.readInt();

        Response<Boolean> response = service.addNewProduct(
                productId, name, manufacturer, category,
                subCategory, subSubCategory, minToRestock, shelfLocation, price);

        if (response.isSuccess())
            System.out.println("Product added successfully.");
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void addItem() {
        System.out.print("Item ID (barcode): ");
        int itemId = reader.readInt();
        System.out.print("Supplier ID: ");
        int supplierId = reader.readInt();
        System.out.print("Cost Price: ");
        int costPrice = reader.readInt();
        Date expirationDate = promptDate("Expiration Date (dd-MM-yyyy): ");
        System.out.print("Product ID: ");
        int productId = reader.readInt();

        Response<Boolean> response = service.addNewItem(itemId, supplierId, costPrice, expirationDate, productId);

        if (response.isSuccess())
            System.out.println("Item added successfully.");
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void updateMinRestock() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();
        System.out.print("New minimum restock quantity: ");
        int min = reader.readInt();

        Response<Boolean> response = service.updateMinToRestock(productId, min);

        if (response.isSuccess())
            System.out.println("Minimum restock level updated.");
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void moveToShelf() {
        System.out.print("Enter item IDs to move to shelf (comma-separated, e.g. 101,102,103): ");
        String input = reader.readString();
        List<Integer> itemIds = parseIdList(input);

        if (itemIds.isEmpty()) {
            System.out.println("No valid item IDs entered.");
            return;
        }

        Response<Boolean> response = service.moveToShelf(itemIds);

        if (response.isSuccess())
            System.out.println("Items moved to shelf successfully.");
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void markDefective() {
        System.out.print("Item ID: ");
        int itemId = reader.readInt();
        System.out.print("Reason: ");
        String reason = reader.readString();

        Response<List<String>> response = integrationService.markDefective(itemId, reason);

        if (response.isSuccess()) {
            System.out.println("Item marked as defective.");
            printAutoOrder(response.getValue());
        } else
            System.out.println("Error: " + response.getMessage());
    }

    private void addProductDiscount() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();
        System.out.print("Discount percentage (0-100): ");
        int discount = reader.readInt();
        Date startDate = promptDate("Start Date (dd-MM-yyyy): ");
        Date endDate = promptDate("End Date (dd-MM-yyyy): ");

        Response<Boolean> response = service.addDiscountForProduct(productId, discount, startDate, endDate);

        if (response.isSuccess())
            System.out.println("Discount added to product.");
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void sellItem() {
        System.out.print("Item ID: ");
        int itemId = reader.readInt();

        Response<List<String>> response = integrationService.sellItem(itemId);

        if (response.isSuccess()) {
            System.out.println("Item sold successfully.");
            printAutoOrder(response.getValue());
        } else
            System.out.println("Error: " + response.getMessage());
    }

    private void getProductPrice() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();

        Response<Double> response = service.getPriceForProduct(productId);

        if (response.isSuccess())
            System.out.println("Current price: " + response.getValue());
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void getProductInfo() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();

        Response<Product> response = service.getProduct(productId);

        if (response.isSuccess())
            System.out.println(response.getValue());
        else
            System.out.println("Error: " + response.getMessage());
    }

    private void removeItem() {
        System.out.print("Item ID: ");
        int itemId = reader.readInt();

        Response<List<String>> response = integrationService.removeItem(itemId);

        if (response.isSuccess()) {
            System.out.println("Item removed.");
            printAutoOrder(response.getValue());
        } else
            System.out.println("Error: " + response.getMessage());
    }

    /** Show any shortage orders the action just triggered. */
    private void printAutoOrder(List<String> summary) {
        if (summary == null || summary.isEmpty()) return;
        System.out.println("--- Auto-order triggered (stock below minimum) ---");
        for (String line : summary)
            System.out.println("  -> " + line);
    }

    private void removeProduct() {
        System.out.print("Product ID: ");
        int productId = reader.readInt();

        Response<Boolean> response = service.removeProduct(productId);

        if (response.isSuccess())
            System.out.println("Product removed.");
        else
            System.out.println("Error: " + response.getMessage());
    }

    private Date promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = reader.readString();
            try {
                return dateFormat.parse(input);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please use dd-MM-yyyy.");
            }
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
