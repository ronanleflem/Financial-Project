package com.yourapp.live;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record LiveSignalDTO(
    @NotBlank String strategyId,
    @NotBlank String symbol,
    @NotBlank String timeframe,
    @NotNull Instant tsOpenUtc,
    @NotBlank String side,
    @NotNull Double entryPrice,
    Double sl,
    Double tp,
    Double expectedRr,
    Map<String, Object> payload) {}
