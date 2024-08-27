package finance.project.api.controllers;

import finance.project.api.model.CandleDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CandleControllerIT {

    @Autowired
    CandleController candleController;

    @Test
    void testGetChartData() {
        List<CandleDTO> dtos = candleController.listCandles();

        assertThat(dtos.size()).isEqualTo(1);
    }
}
