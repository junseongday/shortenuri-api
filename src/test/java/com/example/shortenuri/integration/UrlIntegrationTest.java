package com.example.shortenuri.integration;

import com.example.shortenuri.dto.CreateUrlRequest;
import com.example.shortenuri.dto.CreateUrlResponse;
import com.example.shortenuri.dto.UrlStatsResponse;
import com.example.shortenuri.entity.Url;
import com.example.shortenuri.repository.UrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Transactional
class UrlIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        urlRepository.deleteAll();
    }

    @Test
    void createShortUrl_Integration_Success() throws Exception {
        // Given
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.google.com");

        // When & Then
        String responseJson = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"))
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreateUrlResponse response = objectMapper.readValue(responseJson, CreateUrlResponse.class);
        
        // Verify database
        assertTrue(urlRepository.existsByShortCode(response.getShortCode()));
        Url savedUrl = urlRepository.findByShortCode(response.getShortCode()).orElse(null);
        assertNotNull(savedUrl);
        assertEquals("https://www.google.com", savedUrl.getOriginalUrl());
        assertEquals(0L, savedUrl.getClickCount());
    }

    @Test
    void createShortUrl_WithCustomCode_Integration_Success() throws Exception {
        // Given
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.github.com");
        request.setCustomShortCode("github123");

        // When & Then
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("github123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.github.com"));

        // Verify database
        assertTrue(urlRepository.existsByShortCode("github123"));
    }

    @Test
    void createShortUrl_DuplicateCustomCode_Integration_BadRequest() throws Exception {
        // Given - Create first URL
        CreateUrlRequest request1 = new CreateUrlRequest();
        request1.setOriginalUrl("https://www.google.com");
        request1.setCustomShortCode("duplicate123");

        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Given - Create second URL with same custom code
        CreateUrlRequest request2 = new CreateUrlRequest();
        request2.setOriginalUrl("https://www.github.com");
        request2.setCustomShortCode("duplicate123");

        // When & Then
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShortUrl_WithExpiration_Integration_Success() throws Exception {
        // Given
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setExpiresAt(LocalDateTime.now().plusDays(7));

        // When & Then
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify database
        String shortCode = urlRepository.findByOriginalUrl("https://www.example.com")
                .map(Url::getShortCode)
                .orElse(null);
        assertNotNull(shortCode);
        
        Url savedUrl = urlRepository.findByShortCode(shortCode).orElse(null);
        assertNotNull(savedUrl);
        assertNotNull(savedUrl.getExpiresAt());
    }

    @Test
    void getUrlStats_Integration_Success() throws Exception {
        // Given - Create URL first
        CreateUrlRequest createRequest = new CreateUrlRequest();
        createRequest.setOriginalUrl("https://www.test.com");
        createRequest.setCustomShortCode("test123");

        String createResponseJson = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreateUrlResponse createResponse = objectMapper.readValue(createResponseJson, CreateUrlResponse.class);

        // When & Then - Get stats
        mockMvc.perform(get("/api/urls/" + createResponse.getShortCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("test123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.test.com"))
                .andExpect(jsonPath("$.clickCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getUrlStats_NonExistentCode_Integration_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/urls/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUrl_Integration_Success() throws Exception {
        // Given - Create URL first
        CreateUrlRequest createRequest = new CreateUrlRequest();
        createRequest.setOriginalUrl("https://www.delete.com");
        createRequest.setCustomShortCode("delete123");

        String createResponseJson = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreateUrlResponse createResponse = objectMapper.readValue(createResponseJson, CreateUrlResponse.class);

        // Verify URL exists
        assertTrue(urlRepository.existsByShortCode("delete123"));

        // When & Then - Delete URL
        mockMvc.perform(delete("/api/urls/" + createResponse.getShortCode()))
                .andExpect(status().isNoContent());

        // Verify URL is deleted
        assertFalse(urlRepository.existsByShortCode("delete123"));
    }

    @Test
    void deleteUrl_NonExistentCode_Integration_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/urls/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectToOriginalUrl_Integration_Success() throws Exception {
        // Given - Create URL first
        CreateUrlRequest createRequest = new CreateUrlRequest();
        createRequest.setOriginalUrl("https://www.redirect.com");
        createRequest.setCustomShortCode("redirect123");

        String createResponseJson = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreateUrlResponse createResponse = objectMapper.readValue(createResponseJson, CreateUrlResponse.class);

        // When & Then - Redirect
        mockMvc.perform(get("/api/urls/redirect/" + createResponse.getShortCode()))
                .andExpect(status().is3xxRedirection());

        // Verify click count is incremented
        Url url = urlRepository.findByShortCode("redirect123").orElse(null);
        assertNotNull(url);
        assertEquals(1L, url.getClickCount());
    }

    @Test
    void redirectToOriginalUrl_NonExistentCode_Integration_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/urls/redirect/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createShortUrl_InvalidRequest_Integration_BadRequest() throws Exception {
        // Given - Empty URL
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("");

        // When & Then
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShortUrl_InvalidUrlFormat_Integration_BadRequest() throws Exception {
        // Given - Invalid URL format
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("not-a-valid-url");

        // When & Then
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShortUrl_DuplicateOriginalUrl_Integration_ReturnsExisting() throws Exception {
        // Given - Create first URL
        CreateUrlRequest request1 = new CreateUrlRequest();
        request1.setOriginalUrl("https://www.duplicate.com");

        String response1Json = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreateUrlResponse response1 = objectMapper.readValue(response1Json, CreateUrlResponse.class);

        // Given - Create second URL with same original URL
        CreateUrlRequest request2 = new CreateUrlRequest();
        request2.setOriginalUrl("https://www.duplicate.com");

        String response2Json = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreateUrlResponse response2 = objectMapper.readValue(response2Json, CreateUrlResponse.class);

        // Verify both responses have the same short code
        assertEquals(response1.getShortCode(), response2.getShortCode());
        assertEquals(response1.getShortUrl(), response2.getShortUrl());
    }
}
