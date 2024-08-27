package finance.project.api.controllers;

import finance.project.api.model.SymbolDTO;
import finance.project.api.services.SymbolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance/symbols")
public class SymbolController {

    private final SymbolService symbolService;

    /*
    @GetMapping
    public ResponseEntity<List<SymbolDTO>> listAllSymbolsResp() {
        return new ResponseEntity<>(symbolService.listAllSymbols(), HttpStatus.OK);
    }*/

    @GetMapping
    public List<SymbolDTO> listAllSymbols() {
        return symbolService.listAllSymbols();
    }

    @GetMapping("/{symbolId}")
    public SymbolDTO getSymbolById(@PathVariable("symbolId") UUID id) {
        return symbolService.getSymbolById(id).orElseThrow(NotFoundException::new);
    }

    @PostMapping
    public ResponseEntity<SymbolDTO> createSymbol(@Validated @RequestBody SymbolDTO symbolDTO) {
        SymbolDTO createdSymbol = symbolService.createSymbol(symbolDTO);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/finance/symbols" + "/" + createdSymbol.getId().toString());

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/{symbolId}")
    public ResponseEntity<SymbolDTO> updateSymbol(@PathVariable("symbolId") UUID id, @RequestBody SymbolDTO symbolDTO) {
        return new ResponseEntity<>(symbolService.updateSymbol(id, symbolDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{symbolId}")
    public ResponseEntity<Void> deleteSymbol(@PathVariable("symbolId") UUID id) {
        if(!symbolService.deleteSymbol(id)) {
            throw new NotFoundException();
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}