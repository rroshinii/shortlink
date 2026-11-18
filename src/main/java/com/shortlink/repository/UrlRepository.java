package com.shortlink.repository;

import com.shortlink.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<Url> findByOriginalUrlContainingIgnoreCaseOrShortCodeContainingIgnoreCase(
            String originalUrl, String shortCode, Pageable pageable);

    // Atomic increment in the database, safe even when many people click at once
    @Modifying
    @Query("update Url u set u.clickCount = u.clickCount + 1 where u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    // Deletes links that expired before the cutoff
    @Modifying
    @Query("delete from Url u where u.expiresAt is not null and u.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}