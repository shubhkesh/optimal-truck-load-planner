package com.teleport.loadplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationResponse {
    
    @JsonProperty("truck_id")
    private String truckId;
    
    @JsonProperty("selected_order_ids")
    private List<String> selectedOrderIds;
    
    @JsonProperty("total_payout_cents")
    private long totalPayoutCents;
    
    @JsonProperty("total_weight_lbs")
    private int totalWeightLbs;
    
    @JsonProperty("total_volume_cuft")
    private int totalVolumeCuft;
    
    @JsonProperty("utilization_weight_percent")
    private double utilizationWeightPercent;
    
    @JsonProperty("utilization_volume_percent")
    private double utilizationVolumePercent;
    
    public static OptimizationResponse fromSelection(
            String truckId,
            List<String> selectedOrderIds,
            long totalPayoutCents,
            int totalWeightLbs,
            int totalVolumeCuft,
            int maxWeightLbs,
            int maxVolumeCuft) {
        
        double weightUtilization = calculateUtilization(totalWeightLbs, maxWeightLbs);
        double volumeUtilization = calculateUtilization(totalVolumeCuft, maxVolumeCuft);
        
        return OptimizationResponse.builder()
                .truckId(truckId)
                .selectedOrderIds(selectedOrderIds)
                .totalPayoutCents(totalPayoutCents)
                .totalWeightLbs(totalWeightLbs)
                .totalVolumeCuft(totalVolumeCuft)
                .utilizationWeightPercent(weightUtilization)
                .utilizationVolumePercent(volumeUtilization)
                .build();
    }
    
    private static double calculateUtilization(int used, int max) {
        if (max == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(used)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(max), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
