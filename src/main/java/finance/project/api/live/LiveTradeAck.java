package finance.project.api.live;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "trades_live_ack",
    uniqueConstraints = @UniqueConstraint(name = "ux_uniq_hash", columnNames = "uniqHash"))
public class LiveTradeAck {

  public enum Side {
    LONG,
    SHORT
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String strategyId;

  @Column(nullable = false)
  private String symbol;

  @Column(nullable = false)
  private String timeframe;

  @Column(nullable = false)
  private Instant tsOpenUtc;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Side side;

  @Column(nullable = false)
  private double entryPrice;

  private Double sl;

  private Double tp;

  private Double expectedRr;

  @Column(nullable = false, length = 64)
  private String uniqHash;

  @Column(columnDefinition = "text")
  private String signalPayloadJson;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public LiveTradeAck() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStrategyId() {
    return strategyId;
  }

  public void setStrategyId(String strategyId) {
    this.strategyId = strategyId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getTimeframe() {
    return timeframe;
  }

  public void setTimeframe(String timeframe) {
    this.timeframe = timeframe;
  }

  public Instant getTsOpenUtc() {
    return tsOpenUtc;
  }

  public void setTsOpenUtc(Instant tsOpenUtc) {
    this.tsOpenUtc = tsOpenUtc;
  }

  public Side getSide() {
    return side;
  }

  public void setSide(Side side) {
    this.side = side;
  }

  public double getEntryPrice() {
    return entryPrice;
  }

  public void setEntryPrice(double entryPrice) {
    this.entryPrice = entryPrice;
  }

  public Double getSl() {
    return sl;
  }

  public void setSl(Double sl) {
    this.sl = sl;
  }

  public Double getTp() {
    return tp;
  }

  public void setTp(Double tp) {
    this.tp = tp;
  }

  public Double getExpectedRr() {
    return expectedRr;
  }

  public void setExpectedRr(Double expectedRr) {
    this.expectedRr = expectedRr;
  }

  public String getUniqHash() {
    return uniqHash;
  }

  public void setUniqHash(String uniqHash) {
    this.uniqHash = uniqHash;
  }

  public String getSignalPayloadJson() {
    return signalPayloadJson;
  }

  public void setSignalPayloadJson(String signalPayloadJson) {
    this.signalPayloadJson = signalPayloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
