package com.example.shortenuri.repository;

import com.example.shortenuri.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);
    
    boolean existsByShortCode(String shortCode);
    
    @Query("SELECT u FROM Url u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < CURRENT_TIMESTAMP")
    List<Url> findExpiredUrls();
    
    @Query("SELECT u FROM Url u WHERE u.originalUrl = :originalUrl")
    Optional<Url> findByOriginalUrl(@Param("originalUrl") String originalUrl);
}
