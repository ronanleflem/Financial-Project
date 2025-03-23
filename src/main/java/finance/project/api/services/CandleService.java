package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.entities.PointOfInterest;
import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.CandleFilterDTO;
import finance.project.api.model.SymbolDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleService {
    List<CandleDTO> getCandles(SymbolDTO symbol, String interval);

    List<CandleDTO> getCandlesByTimeframeAndIntervalDate(String symbol, String timeframe, LocalDateTime startDate, LocalDateTime endDate);
    List<CandleDTO> getLastCandles(String symbol, String timeframe, int limit);

    List<CandleDTO> getCandles(String symbol);

    List<Double> getPriceVariations(String symbol, String timeframe, int limit);

    List<CandleDTO> loadCsvTradingView(String symbolName, String timeframe, Boolean volume);

    List<Double> getPriceReturns(String symbol, String timeframe, int period);

    List<Candle> getFilteredCandles(CandleFilterDTO filter);

    List<CandleDTO> loadCsvCME(String symbol, String timeframe, String data);

    void saveCandlesToDatabase(List<CandleDTO> candles, Symbol symbol, String timeframe);

    void saveCandlesToDatabase(List<CandleDTO> candles, String symbol, String timeframe);

    List<PointOfInterest> getInstitutionalLevels(String symbol, String timeframe);

}
