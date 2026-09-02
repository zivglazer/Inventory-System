package presentation;

import service.InventoryService;
import service.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CategoryMenu {

    private final InputReader reader;
    private final InventoryService service;
    private final SimpleDateFormat dateFormat;

    public CategoryMenu(InputReader reader, InventoryService service) {
        this.reader = reader;
        this.service = service;
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.dateFormat.setLenient(false);
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = reader.readInt();
            switch (choice) {
                case 1: viewAllCategories();    break;
                case 2: addCategoryDiscount();  break;
                case 0: running = false;        break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== CATEGORY MENU ===");
        System.out.println(" 1. View All Categories");
        System.out.println(" 2. Add Category Discount");
        System.out.println(" 0. Back");
        System.out.print("Choose: ");
    }

    private void viewAllCategories() {
        Response<List<String>> response = service.getCategories();
        if (!response.isSuccess()) {
            System.out.println("Error: " + response.getMessage());
            return;
        }
        List<String> categories = response.getValue();
        if (categories.isEmpty()) {
            System.out.println("No categories found.");
            return;
        }
        System.out.println("\n--- Categories ---");
        for (String category : categories) {
            System.out.println("  " + category);
        }
    }

    private void addCategoryDiscount() {
        System.out.print("Category name: ");
        String categoryName = reader.readString();
        System.out.print("Discount percentage (0-100): ");
        int discount = reader.readInt();
        Date startDate = promptDate("Start Date (dd-MM-yyyy): ");
        Date endDate = promptDate("End Date (dd-MM-yyyy): ");

        Response<Boolean> response = service.addDiscountForCategory(categoryName, discount, startDate, endDate);

        if (response.isSuccess())
            System.out.println("Discount added to category.");
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
}
