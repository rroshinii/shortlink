package com.shortlink.controller;

import com.shortlink.dto.PageResponse;
import com.shortlink.dto.ShortenRequest;
import com.shortlink.dto.ShortenResponse;
import com.shortlink.dto.StatsResponse;
import com.shortlink.dto.UpdateUrlRequest;
import com.shortlink.dto.UrlSummaryResponse;
import com.shortlink.service.QrCodeService;
import com.shortlink.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final QrCodeService qrCodeService;

    // Constructor injection of two dependencies
    public UrlController(UrlService urlService, QrCodeService qrCodeService) {
        this.urlService = urlService;
        this.qrCodeService = qrCodeService;
    }

    // CREATE: POST /api/shorten
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.shorten(request));
    }

    // LIST: GET /api/urls?page=0&size=10&sort=createdAt,desc&search=git
    @GetMapping("/api/urls")
    public PageResponse<UrlSummaryResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return urlService.list(search, pageable);
    }

    // STATS: GET /api/urls/{code}/stats
    @GetMapping("/api/urls/{code}/stats")
    public StatsResponse stats(@PathVariable String code) {
        return urlService.getStats(code);
    }

    // QR CODE: GET /api/urls/{code}/qr?size=300
    @GetMapping("/api/urls/{code}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable String code,
                                     @RequestParam(defaultValue = "300") int size) {
        int safeSize = Math.max(100, Math.min(size, 1000));   // keep between 100 and 1000 px
        String shortUrl = urlService.getShortUrl(code);
        byte[] png = qrCodeService.generatePng(shortUrl, safeSize);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache())
                .body(png);
    }

    // UPDATE: PUT /api/urls/{code}
    @PutMapping("/api/urls/{code}")
    public ShortenResponse update(@PathVariable String code,
                                  @Valid @RequestBody UpdateUrlRequest request) {
        return urlService.update(code, request);
    }

    // DELETE: DELETE /api/urls/{code}
    @DeleteMapping("/api/urls/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        urlService.delete(code);
        return ResponseEntity.noContent().build();   // 204
    }

    // REDIRECT: GET /{code}
    @GetMapping("/{code:[a-zA-Z0-9_-]{3,30}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String target = urlService.resolveAndTrack(
                code, request.getHeader("User-Agent"), request.getHeader("Referer"));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}