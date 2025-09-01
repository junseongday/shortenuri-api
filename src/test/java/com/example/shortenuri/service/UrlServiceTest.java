package com.example.shortenuri.service;

import com.example.shortenuri.dto.CreateUrlRequest;
import com.example.shortenuri.dto.CreateUrlResponse;
import com.example.shortenuri.dto.UrlStatsResponse;
import com.example.shortenuri.entity.Url;
import com.example.shortenuri.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    private CreateUrlRequest validRequest;
    private Url validUrl;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUrlRequest();
        validRequest.setOriginalUrl("https://www.google.com");

        validUrl = new Url("abc12345", "https://www.google.com");
        validUrl.setId(1L);
        validUrl.setCreatedAt(LocalDateTime.now());
        validUrl.setClickCount(0L);

        // Set base URL for testing
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void createShortUrl_NewUrl_Success() {
        // Given
        when(urlRepository.findByOriginalUrl(validRequest.getOriginalUrl())).thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode(any())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(validUrl);

        // When
        CreateUrlResponse response = urlService.createShortUrl(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("abc12345", response.getShortCode());
        assertEquals("https://www.google.com", response.getOriginalUrl());
        assertEquals("http://localhost:8080/abc12345", response.getShortUrl());
        assertNotNull(response.getCreatedAt());

        verify(urlRepository).findByOriginalUrl(validRequest.getOriginalUrl());
        verify(urlRepository).existsByShortCode(any());
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void createShortUrl_ExistingUrl_ReturnsExisting() {
        // Given
        when(urlRepository.findByOriginalUrl(validRequest.getOriginalUrl())).thenReturn(Optional.of(validUrl));

        // When
        CreateUrlResponse response = urlService.createShortUrl(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("abc12345", response.getShortCode());
        assertEquals("https://www.google.com", response.getOriginalUrl());

        verify(urlRepository).findByOriginalUrl(validRequest.getOriginalUrl());
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void createShortUrl_WithCustomCode_Success() {
        // Given
        validRequest.setCustomShortCode("custom123");
        when(urlRepository.findByOriginalUrl(validRequest.getOriginalUrl())).thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode("custom123")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(validUrl);

        // When
        CreateUrlResponse response = urlService.createShortUrl(validRequest);

        // Then
        assertNotNull(response);
        verify(urlRepository).existsByShortCode("custom123");
    }

    @Test
    void createShortUrl_CustomCodeExists_ThrowsException() {
        // Given
        validRequest.setCustomShortCode("existing123");
        when(urlRepository.findByOriginalUrl(validRequest.getOriginalUrl())).thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode("existing123")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            urlService.createShortUrl(validRequest);
        });

        assertEquals("Short code already exists: existing123", exception.getMessage());
    }

    @Test
    void createShortUrl_WithExpiration_Success() {
        // Given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        validRequest.setExpiresAt(expiresAt);
        when(urlRepository.findByOriginalUrl(validRequest.getOriginalUrl())).thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode(any())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(validUrl);

        // When
        CreateUrlResponse response = urlService.createShortUrl(validRequest);

        // Then
        assertNotNull(response);
        verify(urlRepository).save(argThat(url -> expiresAt.equals(url.getExpiresAt())));
    }

    @Test
    void getOriginalUrl_ValidCode_Success() {
        // Given
        when(urlRepository.findByShortCode("abc12345")).thenReturn(Optional.of(validUrl));

        // When
        String originalUrl = urlService.getOriginalUrl("abc12345");

        // Then
        assertEquals("https://www.google.com", originalUrl);
        verify(urlRepository).findByShortCode("abc12345");
    }

    @Test
    void getOriginalUrl_InvalidCode_ThrowsException() {
        // Given
        when(urlRepository.findByShortCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            urlService.getOriginalUrl("nonexistent");
        });

        assertEquals("Short URL not found: nonexistent", exception.getMessage());
    }

    @Test
    void getOriginalUrl_ExpiredUrl_ThrowsException() {
        // Given
        validUrl.setExpiresAt(LocalDateTime.now().minusDays(1)); // 만료된 URL
        when(urlRepository.findByShortCode("abc12345")).thenReturn(Optional.of(validUrl));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            urlService.getOriginalUrl("abc12345");
        });

        assertEquals("Short URL has expired: abc12345", exception.getMessage());
    }

    @Test
    void incrementClickCount_ValidCode_Success() {
        // Given
        when(urlRepository.findByShortCode("abc12345")).thenReturn(Optional.of(validUrl));
        when(urlRepository.save(any(Url.class))).thenReturn(validUrl);

        // When
        urlService.incrementClickCount("abc12345");

        // Then
        verify(urlRepository).findByShortCode("abc12345");
        verify(urlRepository).save(validUrl);
        assertEquals(1L, validUrl.getClickCount());
    }

    @Test
    void incrementClickCount_InvalidCode_NoException() {
        // Given
        when(urlRepository.findByShortCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> {
            urlService.incrementClickCount("nonexistent");
        });

        verify(urlRepository).findByShortCode("nonexistent");
        verify(urlRepository, never()).save(any(Url.class));
    }

    @Test
    void getUrlStats_ValidCode_Success() {
        // Given
        validUrl.setClickCount(5L);
        when(urlRepository.findByShortCode("abc12345")).thenReturn(Optional.of(validUrl));

        // When
        UrlStatsResponse response = urlService.getUrlStats("abc12345");

        // Then
        assertNotNull(response);
        assertEquals("abc12345", response.getShortCode());
        assertEquals("https://www.google.com", response.getOriginalUrl());
        assertEquals("http://localhost:8080/abc12345", response.getShortUrl());
        assertEquals(5L, response.getClickCount());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void getUrlStats_InvalidCode_ThrowsException() {
        // Given
        when(urlRepository.findByShortCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            urlService.getUrlStats("nonexistent");
        });

        assertEquals("Short URL not found: nonexistent", exception.getMessage());
    }

    @Test
    void deleteUrl_ValidCode_Success() {
        // Given
        when(urlRepository.findByShortCode("abc12345")).thenReturn(Optional.of(validUrl));
        doNothing().when(urlRepository).delete(validUrl);

        // When
        urlService.deleteUrl("abc12345");

        // Then
        verify(urlRepository).findByShortCode("abc12345");
        verify(urlRepository).delete(validUrl);
    }

    @Test
    void deleteUrl_InvalidCode_ThrowsException() {
        // Given
        when(urlRepository.findByShortCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            urlService.deleteUrl("nonexistent");
        });

        assertEquals("Short URL not found: nonexistent", exception.getMessage());
        verify(urlRepository, never()).delete(any(Url.class));
    }


}
