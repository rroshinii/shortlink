package com.shortlink.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

public record UpdateUrlRequest(
        @URL(message = "originalUrl must be a valid URL") String originalUrl,
        @Min(1) @Max(365) Integer expiryDays
) {}