package finance.project.api.live;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class LiveSseHubTest {

  @Test
  void shouldSendHandshakeAndTradeEvent() throws Exception {
    LiveProps props =
        new LiveProps(
            new LiveProps.Ingest(false, "X-Live-Hmac", "LIVE_HMAC_SECRET"),
            new LiveProps.Sse(0L));
    ObjectMapper mapper = new ObjectMapper();
    AtomicReference<TestEmitter> ref = new AtomicReference<>();
    LiveSseHub hub = new LiveSseHub(props, mapper, () -> {
      TestEmitter emitter = new TestEmitter();
      ref.set(emitter);
      return emitter;
    });

    try {
      SseEmitter emitter = hub.subscribe();
      assertThat(emitter).isInstanceOf(TestEmitter.class);

      LiveTradeAck ack = new LiveTradeAck();
      ack.setId(1L);
      ack.setStrategyId("strategy-x");
      ack.setSymbol("EURUSD");
      ack.setTimeframe("M5");
      ack.setTsOpenUtc(Instant.parse("2024-01-01T00:00:00Z"));
      ack.setSide(LiveTradeAck.Side.LONG);
      ack.setEntryPrice(1.234);
      ack.setSl(1.2);
      ack.setTp(1.3);
      ack.setExpectedRr(2.0);
      ack.setUniqHash("abc");
      ack.setSignalPayloadJson("{\"foo\":\"bar\"}");
      ack.setCreatedAt(Instant.parse("2024-01-01T00:00:01Z"));

      hub.broadcast(ack);

      TestEmitter testEmitter = ref.get();
      assertThat(testEmitter.events).isNotEmpty();
      assertThat(testEmitter.events.get(0)).isEqualTo("hello");
      assertThat(testEmitter.events).contains("trade");
      int tradeIndex = testEmitter.events.lastIndexOf("trade");
      Object tradeData = testEmitter.data.get(tradeIndex);
      assertThat(tradeData).isInstanceOf(Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> payload = (Map<String, Object>) tradeData;
      assertThat(payload.get("strategyId")).isEqualTo("strategy-x");
      assertThat(payload.get("payload")).isInstanceOf(Map.class);
    } finally {
      hub.shutdown();
    }
  }

  private static class TestEmitter extends SseEmitter {
    private final List<String> events = new ArrayList<>();
    private final List<Object> data = new ArrayList<>();

    TestEmitter() {
      super(0L);
    }

    @Override
    public synchronized void send(SseEventBuilder builder) throws IOException {
      Object eventName = readField(builder, "name");
      if (eventName == null) {
        eventName = readField(builder, "event");
      }
      events.add((String) eventName);
      Object payload = readField(builder, "data");
      if (payload == null) {
        payload = readField(builder, "dataToSend");
      }
      data.add(payload);
    }

    private Object readField(Object target, String fieldName) throws IOException {
      try {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
      } catch (NoSuchFieldException e) {
        return readFromSuperclass(target, fieldName);
      } catch (IllegalAccessException e) {
        throw new IOException("Cannot access field " + fieldName, e);
      }
    }

    private Object readFromSuperclass(Object target, String fieldName) throws IOException {
      Class<?> type = target.getClass().getSuperclass();
      while (type != null) {
        try {
          Field field = type.getDeclaredField(fieldName);
          field.setAccessible(true);
          return field.get(target);
        } catch (NoSuchFieldException ignored) {
          type = type.getSuperclass();
        } catch (IllegalAccessException e) {
          throw new IOException("Cannot access field " + fieldName, e);
        }
      }
      return null;
    }
  }
}
