package finance.project.api.services;

import finance.project.api.model.PerformanceComparisonDTO;

import java.time.LocalDate;

public interface ComparisonService {

    PerformanceComparisonDTO comparePerformance(String symbol1, String symbol2, LocalDate startDate, LocalDate endDate);

}
