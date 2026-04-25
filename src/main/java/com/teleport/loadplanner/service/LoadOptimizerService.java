package com.teleport.loadplanner.service;

import com.teleport.loadplanner.exception.InvalidRequestException;
import com.teleport.loadplanner.exception.PayloadTooLargeException;
import com.teleport.loadplanner.model.OptimizationMode;
import com.teleport.loadplanner.model.OptimizationRequest;
import com.teleport.loadplanner.model.OptimizationResponse;
import com.teleport.loadplanner.model.Order;
import com.teleport.loadplanner.model.ParetoSolution;
import com.teleport.loadplanner.model.Truck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LoadOptimizerService {

    private static final Logger logger = LoggerFactory.getLogger(LoadOptimizerService.class);
    private static final int MAX_ORDERS = 22;
    private static final int MAX_PARETO_SOLUTIONS = 10;

    private record DPState(long[] payout, int[] weight, int[] volume, boolean[] valid, int n, int bestRevenueMask) {}

    @Cacheable(value = "optimizations", key = "#request")
    public OptimizationResponse optimize(OptimizationRequest request) {
        validateRequest(request);

        Truck truck = request.getTruck();
        List<Order> orders = request.getOrders();
        OptimizationMode mode = request.getOptimizationMode() != null
                ? request.getOptimizationMode() : OptimizationMode.REVENUE;

        logger.info("Optimizing load for truck {} with {} orders, mode={}", truck.getId(), orders.size(), mode);

        long startTime = System.currentTimeMillis();

        List<Order> compatibleOrders = filterCompatibleOrders(orders);

        if (compatibleOrders.isEmpty()) {
            logger.warn("No compatible orders found for truck {}", truck.getId());
            return createEmptyResponse(truck);
        }

        DPState dp = runDP(truck, compatibleOrders);
        int bestMask = selectBestMask(dp, mode, truck, compatibleOrders);
        List<ParetoSolution> pareto = computeParetoFrontier(dp, compatibleOrders, truck);

        OptimizationResponse response = buildResponse(truck, compatibleOrders, bestMask, pareto);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Optimization completed in {}ms. Selected {} orders, payout=${}, pareto={} solutions",
                duration, response.getSelectedOrderIds().size(),
                response.getTotalPayoutCents() / 100.0, pareto.size());

        return response;
    }
    
    private void validateRequest(OptimizationRequest request) {
        if (request.getOrders().size() > MAX_ORDERS) {
            throw new PayloadTooLargeException(
                    String.format("Maximum %d orders allowed, received %d", MAX_ORDERS, request.getOrders().size()));
        }
        
        for (Order order : request.getOrders()) {
            if (order.getPickupDate().isAfter(order.getDeliveryDate())) {
                throw new InvalidRequestException(
                        String.format("Order %s has pickup date after delivery date", order.getId()));
            }
        }
    }
    
    private List<Order> filterCompatibleOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        
        String primaryOrigin = orders.get(0).getOrigin();
        String primaryDestination = orders.get(0).getDestination();
        
        List<Order> compatible = new ArrayList<>();
        
        for (Order order : orders) {
            if (isCompatible(order, primaryOrigin, primaryDestination)) {
                compatible.add(order);
            }
        }
        
        return compatible;
    }
    
    private boolean isCompatible(Order order, String origin, String destination) {
        return order.getOrigin().equalsIgnoreCase(origin) &&
               order.getDestination().equalsIgnoreCase(destination);
    }
    
    private DPState runDP(Truck truck, List<Order> orders) {
        int n = orders.size();
        int totalStates = 1 << n;

        long[] dpPayout = new long[totalStates];
        int[] dpWeight = new int[totalStates];
        int[] dpVolume = new int[totalStates];
        int[] dpHazmat = new int[totalStates];
        int[] dpNonHazmat = new int[totalStates];
        boolean[] dpValid = new boolean[totalStates];

        dpValid[0] = true;
        int bestRevenueMask = 0;
        long bestRevenuePayout = 0;

        for (int mask = 0; mask < totalStates; mask++) {
            if (!dpValid[mask]) continue;

            if (dpPayout[mask] > bestRevenuePayout) {
                bestRevenuePayout = dpPayout[mask];
                bestRevenueMask = mask;
            }

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) continue;

                Order order = orders.get(i);
                int newWeight = dpWeight[mask] + order.getWeightLbs();
                int newVolume = dpVolume[mask] + order.getVolumeCuft();
                int newHazmat = dpHazmat[mask] + (order.getIsHazmat() ? 1 : 0);
                int newNonHazmat = dpNonHazmat[mask] + (order.getIsHazmat() ? 0 : 1);

                if (newWeight > truck.getMaxWeightLbs() || newVolume > truck.getMaxVolumeCuft()) continue;
                if (newHazmat > 1 || (newHazmat > 0 && newNonHazmat > 0)) continue;

                int newMask = mask | (1 << i);
                long newPayout = dpPayout[mask] + order.getPayoutCents();

                if (!dpValid[newMask] || newPayout > dpPayout[newMask]) {
                    dpValid[newMask] = true;
                    dpPayout[newMask] = newPayout;
                    dpWeight[newMask] = newWeight;
                    dpVolume[newMask] = newVolume;
                    dpHazmat[newMask] = newHazmat;
                    dpNonHazmat[newMask] = newNonHazmat;
                }
            }
        }

        return new DPState(dpPayout, dpWeight, dpVolume, dpValid, n, bestRevenueMask);
    }

    private int selectBestMask(DPState dp, OptimizationMode mode, Truck truck, List<Order> orders) {
        if (mode == OptimizationMode.REVENUE) {
            return dp.bestRevenueMask();
        }

        int bestMask = 0;
        double bestScore = -1;
        long maxPossiblePayout = orders.stream().mapToLong(Order::getPayoutCents).sum();
        int totalStates = 1 << dp.n();

        for (int mask = 1; mask < totalStates; mask++) {
            if (!dp.valid()[mask]) continue;
            double score = computeScore(mask, dp, mode, truck, maxPossiblePayout);
            if (score > bestScore) {
                bestScore = score;
                bestMask = mask;
            }
        }
        return bestMask;
    }

    private double computeScore(int mask, DPState dp, OptimizationMode mode, Truck truck, long maxPossiblePayout) {
        double weightUtil = truck.getMaxWeightLbs() > 0
                ? (double) dp.weight()[mask] / truck.getMaxWeightLbs() : 0;
        double volumeUtil = truck.getMaxVolumeCuft() > 0
                ? (double) dp.volume()[mask] / truck.getMaxVolumeCuft() : 0;
        double avgUtil = (weightUtil + volumeUtil) / 2.0;

        return switch (mode) {
            case REVENUE -> (double) dp.payout()[mask];
            case UTILIZATION -> avgUtil;
            case BALANCED -> {
                double revenueScore = maxPossiblePayout > 0
                        ? (double) dp.payout()[mask] / maxPossiblePayout : 0;
                yield 0.5 * revenueScore + 0.5 * avgUtil;
            }
        };
    }

    private List<ParetoSolution> computeParetoFrontier(DPState dp, List<Order> orders, Truck truck) {
        int totalStates = 1 << dp.n();
        int numBuckets = 100;

        long[] bucketMaxPayout = new long[numBuckets + 1];
        int[] bucketBestMask = new int[numBuckets + 1];
        java.util.Arrays.fill(bucketBestMask, -1);

        for (int mask = 1; mask < totalStates; mask++) {
            if (!dp.valid()[mask]) continue;
            double wUtil = truck.getMaxWeightLbs() > 0
                    ? (double) dp.weight()[mask] / truck.getMaxWeightLbs() : 0;
            double vUtil = truck.getMaxVolumeCuft() > 0
                    ? (double) dp.volume()[mask] / truck.getMaxVolumeCuft() : 0;
            int bucket = Math.min(numBuckets, (int) Math.round((wUtil + vUtil) / 2.0 * numBuckets));
            if (dp.payout()[mask] > bucketMaxPayout[bucket]) {
                bucketMaxPayout[bucket] = dp.payout()[mask];
                bucketBestMask[bucket] = mask;
            }
        }

        record Candidate(int mask, long payout, int bucket) {}
        List<Candidate> candidates = new ArrayList<>();
        for (int b = 0; b <= numBuckets; b++) {
            if (bucketBestMask[b] >= 0) {
                candidates.add(new Candidate(bucketBestMask[b], bucketMaxPayout[b], b));
            }
        }

        candidates.sort(Comparator.comparingLong(Candidate::payout).reversed()
                .thenComparingInt(Candidate::bucket).reversed());

        List<ParetoSolution> pareto = new ArrayList<>();
        int maxBucketSeen = -1;

        for (Candidate c : candidates) {
            if (c.bucket() > maxBucketSeen) {
                maxBucketSeen = c.bucket();
                pareto.add(buildParetoSolution(c.mask(), orders, dp, truck));
                if (pareto.size() >= MAX_PARETO_SOLUTIONS) break;
            }
        }

        return pareto;
    }

    private ParetoSolution buildParetoSolution(int mask, List<Order> orders, DPState dp, Truck truck) {
        List<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            if ((mask & (1 << i)) != 0) selectedIds.add(orders.get(i).getId());
        }
        return ParetoSolution.builder()
                .selectedOrderIds(selectedIds)
                .totalPayoutCents(dp.payout()[mask])
                .totalWeightLbs(dp.weight()[mask])
                .totalVolumeCuft(dp.volume()[mask])
                .utilizationWeightPercent(OptimizationResponse.calculateUtilization(dp.weight()[mask], truck.getMaxWeightLbs()))
                .utilizationVolumePercent(OptimizationResponse.calculateUtilization(dp.volume()[mask], truck.getMaxVolumeCuft()))
                .build();
    }
    
    private OptimizationResponse buildResponse(Truck truck, List<Order> orders, int mask, List<ParetoSolution> pareto) {
        List<String> selectedIds = new ArrayList<>();
        long totalPayout = 0;
        int totalWeight = 0;
        int totalVolume = 0;

        for (int i = 0; i < orders.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                Order order = orders.get(i);
                selectedIds.add(order.getId());
                totalPayout += order.getPayoutCents();
                totalWeight += order.getWeightLbs();
                totalVolume += order.getVolumeCuft();
            }
        }

        return OptimizationResponse.fromSelection(
                truck.getId(), selectedIds, totalPayout, totalWeight, totalVolume,
                truck.getMaxWeightLbs(), truck.getMaxVolumeCuft(), pareto);
    }

    private OptimizationResponse createEmptyResponse(Truck truck) {
        return OptimizationResponse.fromSelection(
                truck.getId(), new ArrayList<>(), 0L, 0, 0,
                truck.getMaxWeightLbs(), truck.getMaxVolumeCuft(), new ArrayList<>());
    }
}
