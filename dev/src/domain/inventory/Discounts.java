package domain.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Discounts {
    private List<DiscountInfo> discounts;

    public Discounts() {
        this.discounts = new ArrayList<>();
    }

        public void addDiscount(DiscountInfo discountInfo) {
            this.discounts.add(discountInfo);
        }
    public double getBestActiveDiscount(Date date) {
        double best = 0;
        for (DiscountInfo discount : discounts) {
            if (discount.isActiveOn(date) && discount.getPercentage() > best) {
                best = discount.getPercentage();
            }
        }
        return best;
    }

    }
