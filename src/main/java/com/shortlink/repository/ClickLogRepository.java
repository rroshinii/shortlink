package com.shortlink.repository;

import com.shortlink.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

    long countByUrlIdAndClickedAtAfter(Long urlId, LocalDateTime since);

    Optional<ClickLog> findFirstByUrlIdOrderByClickedAtDesc(Long urlId);

    void deleteByUrlId(Long urlId);

    // Deletes click logs of links that expired before the cutoff
    @Modifying
    @Query("delete from ClickLog c where c.url.id in "
            + "(select u.id from Url u where u.expiresAt is not null and u.expiresAt < :cutoff)")
    int deleteLogsOfExpiredUrls(@Param("cutoff") LocalDateTime cutoff);
}