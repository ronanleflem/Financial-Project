package finance.project.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.Symbology;
import finance.project.api.repositories.SymbologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymbologyService {

    private final SymbologyRepository symbologyRepository;

    public void loadSymbologyFromJson(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(new File(filePath));
            JsonNode result = root.get("result");

            List<Symbology> symbologyList = new ArrayList<>();

            Iterator<String> fieldNames = result.fieldNames();

            while (fieldNames.hasNext()) {
                String symbol = fieldNames.next();
                JsonNode entries = result.get(symbol);

                for (JsonNode entry : entries) {
                    LocalDate startDate = LocalDate.parse(entry.get("d0").asText());
                    LocalDate endDate = LocalDate.parse(entry.get("d1").asText());
                    Long score = entry.has("s") ? entry.get("s").asLong() : null;

                    Symbology symbology = Symbology.builder()
                            .symbol(symbol)
                            .startDate(startDate)
                            .endDate(endDate)
                            .score(score)
                            .build();

                    symbologyList.add(symbology);
                }
            }

            symbologyRepository.saveAll(symbologyList);
            log.info("✅ Symbology loaded successfully: {} records", symbologyList.size());

        } catch (Exception e) {
            log.error("❌ Error loading symbology JSON: {}", e.getMessage(), e);
        }
    }

    public List<Symbology> findBySymbol(String symbol) {
        return symbologyRepository.findBySymbol(symbol);
    }

    public List<Symbology> findAll() {
        return symbologyRepository.findAll();
    }
}
