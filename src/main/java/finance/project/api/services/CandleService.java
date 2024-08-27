package finance.project.api.services;

import finance.project.api.model.CandleDTO;

import java.util.List;

public interface CandleService {

    List<CandleDTO> getCandles(String symbol, String interval);
}
