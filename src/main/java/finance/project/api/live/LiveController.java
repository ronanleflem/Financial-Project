package finance.project.api.live;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/live")
public class LiveController {

  private final LiveSignalService service;
  private final LiveSseHub hub;

  public LiveController(LiveSignalService service, LiveSseHub hub) {
    this.service = service;
    this.hub = hub;
  }

  @Operation(summary = "Ingest live trade signal")
  @PostMapping("/signal")
  public ResponseEntity<LiveAckResponse> signal(
      @RequestBody @Valid LiveSignalDTO dto,
      @RequestHeader(name = "#{@liveProps.ingest().hmacHeader}", required = false) String hmac) {
    LiveAckResponse response = service.ingest(dto, hmac);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "SSE stream of live trades")
  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream() {
    return hub.subscribe();
  }

  @GetMapping("/recent")
  public List<LiveTradeAck> recent(@RequestParam(defaultValue = "PT6H") String lookback) {
    try {
      return service.recent(Duration.parse(lookback));
    } catch (DateTimeParseException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_lookback", ex);
    }
  }
}
