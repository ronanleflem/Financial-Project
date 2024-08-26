package finance.project.api.bootstrap;

import finance.project.api.entities.FinancialData;
import finance.project.api.model.ChartDataDTO;
import finance.project.api.repositories.ChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final ChartRepository chartRepository;

    @Override
    public void run(String... args) throws Exception {
        loadChartData();
    }

    private void loadChartData() {
        if(chartRepository.count() == 0) {

            FinancialData chart1 = FinancialData.builder()
                    .symbol("AAPL")
                    .date(LocalDate.now())
                    .open(new BigDecimal("100.00"))
                    .close(new BigDecimal("110.00"))
                    .high(new BigDecimal("115.00"))
                    .low(new BigDecimal("95.00"))
                    .volume(new BigDecimal("1000000"))
                    .build();

            FinancialData chart2 = FinancialData.builder()
                    .symbol("AAPL")
                    .date(LocalDate.now())
                    .open(new BigDecimal("110.00"))
                    .close(new BigDecimal("120.00"))
                    .high(new BigDecimal("125.00"))
                    .low(new BigDecimal("105.00"))
                    .volume(new BigDecimal("1100000"))
                    .build();

            chartRepository.saveAll(Arrays.asList(chart1,chart2));
        }
    }

}
