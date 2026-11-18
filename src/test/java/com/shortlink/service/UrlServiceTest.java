package com.shortlink.service;

import com.shortlink.dto.ShortenRequest;
import com.shortlink.dto.ShortenResponse;
import com.shortlink.dto.UpdateUrlRequest;
import com.shortlink.entity.ClickLog;
import com.shortlink.entity.Url;
import com.shortlink.exception.AliasAlreadyExistsException;
import com.shortlink.exception.LinkExpiredException;
import com.shortlink.exception.UrlNotFoundException;
import com.shortlink.repository.ClickLogRepository;
import com.shortlink.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ClickLogRepository clickLogRepository;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, clickLogRepository, BASE_URL);
    }

    // ---------- shorten ----------

    @Test
    void shorten_withCustomAlias_savesAndReturnsShortUrl() {
        ShortenRequest request = new ShortenRequest("https://www.github.com", "mylink", 7);
        when(urlRepository.existsByShortCode("mylink")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse response = urlService.shorten(request);

        assertEquals("mylink", response.shortCode());
        assertEquals("http://localhost:8080/mylink", response.shortUrl());
        assertEquals("https://www.github.com", response.originalUrl());
        assertNotNull(response.expiresAt());
    }

    @Test
    void shorten_withExistingAlias_throwsAndDoesNotSave() {
        ShortenRequest request = new ShortenRequest("https://www.github.com", "taken", null);
        when(urlRepository.existsByShortCode("taken")).thenReturn(true);

        assertThrows(AliasAlreadyExistsException.class, () -> urlService.shorten(request));

        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void shorten_withoutAlias_generatesSevenCharCodeWithNoExpiry() {
        ShortenRequest request = new ShortenRequest("https://www.youtube.com", null, null);
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse response = urlService.shorten(request);

        assertTrue(response.shortCode().matches("[A-Za-z0-9]{7}"));
        assertNull(response.expiresAt());
    }

    // ---------- resolveAndTrack ----------

    @Test
    void resolveAndTrack_validLink_countsClickAndReturnsOriginalUrl() {
        Url url = new Url("https://www.github.com", "abc123", null);
        ReflectionTestUtils.setField(url, "id", 1L);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        String target = urlService.resolveAndTrack("abc123", "Chrome", "https://google.com");

        assertEquals("https://www.github.com", target);
        verify(urlRepository).incrementClickCount(1L);
        verify(clickLogRepository).save(any(ClickLog.class));
    }

    @Test
    void resolveAndTrack_expiredLink_throwsAndDoesNotCountClick() {
        Url url = new Url("https://www.github.com", "old123", LocalDateTime.now().minusDays(1));
        when(urlRepository.findByShortCode("old123")).thenReturn(Optional.of(url));

        assertThrows(LinkExpiredException.class,
                () -> urlService.resolveAndTrack("old123", "Chrome", null));

        verify(urlRepository, never()).incrementClickCount(any());
        verifyNoInteractions(clickLogRepository);
    }

    @Test
    void resolveAndTrack_unknownCode_throwsNotFound() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class,
                () -> urlService.resolveAndTrack("nope", "Chrome", null));
    }

    // ---------- update ----------

    @Test
    void update_onlyOriginalUrl_keepsExpiryUnchanged() {
        Url url = new Url("https://old.com", "abc123", null);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        ShortenResponse response =
                urlService.update("abc123", new UpdateUrlRequest("https://new.com", null));

        assertEquals("https://new.com", response.originalUrl());
        assertNull(response.expiresAt());
    }

    @Test
    void update_withExpiryDays_setsNewExpiry() {
        Url url = new Url("https://old.com", "abc123", null);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        ShortenResponse response = urlService.update("abc123", new UpdateUrlRequest(null, 10));

        assertEquals("https://old.com", response.originalUrl());
        assertNotNull(response.expiresAt());
    }

    // ---------- delete ----------

    @Test
    void delete_removesClickLogsBeforeTheUrl() {
        Url url = new Url("https://www.github.com", "abc123", null);
        ReflectionTestUtils.setField(url, "id", 5L);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        urlService.delete("abc123");

        InOrder inOrder = inOrder(clickLogRepository, urlRepository);
        inOrder.verify(clickLogRepository).deleteByUrlId(5L);
        inOrder.verify(urlRepository).delete(url);
    }

    @Test
    void delete_unknownCode_throwsNotFound() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.delete("nope"));

        verify(urlRepository, never()).delete(any(Url.class));
    }
}