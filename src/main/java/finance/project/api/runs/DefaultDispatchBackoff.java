package finance.project.api.runs;

import org.springframework.stereotype.Component;

@Component
public class DefaultDispatchBackoff implements DispatchBackoff {
    private static final long[] BACKOFF_MILLIS = new long[]{1_000L, 5_000L, 15_000L};

    @Override
    public void backoff(int attempt) {
        long backoff = BACKOFF_MILLIS[Math.min(attempt - 1, BACKOFF_MILLIS.length - 1)];
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

