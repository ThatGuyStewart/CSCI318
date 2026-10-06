package com.retail.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetRecommendations_BlankMessage_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/product/recommendation").param("message", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRecommendations_MissingMessage_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/product/recommendation"))
                .andExpect(status().isBadRequest());
    }

    // AI is disabled by default (retail.ai.recommendation.enabled=false), so no RecommendationAgent
    // bean exists and the endpoint must fail gracefully instead of throwing or crashing the app.
    @Test
    void testGetRecommendations_AiDisabled_ReturnsServiceUnavailable() throws Exception {
        mockMvc.perform(get("/product/recommendation").param("message", "I need a gift for a home cook"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }
}
