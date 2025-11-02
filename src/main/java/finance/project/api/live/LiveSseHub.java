package finance.project.api.live;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class LiveSseHub {

  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<>() {};

  private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();
  private final LiveProps props;
  private final ObjectMapper objectMapper;
  private final Supplier<SseEmitter> emitterSupplier;
  private final ScheduledExecutorService scheduler;

  @Autowired
  public LiveSseHub(LiveProps props, ObjectMapper objectMapper) {
    this(props, objectMapper, () -> new SseEmitter(0L));
  }

  LiveSseHub(
      LiveProps props, ObjectMapper objectMapper, Supplier<SseEmitter> emitterSupplier) {
    this.props = props;
    this.objectMapper = objectMapper;
    this.emitterSupplier = emitterSupplier;
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
    long heartbeat = props.sse() != null ? props.sse().heartbeatMillis() : 15000L;
    if (heartbeat > 0) {
      scheduler.scheduleAtFixedRate(
          this::heartbeat, heartbeat, heartbeat, TimeUnit.MILLISECONDS);
    }
  }

  public SseEmitter subscribe() {
    SseEmitter emitter = emitterSupplier.get();
    clients.add(emitter);
    emitter.onCompletion(() -> clients.remove(emitter));
    emitter.onTimeout(() -> clients.remove(emitter));
    send(emitter, "hello", Map.of("ts", Instant.now().toString()));
    return emitter;
  }

  public void broadcast(LiveTradeAck ack) {
    clients.forEach(em -> send(em, "trade", ackToMap(ack)));
  }

  private void heartbeat() {
    clients.forEach(em -> send(em, "heartbeat", Map.of("ts", Instant.now().toString())));
  }

  private void send(SseEmitter emitter, String event, Object data) {
    try {
      emitter.send(SseEmitter.event().name(event).data(data));
    } catch (IOException ex) {
      clients.remove(emitter);
    }
  }

  private Map<String, Object> ackToMap(LiveTradeAck ack) {
    Map<String, Object> payload = parsePayload(ack.getSignalPayloadJson());
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", ack.getId());
    map.put("strategyId", ack.getStrategyId());
    map.put("symbol", ack.getSymbol());
    map.put("timeframe", ack.getTimeframe());
    map.put("tsOpenUtc", ack.getTsOpenUtc());
    map.put("side", ack.getSide() != null ? ack.getSide().name() : null);
    map.put("entryPrice", ack.getEntryPrice());
    map.put("sl", ack.getSl());
    map.put("tp", ack.getTp());
    map.put("expectedRr", ack.getExpectedRr());
    map.put("payload", payload);
    map.put("uniqHash", ack.getUniqHash());
    map.put("createdAt", ack.getCreatedAt());
    return map;
  }

  private Map<String, Object> parsePayload(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, MAP_TYPE);
    } catch (IOException e) {
      return Map.of("raw", json);
    }
  }

  @PreDestroy
  public void shutdown() {
    scheduler.shutdownNow();
    clients.forEach(SseEmitter::complete);
    clients.clear();
  }
}
