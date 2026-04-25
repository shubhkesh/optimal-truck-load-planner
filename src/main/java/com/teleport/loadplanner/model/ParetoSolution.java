package com.teleport.loadplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParetoSolution {

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
}
