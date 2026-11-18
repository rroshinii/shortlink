package com.shortlink.service;

import com.shortlink.dto.*;
import com.shortlink.entity.ClickLog;
import com.shortlink.entity.Url;
import com.shortlink.exception.*;
import com.shortlink.repository.ClickLogRepository;
import com.shortlink.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UrlService {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    private final UrlRepository urlRepository;
    private final ClickLogRepository clickLogRepository;
    private final String baseUrl;

    // Constructor injection (no @Autowired needed with a single constructor)
    public UrlService(UrlRepository urlRepository,
                      ClickLogRepository clickLogRepository,
                      @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.clickLogRepository = clickLogRepository;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String code;
        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            code = request.customAlias();
            if (urlRepository.existsByShortCode(code)) {
                throw new AliasAlreadyExistsException(code);
            }
        } else {
            code = generateUniqueCode();
        }

        LocalDateTime expiresAt = request.expiryDays() == null
                ? null
                : LocalDateTime.now().plusDays(request.expiryDays());

        Url saved = urlRepository.save(new Url(request.originalUrl(), code, expiresAt));
        return toResponse(saved);
    }

    /** Resolves the code, records the click and returns the original URL. */
    @Transactional
    public String resolveAndTrack(String code, String userAgent, String referrer) {
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));

        if (url.isExpired()) {
            throw new LinkExpiredException(code);
        }

        urlRepository.incrementClickCount(url.getId());
        clickLogRepository.save(new ClickLog(url, trim(userAgent), trim(referrer)));
        return url.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String code) {
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));

        long last24h = clickLogRepository
                .countByUrlIdAndClickedAtAfter(url.getId(), LocalDateTime.now().minusHours(24));
        LocalDateTime lastClicked = clickLogRepository
                .findFirstByUrlIdOrderByClickedAtDesc(url.getId())
                .map(ClickLog::getClickedAt)
                .orElse(null);

        return new StatsResponse(url.getShortCode(), url.getOriginalUrl(), url.getClickCount(),
                last24h, url.getCreatedAt(), url.getExpiresAt(), lastClicked, url.isExpired());
    }

    @Transactional(readOnly = true)
    public PageResponse<UrlSummaryResponse> list(String search, Pageable pageable) {
        Page<Url> page = (search == null || search.isBlank())
                ? urlRepository.findAll(pageable)
                : urlRepository
                .findByOriginalUrlContainingIgnoreCaseOrShortCodeContainingIgnoreCase(
                        search, search, pageable);

        List<UrlSummaryResponse> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional
    public ShortenResponse update(String code, UpdateUrlRequest request) {
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));

        if (request.originalUrl() != null) {
            url.setOriginalUrl(request.originalUrl());
        }
        if (request.expiryDays() != null) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.expiryDays()));
        }
        // No save() needed: the entity is managed, so JPA writes the changes on commit
        return toResponse(url);
    }

    @Transactional
    public void delete(String code) {
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));
        clickLogRepository.deleteByUrlId(url.getId());
        urlRepository.delete(url);
    }

    @Transactional(readOnly = true)
    public String getShortUrl(String code) {
        Url url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));
        return baseUrl + "/" + url.getShortCode();
    }

    private ShortenResponse toResponse(Url url) {
        return new ShortenResponse(url.getShortCode(), baseUrl + "/" + url.getShortCode(),
                url.getOriginalUrl(), url.getExpiresAt());
    }

    private UrlSummaryResponse toSummary(Url url) {
        return new UrlSummaryResponse(url.getShortCode(), baseUrl + "/" + url.getShortCode(),
                url.getOriginalUrl(), url.getClickCount(), url.getCreatedAt(),
                url.getExpiresAt(), url.isExpired());
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    private String trim(String value) {
        return (value != null && value.length() > 500) ? value.substring(0, 500) : value;
    }
}