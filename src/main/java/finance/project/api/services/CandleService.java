package finance.project.api.services;

import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;

import java.util.List;

public interface CandleService {
    List<CandleDTO> getCandles(SymbolDTO symbol, String interval);

    List<CandleDTO> getLastCandles(String symbol, String timeframe, int limit);

    List<CandleDTO> getCandles(String symbol);

    List<Double> getPriceVariations(String symbol, String timeframe, int limit);

    List<CandleDTO> loadCsvTradingView(String symbolName, String timeframe, Boolean volume);

    List<Double> getPriceReturns(String symbol, String timeframe, int period);

}
