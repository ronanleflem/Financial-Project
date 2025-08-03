package finance.project.api.services;

import finance.project.api.entities.FilterSignal;
import finance.project.api.repositories.FilterSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignalRecorderService {

    private final FilterSignalRepository repository;

    public void recordSignal(String filterName,
                             String symbol,
                             String timeframe,
                             LocalDateTime signalTime,
                             double baseClose,
                             int horizon,
                             double futureClose) {

        double variation = (futureClose - baseClose) / baseClose;
        boolean success = variation > 0;

        repository.save(
                FilterSignal.builder()
                        .filterName(filterName)
                        .symbol(symbol)
                        .timeframe(timeframe)
                        .signalTime(signalTime)
                        .baseClose(baseClose)
                        .horizon(horizon)
                        .futureClose(futureClose)
                        .variationPct(variation)
                        .success(success)
                        .hourOfDay(signalTime.getHour())
                        .dayOfWeek(signalTime.getDayOfWeek().getValue())
                        .build()
        );
    }
}