package com.yourapp.live;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.lang.Nullable;

@Service
public class LiveSignalService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final LiveTradeAckRepository repo;
  private final LiveSseHub sseHub;
  private final LiveProps props;
  private final ObjectMapper objectMapper;

  public LiveSignalService(
      LiveTradeAckRepository repo, LiveSseHub sseHub, LiveProps props, ObjectMapper objectMapper) {
    this.repo = repo;
    this.sseHub = sseHub;
    this.props = props;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public LiveAckResponse ingest(LiveSignalDTO dto, @Nullable String hmacHeader) {
    if (props.ingest() != null && props.ingest().hmacEnabled()) {
      verifyHmac(dto, hmacHeader);
    }

    String uniq = computeUniqHash(dto);
    return repo
        .findByUniqHash(uniq)
        .map(
            existing -> {
              sseHub.broadcast(existing);
              return new LiveAckResponse(true, "duplicate_ignored");
            })
        .orElseGet(
            () -> {
              LiveTradeAck entity = map(dto, uniq);
              repo.save(entity);
              sseHub.broadcast(entity);
              return new LiveAckResponse(true, "accepted");
            });
  }

  public List<LiveTradeAck> recent(Duration lookback) {
    if (lookback.isNegative()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lookback_negative");
    }
    Instant since = Instant.now().minus(lookback);
    return repo.findRecent(since);
  }

  private void verifyHmac(LiveSignalDTO dto, @Nullable String header) {
    if (!StringUtils.hasText(header)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_hmac_header");
    }
    String secretEnv = props.ingest().hmacSecretEnv();
    if (!StringUtils.hasText(secretEnv)) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "missing_hmac_secret_env");
    }
    String secret = System.getenv(secretEnv);
    if (!StringUtils.hasText(secret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "hmac_secret_not_available");
    }

    String canonicalJson = canonicalJson(dto);
    String expected;
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] digest = mac.doFinal(canonicalJson.getBytes(StandardCharsets.UTF_8));
      expected = toHex(digest);
    } catch (GeneralSecurityException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "hmac_failure", e);
    }

    String provided = header.trim().toLowerCase();
    if (!MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_hmac");
    }
  }

  private LiveTradeAck map(LiveSignalDTO dto, String uniq) {
    LiveTradeAck entity = new LiveTradeAck();
    entity.setStrategyId(dto.strategyId());
    entity.setSymbol(dto.symbol());
    entity.setTimeframe(dto.timeframe());
    entity.setTsOpenUtc(dto.tsOpenUtc());
    entity.setSide(parseSide(dto.side()));
    entity.setEntryPrice(dto.entryPrice());
    entity.setSl(dto.sl());
    entity.setTp(dto.tp());
    entity.setExpectedRr(dto.expectedRr());
    entity.setUniqHash(uniq);
    entity.setSignalPayloadJson(serializePayload(dto.payload()));
    entity.setCreatedAt(Instant.now());
    return entity;
  }

  private LiveTradeAck.Side parseSide(String side) {
    try {
      return LiveTradeAck.Side.valueOf(side.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_side");
    }
  }

  private String serializePayload(Map<String, Object> payload) {
    if (payload == null || payload.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload_serialization_error", e);
    }
  }

  private String computeUniqHash(LiveSignalDTO dto) {
    String raw = String.join(
        "|",
        dto.strategyId(),
        dto.symbol(),
        dto.timeframe(),
        dto.tsOpenUtc().toString(),
        dto.side().toUpperCase(),
        String.valueOf(dto.entryPrice()),
        dto.sl() != null ? dto.sl().toString() : "null",
        dto.tp() != null ? dto.tp().toString() : "null");
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return toHex(hash);
    } catch (GeneralSecurityException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "hash_failure", e);
    }
  }

  private String canonicalJson(LiveSignalDTO dto) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("strategyId", dto.strategyId());
    root.put("symbol", dto.symbol());
    root.put("timeframe", dto.timeframe());
    root.put("tsOpenUtc", dto.tsOpenUtc().toString());
    root.put("side", dto.side().toUpperCase());
    root.put("entryPrice", dto.entryPrice());
    root.put("sl", dto.sl());
    root.put("tp", dto.tp());
    root.put("expectedRr", dto.expectedRr());
    root.put("payload", dto.payload() == null ? null : normalizeValue(dto.payload()));

    ObjectMapper canonicalMapper = objectMapper.copy();
    canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    try {
      return canonicalMapper.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "canonical_json_error", e);
    }
  }

  private Object normalizeValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> sorted = new TreeMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        sorted.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
      }
      return sorted;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> normalized = new ArrayList<>();
      for (Object element : iterable) {
        normalized.add(normalizeValue(element));
      }
      return normalized;
    }
    if (value != null && value.getClass().isArray()) {
      int length = java.lang.reflect.Array.getLength(value);
      List<Object> normalized = new ArrayList<>(length);
      for (int i = 0; i < length; i++) {
        normalized.add(normalizeValue(java.lang.reflect.Array.get(value, i)));
      }
      return normalized;
    }
    return value;
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
