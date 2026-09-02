package domain.reports;

import service.Response;
import domain.inventory.*;
import java.util.*;

public class Reportcontroller {
    private static Reportcontroller instance = null;
    private int reportCount;
    private List<Report<?>> reports;
    private final Inventorycontroller inventoryController;

    private Reportcontroller() {
        this.reportCount = 0;
        this.reports = new ArrayList<>();
        this.inventoryController = Inventorycontroller.getInstance();
    }

    public static Reportcontroller getInstance() {
        if (instance == null)
            instance = new Reportcontroller();
        return instance;
    }

    public int generateReportId() { return ++reportCount; }

    public Response<Boolean> saveReport(Report<?> report) {
        try {
            if (report == null) return new Response<>("Report cannot be null");
            reports.add(report);
            return new Response<>(true, "Report saved");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public Response<Report<?>> getReportById(int reportId) {
        for (Report<?> report : reports)
            if (report.getReportId() == reportId)
                return new Response<>(report, "");
        return new Response<>("Report with ID " + reportId + " not found");
    }

    public Response<List<Report<?>>> getAllReports() {
        return new Response<>(new ArrayList<>(reports), "");
    }

    // --- Report Generation ---

    public Response<Report<?>> generateInventoryReport(List<String> categoryNames) {
        try {
            List<Product> products = new ArrayList<>();
            for (String name : categoryNames) {
                Category cat = inventoryController.getCategory(name);
                if (cat != null) products.addAll(cat.getProducts());
            }
            InventoryReport report = new InventoryReport(
                generateReportId(), products, categoryNames, new Date());
            saveReport(report);
            return new Response<>(report, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public Response<Report<?>> generateDefectReport() {
        try {
            List<DefectItem> items = new ArrayList<>(inventoryController.getDefectiveItems());
            DefectReport report = new DefectReport(generateReportId(), items, new Date(0), new Date());
            saveReport(report);
            return new Response<>(report, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public Response<Report<?>> generateDefectReport(Date startDate, Date endDate) {
        try {
            List<DefectItem> items = inventoryController.getDefectiveItemsByDateRange(startDate, endDate);
            DefectReport report = new DefectReport(generateReportId(), items, startDate, endDate);
            saveReport(report);
            return new Response<>(report, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public Response<Report<?>> generateExpiredReport() {
        try {
            List<Item> items = inventoryController.getExpiredItems();
            ExpiredReport report = new ExpiredReport(
                generateReportId(), items, new Date(0), new Date());
            saveReport(report);
            return new Response<>(report, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public Response<Report<?>> generateExpiredReport(Date startDate, Date endDate) {
        try {
            List<Item> filtered = new ArrayList<>();
            for (Item item : inventoryController.getExpiredItems()) {
                Date exp = item.getExpirationDate();
                if (!exp.before(startDate) && !exp.after(endDate))
                    filtered.add(item);
            }
            ExpiredReport report = new ExpiredReport(
                generateReportId(), filtered, startDate, endDate);
            saveReport(report);
            return new Response<>(report, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

    public void resetAll() {
        this.reportCount = 0;
        this.reports = new ArrayList<>();
    }

    public Response<Report<?>> generateOrderReport() {
        try {
            List<Product> products = new ArrayList<>();
            for (String catName : inventoryController.getAllCategories()) {
                Category cat = inventoryController.getCategory(catName);
                for (Product p : cat.getProducts()) {
                    if (p.needsRestock()) products.add(p);
                }
            }
            OrderReport report = new OrderReport(generateReportId(), products, new Date());
            saveReport(report);
            return new Response<>(report, "");
        } catch (Exception e) {
            return new Response<>(e.getMessage());
        }
    }

}
