package finance.project.api.utils;

import java.time.LocalDate;

public class DateUtils {
    public static LocalDate getStartDate(TimePeriod period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case LAST_1_YEAR: return now.minusYears(1);
            case LAST_3_YEARS: return now.minusYears(3);
            case LAST_5_YEARS: return now.minusYears(5);
            case LAST_10_YEARS: return now.minusYears(10);
            default: return now;
        }
    }
}

