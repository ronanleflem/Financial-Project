package finance.project.api.services;

import finance.project.api.model.fx.FxQuote;
import finance.project.api.model.fx.HistBar;

import java.util.List;

/**
 * Common contract for FX market data providers so that controllers can share the same logic
 * independently of the underlying broker or exchange implementation.
 */
public interface FxMarketDataService {

    /**
     * Identifier used for log messages or responses.
     */
    String getProvider();

    /**
     * Attempt to establish a connection to the underlying provider. Implementations that rely on
     * HTTP polling can treat this as a logical toggle.
     */
    void connect(String host, int port, int clientId);

    /**
     * Connect and optionally wait for a handshake/ready signal.
     *
     * @return {@code true} if the provider is ready for use
     */
    boolean connectAndWait(String host, int port, int clientId, long timeoutMs);

    /**
     * Disconnect and release any associated resources.
     */
    void disconnect();

    /**
     * Indicates if the provider is currently connected/ready.
     */
    boolean isConnected();

    /**
     * Whether the provider supports switching market data type (live/delayed, etc.).
     */
    default boolean supportsMarketDataType() {
        return false;
    }

    /**
     * Update the market data type when supported.
     */
    default void setMarketDataType(int type) {
        throw new UnsupportedOperationException("Market data type not supported for provider " + getProvider());
    }

    int startLiveEurUsd();

    void stopLive(int liveReqId);

    List<FxQuote> getRecentLiveQuotes();

    int startLiveMinuteBarsEurUsd();

    int startLiveBars(String pair, String duration, String barSize, String whatToShow, int useRth, int formatDate);

    List<HistBar> getRecentLiveBars(int reqId);

    HistBar getLastLiveBar(int reqId);

    void stopLiveBars(int reqId);

    List<HistBar> getHistoricalEurUsd(String duration, String barSize) throws Exception;
}
