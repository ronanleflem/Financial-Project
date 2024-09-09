package finance.project.api.services;

import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;

import java.util.List;

public interface CandleService {

    List<CandleDTO> getCandles(SymbolDTO symbol, String interval);

}
