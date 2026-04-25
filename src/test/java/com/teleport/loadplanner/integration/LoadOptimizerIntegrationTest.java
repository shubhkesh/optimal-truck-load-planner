package com.teleport.loadplanner.integration;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoadOptimizerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void optimize_full_flow_with_sample_data() throws Exception {
        String requestJson = """
                {
                  "truck": {
                    "id": "truck-123",
                    "max_weight_lbs": 44000,
                    "max_volume_cuft": 3000
                  },
                  "orders": [
                    {
                      "id": "ord-001",
                      "payout_cents": 250000,
                      "weight_lbs": 18000,
                      "volume_cuft": 1200,
                      "origin": "Los Angeles, CA",
                      "destination": "Dallas, TX",
                      "pickup_date": "2025-12-05",
                      "delivery_date": "2025-12-09",
                      "is_hazmat": false
                    },
                    {
                      "id": "ord-002",
                      "payout_cents": 180000,
                      "weight_lbs": 12000,
                      "volume_cuft": 900,
                      "origin": "Los Angeles, CA",
                      "destination": "Dallas, TX",
                      "pickup_date": "2025-12-04",
                      "delivery_date": "2025-12-10",
                      "is_hazmat": false
                    },
                    {
                      "id": "ord-003",
                      "payout_cents": 320000,
                      "weight_lbs": 30000,
                      "volume_cuft": 1800,
                      "origin": "Los Angeles, CA",
                      "destination": "Dallas, TX",
                      "pickup_date": "2025-12-06",
                      "delivery_date": "2025-12-08",
                      "is_hazmat": true
                    }
                  ]
                }
                """;
        
        mockMvc.perform(post("/api/v1/load-optimizer/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truck_id").value("truck-123"))
                .andExpect(jsonPath("$.selected_order_ids").isArray())
                .andExpect(jsonPath("$.selected_order_ids.length()").value(2))
                .andExpect(jsonPath("$.total_payout_cents").value(430000))
                .andExpect(jsonPath("$.total_weight_lbs").value(30000))
                .andExpect(jsonPath("$.total_volume_cuft").value(2100));
    }
    
    @Test
    void optimize_large_order_set() throws Exception {
        StringBuilder ordersJson = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            if (i > 1) ordersJson.append(",");
            ordersJson.append(String.format("""
                    {
                      "id": "ord-%03d",
                      "payout_cents": %d,
                      "weight_lbs": %d,
                      "volume_cuft": %d,
                      "origin": "Los Angeles, CA",
                      "destination": "Dallas, TX",
                      "pickup_date": "2025-12-05",
                      "delivery_date": "2025-12-10",
                      "is_hazmat": false
                    }
                    """, i, 50000 + (i * 10000), 1000 + (i * 500), 100 + (i * 50)));
        }
        
        String requestJson = String.format("""
                {
                  "truck": {
                    "id": "truck-large",
                    "max_weight_lbs": 44000,
                    "max_volume_cuft": 3000
                  },
                  "orders": [%s]
                }
                """, ordersJson.toString());
        
        mockMvc.perform(post("/api/v1/load-optimizer/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truck_id").value("truck-large"))
                .andExpect(jsonPath("$.selected_order_ids").isArray());
    }
    
    @Test
    void optimize_validation_error() throws Exception {
        String requestJson = """
                {
                  "truck": {
                    "id": "",
                    "max_weight_lbs": 44000,
                    "max_volume_cuft": 3000
                  },
                  "orders": [
                    {
                      "id": "ord-001",
                      "payout_cents": 250000,
                      "weight_lbs": 18000,
                      "volume_cuft": 1200,
                      "origin": "Los Angeles, CA",
                      "destination": "Dallas, TX",
                      "pickup_date": "2025-12-05",
                      "delivery_date": "2025-12-09",
                      "is_hazmat": false
                    }
                  ]
                }
                """;
        
        mockMvc.perform(post("/api/v1/load-optimizer/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
