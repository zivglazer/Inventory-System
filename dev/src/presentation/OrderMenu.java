package presentation;

import domain.orders.Order;
import domain.orders.OrderLine;
import domain.orders.ScheduledOrder;
import domain.suppliers.Supplier;
import service.IntegrationService;
import service.OrderService;
import service.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderMenu {

    private final InputReader reader;
    private final OrderService orderService;
    private final IntegrationService integrationService;
    private final SimpleDateFormat dateFormat;

    public OrderMenu(InputReader reader, OrderService orderService, IntegrationService integrationService) {
        this.reader = reader;
        this.orderService = orderService;
        this.integrationService = integrationService;
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        this.dateFormat.setLenient(false);
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            switch (reader.readInt()) {
                case 1: viewAllOrders();   break;
                case 2: scheduledMenu();    break;
                case 3: receiveDelivery(); break;
                case 0: running = false;   break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== ORDER MENU ===");
        System.out.println(" 1. View All Orders");
        System.out.println(" 2. Scheduled Orders");
        System.out.println(" 3. Receive Delivery");
        System.out.println(" 0. Back");
        System.out.print("Choose: ");
    }

    private void viewAllOrders() {
        Response<List<Order>> r = orderService.getAllOrders();
        if (!r.isSuccess()) { System.out.println("Error: " + r.getMessage()); return; }
        if (r.getValue().isEmpty()) { System.out.println("No orders yet."); return; }
        System.out.println();
        for (Order o : r.getValue()) printOrder(o);
    }

    /**
     * Print an order with each line shown by product NAME (the universal id). Orders store the
     * store's internal productId; we resolve it to the name for display, falling back to the id
     * if the product is no longer in inventory.
     */
    private void printOrder(Order o) {
        System.out.println(o.getType() + " Order #" + o.getOrderId()
                + " [supplier " + o.getSupplierId() + ", " + o.getStatus() + "]");
        for (OrderLine line : o.getLines()) {
            Response<String> name = integrationService.getProductName(line.getProductId());
            String label = name.isSuccess() ? name.getValue() : ("product " + line.getProductId());
            System.out.println("   " + label + " x " + line.getQuantity()
                    + "   (productId " + line.getProductId() + ")");
        }
    }

    // ── Scheduled orders ────────────────────────────────────────────────────
    private void scheduledMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== SCHEDULED ORDERS ===");
            System.out.println(" 1. Create Scheduled Order");
            System.out.println(" 2. List Scheduled Orders");
            System.out.println(" 3. Add Product to Order");
            System.out.println(" 4. Edit Product Quantity");
            System.out.println(" 5. Add Delivery Day");
            System.out.println(" 6. Remove Delivery Day");
            System.out.println(" 7. Remove Scheduled Order");
            System.out.println(" 0. Back");
            System.out.print("Choose: ");
            switch (reader.readInt()) {
                case 1: createScheduled();    break;
                case 2: listScheduled();      break;
                case 3: addProduct();        break;
                case 4: editQuantity();      break;
                case 5: addDeliveryDay();    break;
                case 6: removeDeliveryDay(); break;
                case 7: removeScheduled();    break;
                case 0: running = false;     break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void createScheduled() {
        int supplierId = promptInt("Supplier ID: ");
        List<Integer> days = promptDeliveryDays();
        if (days.isEmpty()) { System.out.println("At least one delivery day is required."); return; }
        Response<Integer> r = integrationService.addScheduledOrder(supplierId, days);
        System.out.println(r.isSuccess() ? "Created scheduled order #" + r.getValue() : "Error: " + r.getMessage());
    }

    private void listScheduled() {
        Response<List<ScheduledOrder>> r = orderService.getAllScheduledOrders();
        if (!r.isSuccess()) { System.out.println("Error: " + r.getMessage()); return; }
        if (r.getValue().isEmpty()) { System.out.println("No scheduled orders."); return; }
        for (ScheduledOrder o : r.getValue()) {
            printOrder(o);
            System.out.println("   delivery days: " + o.getDeliveryDays());
            Response<Supplier> info = integrationService.getSupplier(o.getSupplierId());
            if (info.isSuccess())
                System.out.println("   supplier: " + info.getValue().getName()
                        + " | " + info.getValue().getPhone()
                        + " | " + info.getValue().getContactPerson());
        }
    }

    private void addProduct() {
        int orderId = promptInt("Scheduled order ID: ");
        int productId = promptInt("Product ID: ");
        int qty = promptInt("Quantity: ");
        Response<Boolean> r = integrationService.addProductToOrder(orderId, productId, qty);
        System.out.println(r.isSuccess() ? "Added." : "Error: " + r.getMessage());
    }

    private void editQuantity() {
        int orderId = promptInt("Scheduled order ID: ");
        int productId = promptInt("Product ID: ");
        int qty = promptInt("New quantity: ");
        Response<Boolean> r = orderService.editProductQuantityInScheduledOrder(orderId, productId, qty);
        System.out.println(r.isSuccess() ? "Updated." : "Error: " + r.getMessage());
    }

    private void addDeliveryDay() {
        int orderId = promptInt("Scheduled order ID: ");
        int day = promptInt("Delivery day (1=Sun..7=Sat): ");
        Response<Boolean> r = integrationService.addDeliveryDayToScheduledOrder(orderId, day);
        System.out.println(r.isSuccess() ? "Added." : "Error: " + r.getMessage());
    }

    private void removeDeliveryDay() {
        int orderId = promptInt("Scheduled order ID: ");
        int day = promptInt("Delivery day to remove (1..7): ");
        Response<Boolean> r = orderService.removeDeliveryDayFromScheduledOrder(orderId, day);
        System.out.println(r.isSuccess() ? "Removed." : "Error: " + r.getMessage());
    }

    private void removeScheduled() {
        int orderId = promptInt("Scheduled order ID: ");
        Response<Boolean> r = orderService.removeScheduledOrder(orderId);
        System.out.println(r.isSuccess() ? "Removed." : "Error: " + r.getMessage());
    }

    // ── Receiving (Req 30/31) ────────────────────────────────────────────────
    private void receiveDelivery() {
        int orderId = promptInt("Order ID being received: ");
        System.out.println("Enter delivered units. Enter 0 as Item ID to finish.");
        List<IntegrationService.ReceivedUnit> units = new ArrayList<>();
        while (true) {
            int itemId = promptInt("Item ID (barcode, 0 to stop): ");
            if (itemId == 0) break;
            int productId = promptInt("Product ID: ");
            int cost = promptInt("Cost Price: ");
            Date expiry = promptDate("Expiration Date (dd-MM-yyyy): ");
            units.add(new IntegrationService.ReceivedUnit(itemId, productId, cost, expiry));
        }
        if (units.isEmpty()) { System.out.println("No units entered; nothing received."); return; }
        Response<Boolean> r = integrationService.receiveOrder(orderId, units);
        System.out.println(r.isSuccess() ? r.getMessage() : "Error: " + r.getMessage());
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private List<Integer> promptDeliveryDays() {
        System.out.print("Delivery days (1=Sun..7=Sat, comma-separated): ");
        String input = reader.readString();
        List<Integer> days = new ArrayList<>();
        for (String part : input.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            try { days.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
        }
        return days;
    }

    private int promptInt(String prompt) {
        System.out.print(prompt);
        return reader.readInt();
    }

    private Date promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return dateFormat.parse(reader.readString()); }
            catch (ParseException e) { System.out.println("Invalid date. Please use dd-MM-yyyy."); }
        }
    }
}
