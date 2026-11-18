package com.shortlink.dto;

import java.time.LocalDateTime;

public record StatsResponse(
        String shortCode,
        String originalUrl,
        long totalClicks,
        long clicksLast24Hours,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime lastClickedAt,
        boolean expired
) {}
