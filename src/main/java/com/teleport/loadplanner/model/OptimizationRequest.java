package com.teleport.loadplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationRequest {
    
    @NotNull(message = "Truck information is required")
    @Valid
    private Truck truck;
    
    @NotEmpty(message = "At least one order is required")
    @Valid
    private List<Order> orders;

    @JsonProperty("optimization_mode")
    private OptimizationMode optimizationMode = OptimizationMode.REVENUE;

    public OptimizationRequest(Truck truck, List<Order> orders) {
        this.truck = truck;
        this.orders = orders;
        this.optimizationMode = OptimizationMode.REVENUE;
    }
}
