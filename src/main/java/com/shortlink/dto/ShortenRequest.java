package com.shortlink.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

public record ShortenRequest(
        @NotBlank(message = "originalUrl is required")
        @URL(message = "originalUrl must be a valid URL")
        @Size(max = 2048)
        String originalUrl,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$",
                 message = "customAlias must be 3-20 characters: letters, digits, _ or -")
        String customAlias,

        @Min(value = 1, message = "expiryDays must be at least 1")
        @Max(value = 365, message = "expiryDays cannot exceed 365")
        Integer expiryDays
) {}
