package domain.inventory;

import java.util.Date;

/** Represents a defective or expired item (UML: defectItems) */
public class DefectItem extends Item {
    private Date updateDate;
    private String reason;

    public DefectItem(Item item, String reason, Date updateDate) {
        super(item);
        this.updateDate = updateDate;
        this.reason = reason;
    }
    
    public DefectItem(Item item, String reason) {
        super(item);
        this.updateDate = new Date();
        this.reason = reason;
    }
    public Date getUpdateDate() { return updateDate; }
    public String getReason() { return reason; }

    public boolean inRangeDates(Date startDate, Date endDate) {
        return !updateDate.before(startDate) && !updateDate.after(endDate);
    }
}
