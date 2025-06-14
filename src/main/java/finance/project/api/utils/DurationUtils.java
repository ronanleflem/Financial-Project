package finance.project.api.utils;

import java.time.Duration;

public class DurationUtils {
    public static Duration parseTimeframe(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) {
            return Duration.ofMinutes(1);
        }

        String tf = timeframe.toLowerCase();
        if (tf.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(tf.replace("m", "")));
        } else if (tf.endsWith("min")) {
            return Duration.ofMinutes(Long.parseLong(tf.replace("min", "")));
        } else if (tf.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(tf.replace("h", "")));
        } else if (tf.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(tf.replace("d", "")));
        } else if (tf.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(tf.replace("s", "")));
        }

        throw new IllegalArgumentException("Timeframe non reconnu : " + timeframe);
    }
}
