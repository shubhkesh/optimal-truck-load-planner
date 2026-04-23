package com.teleport.loadplanner.service;

import com.teleport.loadplanner.exception.InvalidRequestException;
import com.teleport.loadplanner.model.OptimizationRequest;
import com.teleport.loadplanner.model.OptimizationResponse;
import com.teleport.loadplanner.model.Order;
import com.teleport.loadplanner.model.Truck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoadOptimizerService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadOptimizerService.class);
    private static final int MAX_ORDERS = 22;
    
    public OptimizationResponse optimize(OptimizationRequest request) {
        validateRequest(request);
        
        Truck truck = request.getTruck();
        List<Order> orders = request.getOrders();
        
        logger.info("Optimizing load for truck {} with {} orders", truck.getId(), orders.size());
        
        long startTime = System.currentTimeMillis();
        
        List<Order> compatibleOrders = filterCompatibleOrders(orders);
        
        if (compatibleOrders.isEmpty()) {
            logger.warn("No compatible orders found for truck {}", truck.getId());
            return createEmptyResponse(truck);
        }
        
        int bestMask = findOptimalCombination(truck, compatibleOrders);
        
        OptimizationResponse response = buildResponse(truck, compatibleOrders, bestMask);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Optimization completed in {}ms. Selected {} orders with total payout ${}", 
                duration, response.getSelectedOrderIds().size(), response.getTotalPayoutCents() / 100.0);
        
        return response;
    }
    
    private void validateRequest(OptimizationRequest request) {
        if (request.getOrders().size() > MAX_ORDERS) {
            throw new InvalidRequestException(
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
    
    private int findOptimalCombination(Truck truck, List<Order> orders) {
        int n = orders.size();
        int totalStates = 1 << n;
        
        long[] dpPayout = new long[totalStates];
        boolean[] dpValid = new boolean[totalStates];
        
        dpValid[0] = true;
        
        for (int mask = 0; mask < totalStates; mask++) {
            if (!dpValid[mask]) {
                continue;
            }
            
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    continue;
                }
                
                int newMask = mask | (1 << i);
                
                if (isValidCombination(truck, orders, newMask)) {
                    long newPayout = dpPayout[mask] + orders.get(i).getPayoutCents();
                    
                    if (!dpValid[newMask] || newPayout > dpPayout[newMask]) {
                        dpValid[newMask] = true;
                        dpPayout[newMask] = newPayout;
                    }
                }
            }
        }
        
        int bestMask = 0;
        long bestPayout = 0;
        
        for (int mask = 0; mask < totalStates; mask++) {
            if (dpValid[mask] && dpPayout[mask] > bestPayout) {
                bestPayout = dpPayout[mask];
                bestMask = mask;
            }
        }
        
        return bestMask;
    }
    
    private boolean isValidCombination(Truck truck, List<Order> orders, int mask) {
        int totalWeight = 0;
        int totalVolume = 0;
        boolean hasHazmat = false;
        
        for (int i = 0; i < orders.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                Order order = orders.get(i);
                totalWeight += order.getWeightLbs();
                totalVolume += order.getVolumeCuft();
                
                if (order.getIsHazmat()) {
                    if (hasHazmat) {
                        return false;
                    }
                    hasHazmat = true;
                }
            }
        }
        
        return totalWeight <= truck.getMaxWeightLbs() && 
               totalVolume <= truck.getMaxVolumeCuft();
    }
    
    private OptimizationResponse buildResponse(Truck truck, List<Order> orders, int mask) {
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
                truck.getId(),
                selectedIds,
                totalPayout,
                totalWeight,
                totalVolume,
                truck.getMaxWeightLbs(),
                truck.getMaxVolumeCuft()
        );
    }
    
    private OptimizationResponse createEmptyResponse(Truck truck) {
        return OptimizationResponse.fromSelection(
                truck.getId(),
                new ArrayList<>(),
                0L,
                0,
                0,
                truck.getMaxWeightLbs(),
                truck.getMaxVolumeCuft()
        );
    }
}
