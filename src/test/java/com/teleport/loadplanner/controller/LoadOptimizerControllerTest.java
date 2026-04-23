package com.teleport.loadplanner.controller;

import com.teleport.loadplanner.model.OptimizationRequest;
import com.teleport.loadplanner.model.OptimizationResponse;
import com.teleport.loadplanner.model.Order;
import com.teleport.loadplanner.model.Truck;
import com.teleport.loadplanner.service.LoadOptimizerService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoadOptimizerController.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoadOptimizerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LoadOptimizerService optimizerService;
    
    @Test
    void optimize() throws Exception {
        OptimizationResponse mockResponse = OptimizationResponse.builder()
                .truckId("truck-123")
                .selectedOrderIds(List.of("ord-001", "ord-002"))
                .totalPayoutCents(430000L)
                .totalWeightLbs(30000)
                .totalVolumeCuft(2100)
                .utilizationWeightPercent(68.18)
                .utilizationVolumePercent(70.0)
                .build();
        
        when(optimizerService.optimize(any(OptimizationRequest.class))).thenReturn(mockResponse);
        
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
                    }
                  ]
                }
                """;
        
        mockMvc.perform(post("/api/v1/load-optimizer/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truck_id").value("truck-123"))
                .andExpect(jsonPath("$.total_payout_cents").value(430000))
                .andExpect(jsonPath("$.selected_order_ids").isArray());
    }
    
    @Test
    void optimize_invalid_request_missing_truck() throws Exception {
        String requestJson = """
                {
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
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void optimize_invalid_request_empty_orders() throws Exception {
        String requestJson = """
                {
                  "truck": {
                    "id": "truck-123",
                    "max_weight_lbs": 44000,
                    "max_volume_cuft": 3000
                  },
                  "orders": []
                }
                """;
        
        mockMvc.perform(post("/api/v1/load-optimizer/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void optimize_invalid_request_negative_weight() throws Exception {
        String requestJson = """
                {
                  "truck": {
                    "id": "truck-123",
                    "max_weight_lbs": -1000,
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
                .andExpect(status().isBadRequest());
    }
}
