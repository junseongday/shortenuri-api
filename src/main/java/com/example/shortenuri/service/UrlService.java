package com.example.shortenuri.service;

import com.example.shortenuri.dto.CreateUrlRequest;
import com.example.shortenuri.dto.CreateUrlResponse;
import com.example.shortenuri.dto.UrlStatsResponse;
import com.example.shortenuri.entity.Url;
import com.example.shortenuri.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UrlService {

    @Autowired
    private UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        // Check if URL already exists
        Optional<Url> existingUrl = urlRepository.findByOriginalUrl(request.getOriginalUrl());
        if (existingUrl.isPresent()) {
            Url url = existingUrl.get();
            return new CreateUrlResponse(
                url.getShortCode(),
                url.getOriginalUrl(),
                baseUrl + "/" + url.getShortCode(),
                url.getCreatedAt(),
                url.getExpiresAt()
            );
        }

        // Generate short code
        String shortCode = request.getCustomShortCode() != null && !request.getCustomShortCode().isEmpty() 
            ? request.getCustomShortCode() 
            : generateShortCode();

        // Check if custom short code already exists
        if (urlRepository.existsByShortCode(shortCode)) {
            throw new RuntimeException("Short code already exists: " + shortCode);
        }

        // Create new URL
        Url url = new Url(shortCode, request.getOriginalUrl());
        if (request.getExpiresAt() != null) {
            url.setExpiresAt(request.getExpiresAt());
        }

        url = urlRepository.save(url);

        return new CreateUrlResponse(
            url.getShortCode(),
            url.getOriginalUrl(),
            baseUrl + "/" + url.getShortCode(),
            url.getCreatedAt(),
            url.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        Optional<Url> url = urlRepository.findByShortCode(shortCode);
        if (url.isEmpty()) {
            throw new RuntimeException("Short URL not found: " + shortCode);
        }

        Url urlEntity = url.get();
        if (urlEntity.isExpired()) {
            throw new RuntimeException("Short URL has expired: " + shortCode);
        }

        return urlEntity.getOriginalUrl();
    }

    public void incrementClickCount(String shortCode) {
        Optional<Url> url = urlRepository.findByShortCode(shortCode);
        if (url.isPresent()) {
            Url urlEntity = url.get();
            urlEntity.incrementClickCount();
            urlRepository.save(urlEntity);
        }
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getUrlStats(String shortCode) {
        Optional<Url> url = urlRepository.findByShortCode(shortCode);
        if (url.isEmpty()) {
            throw new RuntimeException("Short URL not found: " + shortCode);
        }

        Url urlEntity = url.get();
        return new UrlStatsResponse(
            urlEntity.getShortCode(),
            urlEntity.getOriginalUrl(),
            baseUrl + "/" + urlEntity.getShortCode(),
            urlEntity.getCreatedAt(),
            urlEntity.getExpiresAt(),
            urlEntity.getClickCount()
        );
    }

    public void deleteUrl(String shortCode) {
        Optional<Url> url = urlRepository.findByShortCode(shortCode);
        if (url.isPresent()) {
            urlRepository.delete(url.get());
        } else {
            throw new RuntimeException("Short URL not found: " + shortCode);
        }
    }

    private String generateShortCode() {
        StringBuilder shortCode = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            shortCode.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return shortCode.toString();
    }
}
