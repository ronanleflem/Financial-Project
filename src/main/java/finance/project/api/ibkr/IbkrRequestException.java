package finance.project.api.ibkr;

/**
 * Runtime exception thrown when an IBKR market data request fails.
 */
public class IbkrRequestException extends RuntimeException {

    public IbkrRequestException(String message) {
        super(message);
    }

    public IbkrRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
