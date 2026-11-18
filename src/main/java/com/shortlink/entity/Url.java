package com.shortlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 30)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt; // null = never expires

    @Column(nullable = false)
    private long clickCount = 0;

    public Url() {}

    public Url(String originalUrl, String shortCode, LocalDateTime expiresAt) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public Long getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getShortCode() { return shortCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public long getClickCount() { return clickCount; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
