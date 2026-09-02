package presentation;

import service.InventoryService;
import service.Response;
import domain.reports.Report;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReportMenu {

    private final InputReader reader;
    private final InventoryService service;
    private final SimpleDateFormat dateFormat;

    public ReportMenu(InputReader reader, InventoryService service) {
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
                case 1: inventoryReport();   break;
                case 2: defectReport();      break;
                case 3: expiredReport();     break;
                case 4: orderReport();       break;
                case 5: viewAllReports();    break;
                case 0: running = false;     break;
                default: System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== REPORTS MENU ===");
        System.out.println(" 1. Inventory Report");
        System.out.println(" 2. Defective Items Report");
        System.out.println(" 3. Expired Items Report");
        System.out.println(" 4. Order / Restock Report");
        System.out.println(" 5. View All Reports");
        System.out.println(" 0. Back");
        System.out.print("Choose: ");
    }

    private void inventoryReport() {
        Response<List<String>> catResponse = service.getCategories();
        if (!catResponse.isSuccess()) {
            System.out.println("Error fetching categories: " + catResponse.getMessage());
            return;
        }

        List<String> allCategories = catResponse.getValue();
        if (allCategories.isEmpty()) {
            System.out.println("No categories available.");
            return;
        }

        System.out.println("\nAvailable categories:");
        for (String cat : allCategories) {
            System.out.println("  " + cat);
        }
        System.out.print("Enter category names (comma-separated) or press Enter for all: ");
        String input = reader.readString();

        List<String> selected;
        if (input.isEmpty()) {
            selected = allCategories;
        } else {
            selected = new ArrayList<>();
            for (String part : input.split(",")) {
                selected.add(part.trim());
            }
        }

        Response<String> response = service.getInventoryReport(selected);
        printReportResult(response);
    }

    private void defectReport() {
        Date[] range = promptDateRange();
        Response<String> response;
        if (range == null) {
            response = service.getDefectReport();
        } else {
            response = service.getDefectReport(range[0], range[1]);
        }
        printReportResult(response);
    }

    private void expiredReport() {
        Date[] range = promptDateRange();
        Response<String> response;
        if (range == null) {
            response = service.getExpiredReport();
        } else {
            response = service.getExpiredReport(range[0], range[1]);
        }
        printReportResult(response);
    }

    private void orderReport() {
        Response<String> response = service.getOrderReport();
        printReportResult(response);
    }

    private void viewAllReports() {
        Response<List<Report<?>>> response = service.getAllReports();
        if (!response.isSuccess()) {
            System.out.println("Error: " + response.getMessage());
            return;
        }
        List<Report<?>> reports = response.getValue();
        if (reports.isEmpty()) {
            System.out.println("No reports found.");
            return;
        }
        for (Report<?> report : reports) {
            System.out.println("\n--- Report #" + report.getReportId()
                    + " | Generated: " + dateFormat.format(report.getPublishDate()) + " ---");
            System.out.println(report.getSummary());
        }
    }

    /**
     * Prompts the user to choose a date range preset or enter a custom range.
     * Returns a Date[2] {startDate, endDate}, or null for "all time".
     */
    private Date[] promptDateRange() {
        System.out.println("\n  Date range:");
        System.out.println("  1. All time");
        System.out.println("  2. Last week");
        System.out.println("  3. Last month");
        System.out.println("  4. Last 3 months");
        System.out.println("  5. Custom date range");
        System.out.print("  Choose: ");

        int choice = reader.readInt();
        Date now = new Date();

        switch (choice) {
            case 1: return null;
            case 2: return new Date[]{daysAgo(7), now};
            case 3: return new Date[]{monthsAgo(1), now};
            case 4: return new Date[]{monthsAgo(3), now};
            case 5:
                Date start = promptDate("  Start Date (dd-MM-yyyy): ");
                Date end = promptDate("  End Date (dd-MM-yyyy): ");
                return new Date[]{start, end};
            default:
                System.out.println("Invalid option, using all time.");
                return null;
        }
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return cal.getTime();
    }

    private Date monthsAgo(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        return cal.getTime();
    }

    private Date promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = reader.readString();
            try {
                return dateFormat.parse(input);
            } catch (ParseException e) {
                System.out.println("  Invalid date format. Please use dd-MM-yyyy.");
            }
        }
    }

    private void printReportResult(Response<String> response) {
        if (response.isSuccess())
            System.out.println("\n" + response.getValue());
        else
            System.out.println("Error: " + response.getMessage());
    }
}
