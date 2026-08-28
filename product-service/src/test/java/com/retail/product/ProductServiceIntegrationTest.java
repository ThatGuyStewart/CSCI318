package com.retail.product;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.product.domain.ProductCategory;
import com.retail.product.dto.ProductCreateRequest;
import com.retail.product.dto.ProductResponse;
import com.retail.product.dto.ProductUpdateRequest;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class ProductServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateAndGetProduct_HappyPath() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Wireless Headphones", ProductCategory.Electronics, 150.0, "Noise cancelling headphones"
        );

        String responseJson = mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Wireless Headphones"))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.price").value(150.0))
                .andReturn().getResponse().getContentAsString();

        ProductResponse created = objectMapper.readValue(responseJson, ProductResponse.class);

        // Get by ID
        mockMvc.perform(get("/product/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Wireless Headphones"));

        // List all
        mockMvc.perform(get("/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testCreateProduct_ValidationFail_Returns400() throws Exception {
        ProductCreateRequest invalid = new ProductCreateRequest("", null, -10.0, "Invalid");

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void testGetProductNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/product/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testUpdateProduct_HappyAndNotFound() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Coffee Maker", ProductCategory.Appliances, 80.0, "Filter coffee machine"
        );

        String responseJson = mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ProductResponse created = objectMapper.readValue(responseJson, ProductResponse.class);

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Espresso Machine", ProductCategory.Appliances, 120.0, "Espresso maker"
        );

        mockMvc.perform(put("/product/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Espresso Machine"))
                .andExpect(jsonPath("$.price").value(120.0));
    }

    @Test
    void testDeleteProduct_HappyPath() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Desk Lamp", ProductCategory.Furniture, 45.0, "LED Desk Lamp"
        );

        String responseJson = mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ProductResponse created = objectMapper.readValue(responseJson, ProductResponse.class);

        mockMvc.perform(delete("/product/" + created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/product/" + created.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCategoryAndSearchFilters() throws Exception {
        ProductCreateRequest p1 = new ProductCreateRequest("Mountain Bike", ProductCategory.Sports, 500.0, "All terrain bike");
        ProductCreateRequest p2 = new ProductCreateRequest("Soccer Ball", ProductCategory.Sports, 30.0, "Size 5 ball");
        ProductCreateRequest p3 = new ProductCreateRequest("Sofa Bed", ProductCategory.Furniture, 750.0, "Comfortable sofa");

        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p1))).andExpect(status().isCreated());
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p2))).andExpect(status().isCreated());
        mockMvc.perform(post("/product").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p3))).andExpect(status().isCreated());

        // Category filter
        mockMvc.perform(get("/product/category/Sports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].category", everyItem(equalTo("Sports"))));

        // Name search
        mockMvc.perform(get("/product/search").param("name", "Bike"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", containsString("Bike")));
    }

    @Test
    void testProductEventEndpoints() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest("Yoga Mat", ProductCategory.Grocery, 25.0, "Exercise mat");

        String responseJson = mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ProductResponse created = objectMapper.readValue(responseJson, ProductResponse.class);

        mockMvc.perform(get("/product/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/product/" + created.getId() + "/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
