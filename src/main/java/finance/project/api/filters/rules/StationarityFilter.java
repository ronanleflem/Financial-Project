package finance.project.api.filters.rules;

import java.util.List;

/**
 * Vérification de la stabilité du marché pour valider les modèles quantitatifs.
 */
public class StationarityFilter {

    public boolean isStationary(List<Double> priceSeries) {
        double[] prices = priceSeries.stream().mapToDouble(Double::doubleValue).toArray();
        ADFTest adfTest = new ADFTest(prices);
        double testStatistic = adfTest.getTestStatistic();
        double criticalValue = adfTest.getCriticalValue(0.05); // Niveau de signification de 5%
        return testStatistic < criticalValue;
    }
}
