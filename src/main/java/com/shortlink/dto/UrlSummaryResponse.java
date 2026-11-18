package com.shortlink.dto;

import java.time.LocalDateTime;

public record UrlSummaryResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        long clickCount,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean expired
) {}