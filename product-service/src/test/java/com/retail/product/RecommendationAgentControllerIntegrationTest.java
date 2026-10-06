package com.retail.product;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.retail.product.domain.ProductCategory;
import com.retail.product.domain.ProductView;
import com.retail.product.repository.ProductViewRepository;
import com.retail.product.service.RecommendationAgent;

@SpringBootTest(properties = "retail.ai.recommendation.enabled=true")
@AutoConfigureMockMvc
class RecommendationAgentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductViewRepository productViewRepository;

    @MockBean
    private RecommendationAgent recommendationAgent;

    @BeforeEach
    void setUp() {
        productViewRepository.deleteAll();
    }

    @Test
    void wellFormedJson_productsResolved_cappedToFive() throws Exception {
        // create 7 products
        for (long i = 1; i <= 7; i++) {
            productViewRepository.save(new ProductView(i, "P" + i, ProductCategory.Grocery, 1.0 + i, "desc" + i));
        }

        when(recommendationAgent.recommend(anyString(), anyString())).thenReturn(
                "{\"message\":\"Here are suggestions\",\"productIds\":[1,2,3,4,5,6,7]}"
        );

        mockMvc.perform(get("/product/recommendation").param("message", "find gifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()", Matchers.is(5)));
    }

    @Test
    void invalidJson_retryPathExercise() throws Exception {
        productViewRepository.save(new ProductView(42L, "P42", ProductCategory.Grocery, 9.99, "desc"));

        // first response invalid, second is valid
        when(recommendationAgent.recommend(anyString(), anyString()))
                .thenReturn("not a json reply")
                .thenReturn("{\"message\":\"ok\",\"productIds\":[42]}");

        mockMvc.perform(get("/product/recommendation").param("message", "something"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()", Matchers.is(1)))
                .andExpect(jsonPath("$.products[0].productId").value(42));
    }

    @Test
    void productIds_textualAndNumericCoercion() throws Exception {
        productViewRepository.save(new ProductView(100L, "P100", ProductCategory.Grocery, 5.0, "desc"));
        productViewRepository.save(new ProductView(200L, "P200", ProductCategory.Grocery, 6.0, "desc"));

        // productIds as a single number
        when(recommendationAgent.recommend(anyString(), anyString()))
                .thenReturn("{\"message\":\"one\",\"productIds\":100}")
                .thenReturn("{\"message\":\"csv\",\"productIds\":\"100,200\"}");

        // first call: numeric
        mockMvc.perform(get("/product/recommendation").param("message", "first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()", Matchers.is(1)))
                .andExpect(jsonPath("$.products[0].productId").value(100));

        // second call: textual CSV
        mockMvc.perform(get("/product/recommendation").param("message", "second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()", Matchers.is(2)));
    }

    @Test
    void bracesInsideMessageString_extractionRobustness() throws Exception {
        productViewRepository.save(new ProductView(7L, "P7", ProductCategory.Grocery, 7.0, "desc"));

        String reply = "{\"message\":\"This message contains braces { like this } and a quote \"inside\"\",\"productIds\":[7]}";
        when(recommendationAgent.recommend(anyString(), anyString())).thenReturn(reply);

        mockMvc.perform(get("/product/recommendation").param("message", "braces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()", Matchers.is(1)))
                .andExpect(jsonPath("$.message", Matchers.containsString("braces")));
    }

    @Test
    void unknownProductIds_areIgnored() throws Exception {
        productViewRepository.save(new ProductView(5L, "P5", ProductCategory.Grocery, 5.0, "desc"));

        when(recommendationAgent.recommend(anyString(), anyString()))
                .thenReturn("{\"message\":\"mixed\",\"productIds\":[9999,5]}");

        mockMvc.perform(get("/product/recommendation").param("message", "unknowns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()", Matchers.is(1)))
                .andExpect(jsonPath("$.products[0].productId").value(5));
    }
}
