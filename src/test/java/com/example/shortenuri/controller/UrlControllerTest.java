package com.example.shortenuri.controller;

import com.example.shortenuri.dto.CreateUrlRequest;
import com.example.shortenuri.dto.CreateUrlResponse;
import com.example.shortenuri.dto.UrlStatsResponse;
import com.example.shortenuri.service.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUrlRequest validRequest;
    private CreateUrlResponse validResponse;
    private UrlStatsResponse validStatsResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUrlRequest();
        validRequest.setOriginalUrl("https://www.google.com");

        validResponse = new CreateUrlResponse(
            "abc12345",
            "https://www.google.com",
            "http://localhost:8080/abc12345",
            LocalDateTime.now()
        );

        validStatsResponse = new UrlStatsResponse(
            "abc12345",
            "https://www.google.com",
            "http://localhost:8080/abc12345",
            LocalDateTime.now(),
            null,
            5L
        );
    }

    @Test
    void createShortUrl_Success() throws Exception {
        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc12345"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc12345"));

        verify(urlService, times(1)).createShortUrl(any(CreateUrlRequest.class));
    }

    @Test
    void createShortUrl_WithCustomCode_Success() throws Exception {
        validRequest.setCustomShortCode("custom123");
        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        verify(urlService, times(1)).createShortUrl(any(CreateUrlRequest.class));
    }

    @Test
    void createShortUrl_InvalidRequest_BadRequest() throws Exception {
        CreateUrlRequest invalidRequest = new CreateUrlRequest();
        invalidRequest.setOriginalUrl(""); // 빈 URL

        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShortUrl_ServiceException_BadRequest() throws Exception {
        when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                .thenThrow(new RuntimeException("Short code already exists"));

        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUrlStats_Success() throws Exception {
        when(urlService.getUrlStats("abc12345")).thenReturn(validStatsResponse);

        mockMvc.perform(get("/api/urls/abc12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc12345"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"))
                .andExpect(jsonPath("$.clickCount").value(5));

        verify(urlService, times(1)).getUrlStats("abc12345");
    }

    @Test
    void getUrlStats_NotFound() throws Exception {
        when(urlService.getUrlStats("nonexistent"))
                .thenThrow(new RuntimeException("Short URL not found"));

        mockMvc.perform(get("/api/urls/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUrl_Success() throws Exception {
        doNothing().when(urlService).deleteUrl("abc12345");

        mockMvc.perform(delete("/api/urls/abc12345"))
                .andExpect(status().isNoContent());

        verify(urlService, times(1)).deleteUrl("abc12345");
    }

    @Test
    void deleteUrl_NotFound() throws Exception {
        doThrow(new RuntimeException("Short URL not found"))
                .when(urlService).deleteUrl("nonexistent");

        mockMvc.perform(delete("/api/urls/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectToOriginalUrl_Success() throws Exception {
        when(urlService.getOriginalUrl("abc12345")).thenReturn("https://www.google.com");
        doNothing().when(urlService).incrementClickCount("abc12345");

        mockMvc.perform(get("/api/urls/redirect/abc12345"))
                .andExpect(status().is3xxRedirection());

        verify(urlService, times(1)).getOriginalUrl("abc12345");
        verify(urlService, times(1)).incrementClickCount("abc12345");
    }

    @Test
    void redirectToOriginalUrl_NotFound() throws Exception {
        when(urlService.getOriginalUrl("nonexistent"))
                .thenThrow(new RuntimeException("Short URL not found"));

        mockMvc.perform(get("/api/urls/redirect/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
