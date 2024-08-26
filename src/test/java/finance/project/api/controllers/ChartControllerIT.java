package finance.project.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.entities.FinancialData;
import finance.project.api.mappers.ChartDataMapper;
import finance.project.api.model.ChartDataDTO;
import finance.project.api.repositories.ChartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ChartControllerIT {

    @Autowired
    ChartController chartController;

    @Autowired
    ChartRepository chartRepository;

    @Autowired
    ChartDataMapper chartDataMapper;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    }

    @Test
    void testGetChartData() {
        List<ChartDataDTO> dtos = chartController.listChartData();

        assertThat(dtos.size()).isEqualTo(2);
    }
}
