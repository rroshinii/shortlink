package com.shortlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_logs")
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id")
    private Url url;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 512)
    private String referrer;

    public ClickLog() {}

    public ClickLog(Url url, String userAgent, String referrer) {
        this.url = url;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.clickedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Url getUrl() { return url; }
    public LocalDateTime getClickedAt() { return clickedAt; }
    public String getUserAgent() { return userAgent; }
    public String getReferrer() { return referrer; }
}
