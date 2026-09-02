package dataaccess;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Converts {@link Date} ⇄ ISO-8601 text for storage in TEXT columns. */
public final class Dates {

    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private Dates() {}

    public static String format(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat(PATTERN).format(date);
    }

    public static Date parse(String iso) {
        if (iso == null) return null;
        try {
            return new SimpleDateFormat(PATTERN).parse(iso);
        } catch (ParseException e) {
            throw new RuntimeException("Bad date in DB: " + iso, e);
        }
    }
}
