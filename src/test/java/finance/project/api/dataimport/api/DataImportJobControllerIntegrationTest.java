package finance.project.api.dataimport.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.DataImportJobRepository;
import finance.project.api.dataimport.DataImportJobRunner;
import finance.project.api.dataimport.dto.DataImportRequest;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataImportJobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataImportJobRepository dataImportJobRepository;

    @MockBean
    private DataImportJobRunner dataImportJobRunner;

    @AfterEach
    void tearDown() {
        dataImportJobRepository.deleteAll();
    }

    @Test
    void createJobPersistsAndReturnsAcceptedResponse() throws Exception {
        DataImportRequest request = new DataImportRequest(
                "IBKR",
                "AAPL",
                "1d",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-10T00:00:00Z"),
                "HISTORICAL",
                "NYSE",
                "USD",
                "UTC",
                "IGNORE",
                "NONE",
                "EQUITY"
        );

        MvcResult result = mockMvc.perform(post("/api/data-import/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.broker").value("IBKR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        String jobId = payload.get("id").asText();

        assertThat(dataImportJobRepository.findById(jobId)).isPresent();
        verify(dataImportJobRunner).runJobAsync(jobId);
    }

    @Test
    void createJobRejectsInvalidDateRange() throws Exception {
        DataImportRequest request = new DataImportRequest(
                "IBKR",
                "AAPL",
                "1d",
                Instant.parse("2024-02-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"),
                "HISTORICAL",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/data-import/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dataImportJobRunner);
    }

    @Test
    void getJobReturnsResponseForExistingJob() throws Exception {
        DataImportJob job = buildJob("job-123");
        dataImportJobRepository.save(job);

        mockMvc.perform(get("/api/data-import/jobs/job-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("job-123"))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getJobReturnsNotFoundForMissingJob() throws Exception {
        mockMvc.perform(get("/api/data-import/jobs/missing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private DataImportJob buildJob(String id) {
        DataImportJob job = new DataImportJob();
        job.setId(id);
        job.setBroker("IBKR");
        job.setSymbol("AAPL");
        job.setTimeframe("1d");
        job.setStartDate(Instant.parse("2024-01-01T00:00:00Z"));
        job.setEndDate(Instant.parse("2024-01-10T00:00:00Z"));
        job.setSourceType("HISTORICAL");
        job.setStatus(DataImportJob.Status.PENDING);
        job.setProgress(0);
        return job;
    }
}
