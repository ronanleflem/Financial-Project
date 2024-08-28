package finance.project.api.controllers;


import finance.project.api.model.CandleDTO;
import finance.project.api.services.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le contrôleur {@code CandleController} est un contrôleur REST qui gère les requêtes HTTP liées aux bougies (candles) financières.
 * Il expose des endpoints pour récupérer des données de bougies en fonction des symboles et des intervalles spécifiés.
 * <p>
 * Cette classe est annotée avec {@link RestController}, ce qui la rend apte à gérer les requêtes HTTP et à renvoyer des réponses JSON.
 * L'annotation {@link RequestMapping} spécifie que toutes les requêtes à ce contrôleur seront préfixées par "/api/finance/charts".
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance/charts")
public class CandleController {

    /**
     * Service pour la gestion des bougies.
     */
    private final CandleService candleService;

    /**
     * Récupère une liste de bougies (candles) en fonction du symbole et de l'intervalle fournis.
     * <p>
     * Cette méthode répond aux requêtes GET à l'URL "/api/finance/charts" avec les paramètres de requête "symbol" et "interval".
     * Elle utilise le service {@link CandleService} pour obtenir les données et renvoie une réponse HTTP avec le statut 200 OK et les données.
     * </p>
     *
     * @param symbol le symbole de négociation pour lequel les bougies doivent être récupérées (ex: "AAPL")
     * @param interval l'intervalle de temps pour les bougies (ex: "daily", "hourly")
     * @return une réponse HTTP contenant la liste des bougies correspondant au symbole et à l'intervalle spécifiés
     */
    @GetMapping
    public ResponseEntity<List<CandleDTO>> getCandles(@RequestParam String symbol, @RequestParam String interval) {
        List<CandleDTO> data = candleService.getCandles(symbol, interval);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    /**
     * Récupère une liste par défaut de bougies pour le symbole "AAPL" avec l'intervalle "daily".
     * <p>
     * Cette méthode répond aux requêtes GET à l'URL "/api/finance/charts/default".
     * Elle utilise le service {@link CandleService} pour obtenir les données par défaut et renvoie directement les bougies sans spécifier de symbole ni d'intervalle dans la requête.
     * </p>
     *
     * @return la liste des bougies pour le symbole "AAPL" avec l'intervalle "daily"
     */
    @GetMapping("/default")
    public List<CandleDTO> listCandles(){
        return candleService.getCandles("AAPL", "daily");
    }
}
