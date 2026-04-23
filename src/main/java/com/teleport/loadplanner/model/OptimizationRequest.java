package com.teleport.loadplanner.model;

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
}
