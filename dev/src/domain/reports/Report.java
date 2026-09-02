package domain.reports;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public abstract class Report<T> {

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private int reportId = 0;
    private List<T> data = null;
    private Date publishDate = null;

    public Report(int reportId, List<T> data, Date publishDate) {
        this.reportId = reportId;
        this.data = data;
        this.publishDate = publishDate;
    }

    public int getReportId() { return reportId; }
    public List<T> getData() { return data; }
    public Date getPublishDate() { return publishDate; }
    public abstract String getSummary();

    @Override
    public String toString() { return getSummary(); }
}
