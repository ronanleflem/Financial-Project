package finance.project.api.scoring;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeScoringService {

    private static final Logger logger = LoggerFactory.getLogger(TradeScoringService.class);

    private final List<ScoreRule> scoreRules;

    @Autowired
    public TradeScoringService(List<ScoreRule> scoreRules) {
        this.scoreRules = scoreRules;
    }

    public int calculateTradeScore(TradeSignalDTO signal, MarketData marketData) {
        int totalScore = 0;

        //logger.info("🔍 Calcul du score pour le trade {} à {}", signal.getDirection(), signal.getTimestamp());

        for (ScoreRule rule : scoreRules) {
            int ruleScore = rule.calculateScore(signal, marketData);
            totalScore += ruleScore;

            logger.info("📌 [{}] Score: {} ({})", rule.getClass().getSimpleName(), ruleScore, totalScore);
        }

        logger.info("✅ Score final du trade : {}", totalScore);
        return totalScore;
    }

    public boolean isTradeValid(TradeSignalDTO signal, MarketData marketData) {
        int score = calculateTradeScore(signal, marketData);

        if (score >= 50) {
            logger.info("✔️ Trade validé avec un score de {} ✅", score);
            return true;
        } else {
            logger.warn("❌ Trade rejeté avec un score de {} ❌", score);
            return false;
        }
    }
}