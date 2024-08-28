package finance.project.api.controllers;

import finance.project.api.model.SymbolDTO;
import finance.project.api.services.SymbolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour gérer les symboles financiers.
 * <p>
 * Cette classe expose des points de terminaison API pour la gestion des symboles financiers. Elle fournit des opérations de
 * création, lecture, mise à jour et suppression (CRUD) pour les symboles, ainsi que la récupération d'une liste de tous les symboles.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance/symbols")
@Validated
public class SymbolController {

    private final SymbolService symbolService;

    /**
     * Récupère la liste de tous les symboles financiers.
     * <p>
     * Ce point de terminaison HTTP GET retourne une liste de tous les symboles financiers présents dans le système.
     * </p>
     *
     * @return une liste de {@link SymbolDTO} représentant tous les symboles financiers
     */
    @GetMapping
    public List<SymbolDTO> listAllSymbols() {
        return symbolService.listAllSymbols();
    }

    /**
     * Récupère un symbole financier par son identifiant.
     * <p>
     * Ce point de terminaison HTTP GET retourne le symbole financier correspondant à l'identifiant spécifié.
     * Si le symbole n'est pas trouvé, une exception {@link NotFoundException} est lancée.
     * </p>
     *
     * @param id l'identifiant du symbole financier à récupérer
     * @return un {@link SymbolDTO} représentant le symbole financier
     * @throws NotFoundException si le symbole avec l'identifiant spécifié n'est pas trouvé
     */
    @GetMapping("/{symbolId}")
    public SymbolDTO getSymbolById(@PathVariable("symbolId") UUID id) {
        return symbolService.getSymbolById(id)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Crée un nouveau symbole financier.
     * <p>
     * Ce point de terminaison HTTP POST permet de créer un nouveau symbole financier. L'objet {@link SymbolDTO} passé dans le
     * corps de la requête est validé avant la création. L'identifiant du symbole créé est ajouté dans l'en-tête "Location" de
     * la réponse. La réponse a un statut HTTP 201 (Created).
     * </p>
     *
     * @param symbolDTO les données du symbole financier à créer
     * @return une réponse HTTP contenant le {@link SymbolDTO} du symbole créé, avec un en-tête "Location"
     */
    @PostMapping
    public ResponseEntity<SymbolDTO> createSymbol(@Valid @RequestBody SymbolDTO symbolDTO) {
        SymbolDTO createdSymbol = symbolService.createSymbol(symbolDTO);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/finance/symbols/" + createdSymbol.getId().toString());

        return new ResponseEntity<>(createdSymbol, headers, HttpStatus.CREATED);
    }

    /**
     * Met à jour un symbole financier existant.
     * <p>
     * Ce point de terminaison HTTP PUT permet de mettre à jour les informations d'un symbole financier existant. Les données du
     * symbole, passées dans le corps de la requête, sont validées avant la mise à jour. La réponse a un statut HTTP 200 (OK).
     * </p>
     *
     * @param id l'identifiant du symbole financier à mettre à jour
     * @param symbolDTO les nouvelles données du symbole financier
     * @return une réponse HTTP contenant le {@link SymbolDTO} mis à jour
     * @throws NotFoundException si le symbole avec l'identifiant spécifié n'est pas trouvé
     */
    @PutMapping("/{symbolId}")
    public ResponseEntity<SymbolDTO> updateSymbol(@PathVariable("symbolId") UUID id, @Valid @RequestBody SymbolDTO symbolDTO) {
        return new ResponseEntity<>(symbolService.updateSymbol(id, symbolDTO), HttpStatus.OK);
    }

    /**
     * Supprime un symbole financier par son identifiant.
     * <p>
     * Ce point de terminaison HTTP DELETE supprime le symbole financier correspondant à l'identifiant spécifié. Si le symbole
     * n'existe pas, une exception {@link NotFoundException} est lancée. La réponse a un statut HTTP 204 (No Content) si la
     * suppression est réussie.
     * </p>
     *
     * @param id l'identifiant du symbole financier à supprimer
     * @return une réponse HTTP avec un statut HTTP 204 (No Content)
     * @throws NotFoundException si le symbole avec l'identifiant spécifié n'est pas trouvé
     */
    @DeleteMapping("/{symbolId}")
    public ResponseEntity<Void> deleteSymbol(@PathVariable("symbolId") UUID id) {
        if (!symbolService.deleteSymbol(id)) {
            throw new NotFoundException();
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
