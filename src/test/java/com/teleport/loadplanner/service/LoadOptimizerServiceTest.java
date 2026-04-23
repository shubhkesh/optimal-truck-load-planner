package com.teleport.loadplanner.service;

import com.teleport.loadplanner.exception.InvalidRequestException;
import com.teleport.loadplanner.model.OptimizationRequest;
import com.teleport.loadplanner.model.OptimizationResponse;
import com.teleport.loadplanner.model.Order;
import com.teleport.loadplanner.model.Truck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoadOptimizerServiceTest {
    
    private LoadOptimizerService cut;
    
    @BeforeEach
    void setUp() {
        cut = new LoadOptimizerService();
    }
    
    @Test
    void optimize() {
        Truck truck = new Truck("truck-123", 44000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", 250000L, 18000, 1200, false),
                createOrder("ord-002", 180000L, 12000, 900, false),
                createOrder("ord-003", 320000L, 30000, 1800, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        assertEquals(2, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-002"));
        assertTrue(response.getSelectedOrderIds().contains("ord-003"));
        assertEquals(500000L, response.getTotalPayoutCents());
        assertEquals(42000, response.getTotalWeightLbs());
        assertEquals(2700, response.getTotalVolumeCuft());
        assertTrue(response.getTotalWeightLbs() <= 44000);
        assertTrue(response.getTotalVolumeCuft() <= 3000);
    }
    
    @Test
    void optimize_single_order_fits() {
        Truck truck = new Truck("truck-456", 20000, 1500);
        List<Order> orders = List.of(
                createOrder("ord-001", 150000L, 15000, 1000, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertEquals(1, response.getSelectedOrderIds().size());
        assertEquals("ord-001", response.getSelectedOrderIds().get(0));
        assertEquals(150000L, response.getTotalPayoutCents());
    }
    
    @Test
    void optimize_no_orders_fit() {
        Truck truck = new Truck("truck-789", 10000, 500);
        List<Order> orders = List.of(
                createOrder("ord-001", 250000L, 20000, 1200, false),
                createOrder("ord-002", 180000L, 15000, 900, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertEquals(0, response.getSelectedOrderIds().size());
        assertEquals(0L, response.getTotalPayoutCents());
    }
    
    @Test
    void optimize_weight_constraint() {
        Truck truck = new Truck("truck-weight", 25000, 5000);
        List<Order> orders = List.of(
                createOrder("ord-001", 200000L, 15000, 1000, false),
                createOrder("ord-002", 150000L, 12000, 800, false),
                createOrder("ord-003", 100000L, 8000, 600, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertTrue(response.getTotalWeightLbs() <= 25000);
        assertEquals(2, response.getSelectedOrderIds().size());
    }
    
    @Test
    void optimize_volume_constraint() {
        Truck truck = new Truck("truck-volume", 50000, 2000);
        List<Order> orders = List.of(
                createOrder("ord-001", 200000L, 10000, 1200, false),
                createOrder("ord-002", 150000L, 8000, 1000, false),
                createOrder("ord-003", 100000L, 5000, 600, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertTrue(response.getTotalVolumeCuft() <= 2000);
    }
    
    @Test
    void optimize_hazmat_only_one_allowed() {
        Truck truck = new Truck("truck-hazmat", 50000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", 200000L, 10000, 1000, true),
                createOrder("ord-002", 250000L, 12000, 1200, true),
                createOrder("ord-003", 100000L, 5000, 500, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        long hazmatCount = response.getSelectedOrderIds().stream()
                .filter(id -> id.equals("ord-001") || id.equals("ord-002"))
                .count();
        
        assertTrue(hazmatCount <= 1);
    }
    
    @Test
    void optimize_incompatible_routes_filtered() {
        Truck truck = new Truck("truck-route", 50000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", "LA", "Dallas", 200000L, 10000, 1000, false),
                createOrder("ord-002", "LA", "Dallas", 150000L, 8000, 800, false),
                createOrder("ord-003", "LA", "Phoenix", 180000L, 9000, 900, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertFalse(response.getSelectedOrderIds().contains("ord-003"));
    }
    
    @Test
    void optimize_empty_orders_list() {
        Truck truck = new Truck("truck-empty", 44000, 3000);
        List<Order> orders = new ArrayList<>();
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertEquals(0, response.getSelectedOrderIds().size());
        assertEquals(0L, response.getTotalPayoutCents());
    }
    
    @Test
    void optimize_too_many_orders() {
        Truck truck = new Truck("truck-many", 100000, 10000);
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            orders.add(createOrder("ord-" + i, 100000L, 1000, 100, false));
        }
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        assertThrows(InvalidRequestException.class, () -> cut.optimize(request));
    }
    
    @Test
    void optimize_invalid_dates() {
        Truck truck = new Truck("truck-dates", 44000, 3000);
        Order invalidOrder = new Order(
                "ord-001",
                100000L,
                10000,
                1000,
                "Los Angeles, CA",
                "Dallas, TX",
                LocalDate.of(2025, 12, 10),
                LocalDate.of(2025, 12, 5),
                false
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, List.of(invalidOrder));
        
        assertThrows(InvalidRequestException.class, () -> cut.optimize(request));
    }
    
    @Test
    void optimize_maximizes_payout() {
        Truck truck = new Truck("truck-max", 30000, 2500);
        List<Order> orders = List.of(
                createOrder("ord-low", 100000L, 10000, 800, false),
                createOrder("ord-high", 300000L, 15000, 1200, false),
                createOrder("ord-medium", 150000L, 12000, 900, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertTrue(response.getSelectedOrderIds().contains("ord-high"));
    }
    
    @Test
    void optimize_zero_capacity_truck() {
        Truck truck = new Truck("truck-zero", 0, 0);
        List<Order> orders = List.of(
                createOrder("ord-001", 100000L, 1000, 100, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertEquals(0, response.getSelectedOrderIds().size());
    }
    
    @Test
    void optimize_exact_capacity_match() {
        Truck truck = new Truck("truck-exact", 20000, 1500);
        List<Order> orders = List.of(
                createOrder("ord-001", 150000L, 10000, 750, false),
                createOrder("ord-002", 200000L, 10000, 750, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertEquals(2, response.getSelectedOrderIds().size());
        assertEquals(20000, response.getTotalWeightLbs());
        assertEquals(1500, response.getTotalVolumeCuft());
    }
    
    private Order createOrder(String id, long payout, int weight, int volume, boolean hazmat) {
        return createOrder(id, "Los Angeles, CA", "Dallas, TX", payout, weight, volume, hazmat);
    }
    
    private Order createOrder(String id, String origin, String destination, 
                            long payout, int weight, int volume, boolean hazmat) {
        return new Order(
                id,
                payout,
                weight,
                volume,
                origin,
                destination,
                LocalDate.of(2025, 12, 5),
                LocalDate.of(2025, 12, 10),
                hazmat
        );
    }
}
