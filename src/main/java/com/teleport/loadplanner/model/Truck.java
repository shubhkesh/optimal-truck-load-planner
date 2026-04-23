package com.teleport.loadplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Truck {
    
    @NotBlank(message = "Truck ID is required")
    private String id;
    
    @JsonProperty("max_weight_lbs")
    @Positive(message = "Max weight must be positive")
    private int maxWeightLbs;
    
    @JsonProperty("max_volume_cuft")
    @Positive(message = "Max volume must be positive")
    private int maxVolumeCuft;
}
