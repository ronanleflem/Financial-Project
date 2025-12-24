package finance.project.api.dataimport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.DataImportService;
import finance.project.api.dataimport.dto.DataImportRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataImportJobController.class)
class DataImportJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataImportService dataImportService;

    @Test
    void createJobReturnsAcceptedResponse() throws Exception {
        DataImportRequest request = new DataImportRequest(
                "IBKR",
                "AAPL",
                "1d",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-10T00:00:00Z"),
                "HISTORICAL",
                "NYSE",
                "UTC",
                "IGNORE",
                "NONE",
                "EQUITY"
        );

        DataImportJob job = buildJob("job-1");

        given(dataImportService.createJob(request)).willReturn(job);

        mockMvc.perform(post("/api/data-import/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("job-1")))
                .andExpect(jsonPath("$.broker", is("IBKR")))
                .andExpect(jsonPath("$.status", is("PENDING")));
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
                null
        );

        mockMvc.perform(post("/api/data-import/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobReturnsResponse() throws Exception {
        DataImportJob job = buildJob("job-2");
        given(dataImportService.getJob("job-2")).willReturn(Optional.of(job));

        mockMvc.perform(get("/api/data-import/jobs/job-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("job-2")))
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void getJobReturnsNotFound() throws Exception {
        given(dataImportService.getJob("missing")).willReturn(Optional.empty());

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
        job.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        job.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        return job;
    }
}
