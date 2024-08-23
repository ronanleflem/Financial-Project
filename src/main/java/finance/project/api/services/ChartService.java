package finance.project.api.services;

import finance.project.api.model.ChartDataDTO;

import java.util.List;

public interface ChartService {

    List<ChartDataDTO> getChartData(String symbol, String interval);
}
