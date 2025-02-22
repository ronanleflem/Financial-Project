package finance.project.api.filters.rules;

import finance.project.api.model.CandleDTO;
import finance.project.api.services.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 📌 Prochaine étape ?
 *
 * Ajout de données macroéconomiques (COT Report, Open Interest) pour affiner le filtre 📊🚀
 */
@Service
@RequiredArgsConstructor
public class BiaisInstitutionalFilter {

    private final MarketDataService marketDataService;
    /**
     * Calcule le biais institutionnel en fonction des indicateurs techniques
     *
     * @return Score de biais institutionnel (-1 = baissier, 0 = neutre, 1 = haussier)
     */
    public int calculateInstitutionalBias(List<CandleDTO> candlesLatest) {
        if (candlesLatest.size() < 200) {
            throw new IllegalArgumentException("Pas assez de données (min 200 bougies)");
        }

        double price = candlesLatest.get(candlesLatest.size() - 1).getClose().doubleValue(); // Dernier prix
        double ema50 = marketDataService.calculateEMA(candlesLatest.subList(candlesLatest.size() - 50, candlesLatest.size()), 50);
        double ema200 = marketDataService.calculateEMA(candlesLatest.subList(candlesLatest.size() - 200, candlesLatest.size()), 200);
        //double vwap = marketDataService.calculateVWAP(candlesLatest);

        int bias = 0;

        // Biais basé sur les EMA
        if (price > ema50 && ema50 > ema200) {
            bias += 1; // Biais haussier
        } else if (price < ema50 && ema50 < ema200) {
            bias -= 1; // Biais baissier
        }

        // Biais basé sur le VWAP
        /*
        if (price > vwap) {
            bias += 1; // Pression acheteuse
        } else {
            bias -= 1; // Pression vendeuse
        }*/

        return Integer.compare(bias, 0);
    }

}