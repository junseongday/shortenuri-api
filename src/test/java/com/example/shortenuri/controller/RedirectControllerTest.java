package com.example.shortenuri.controller;

import com.example.shortenuri.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        reset(urlService);
    }

    @Test
    void redirectToOriginalUrl_Success() throws Exception {
        // Given
        String shortCode = "abc12345";
        String originalUrl = "https://www.google.com";
        
        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().is3xxRedirection());

        verify(urlService, times(1)).getOriginalUrl(shortCode);
        verify(urlService, times(1)).incrementClickCount(shortCode);
    }

    @Test
    void redirectToOriginalUrl_NotFound() throws Exception {
        // Given
        String shortCode = "nonexistent";
        
        when(urlService.getOriginalUrl(shortCode))
                .thenThrow(new RuntimeException("Short URL not found"));

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound());

        verify(urlService, times(1)).getOriginalUrl(shortCode);
        verify(urlService, never()).incrementClickCount(any());
    }

    @Test
    void redirectToOriginalUrl_ExpiredUrl() throws Exception {
        // Given
        String shortCode = "expired123";
        
        when(urlService.getOriginalUrl(shortCode))
                .thenThrow(new RuntimeException("Short URL has expired"));

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound());

        verify(urlService, times(1)).getOriginalUrl(shortCode);
        verify(urlService, never()).incrementClickCount(any());
    }
}
