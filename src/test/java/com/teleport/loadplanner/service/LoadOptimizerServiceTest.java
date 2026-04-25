package com.teleport.loadplanner.service;

import com.teleport.loadplanner.exception.InvalidRequestException;
import com.teleport.loadplanner.exception.PayloadTooLargeException;
import com.teleport.loadplanner.model.OptimizationMode;
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
                createOrder("ord-003", 320000L, 30000, 1800, true)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        assertNotNull(response);
        assertEquals("truck-123", response.getTruckId());
        assertEquals(2, response.getSelectedOrderIds().size());
        assertTrue(response.getSelectedOrderIds().contains("ord-001"));
        assertTrue(response.getSelectedOrderIds().contains("ord-002"));
        assertEquals(430000L, response.getTotalPayoutCents());
        assertEquals(30000, response.getTotalWeightLbs());
        assertEquals(2100, response.getTotalVolumeCuft());
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
    void optimize_hazmat_isolation() {
        Truck truck = new Truck("truck-hazmat", 50000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", 200000L, 10000, 1000, true),
                createOrder("ord-002", 250000L, 12000, 1200, true),
                createOrder("ord-003", 100000L, 5000, 500, false)
        );
        
        OptimizationRequest request = new OptimizationRequest(truck, orders);
        
        OptimizationResponse response = cut.optimize(request);
        
        boolean hasHazmat = response.getSelectedOrderIds().stream()
                .anyMatch(id -> id.equals("ord-001") || id.equals("ord-002"));
        boolean hasNonHazmat = response.getSelectedOrderIds().contains("ord-003");
        
        assertFalse(hasHazmat && hasNonHazmat, "Hazmat and non-hazmat orders must not be combined");
        
        long hazmatCount = response.getSelectedOrderIds().stream()
                .filter(id -> id.equals("ord-001") || id.equals("ord-002"))
                .count();
        assertTrue(hazmatCount <= 1, "At most one hazmat order allowed");
        
        assertEquals("ord-002", response.getSelectedOrderIds().get(0));
        assertEquals(250000L, response.getTotalPayoutCents());
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
        
        assertThrows(PayloadTooLargeException.class, () -> cut.optimize(request));
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
    
    @Test
    void optimize_utilization_mode_prefers_higher_fill() {
        Truck truck = new Truck("truck-util", 44000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-high-pay", 500000L, 5000, 300, false),
                createOrder("ord-high-util", 100000L, 40000, 2800, false)
        );

        OptimizationRequest request = new OptimizationRequest(truck, orders, OptimizationMode.UTILIZATION);

        OptimizationResponse response = cut.optimize(request);

        assertTrue(response.getSelectedOrderIds().contains("ord-high-util"),
                "UTILIZATION mode should prefer the order that fills the truck more");
    }

    @Test
    void optimize_balanced_mode_considers_both() {
        Truck truck = new Truck("truck-bal", 44000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", 250000L, 18000, 1200, false),
                createOrder("ord-002", 180000L, 12000, 900, false),
                createOrder("ord-003", 320000L, 30000, 1800, true)
        );

        OptimizationRequest request = new OptimizationRequest(truck, orders, OptimizationMode.BALANCED);

        OptimizationResponse response = cut.optimize(request);

        assertNotNull(response);
        assertTrue(response.getTotalPayoutCents() > 0);
        assertTrue(response.getTotalWeightLbs() <= 44000);
        assertTrue(response.getTotalVolumeCuft() <= 3000);
    }

    @Test
    void optimize_pareto_solutions_present() {
        Truck truck = new Truck("truck-pareto", 44000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", 250000L, 18000, 1200, false),
                createOrder("ord-002", 180000L, 12000, 900, false),
                createOrder("ord-003", 50000L, 40000, 2800, false)
        );

        OptimizationRequest request = new OptimizationRequest(truck, orders);

        OptimizationResponse response = cut.optimize(request);

        assertNotNull(response.getParetoSolutions());
        assertFalse(response.getParetoSolutions().isEmpty());
    }

    @Test
    void optimize_time_window_conflict_excluded() {
        Truck truck = new Truck("truck-tw", 50000, 5000);
        List<Order> orders = List.of(
                createOrder("ord-early", 200000L, 10000, 1000, false,
                        LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 5)),
                createOrder("ord-late", 300000L, 12000, 1200, false,
                        LocalDate.of(2025, 12, 10), LocalDate.of(2025, 12, 15)),
                createOrder("ord-overlap", 150000L, 8000, 800, false,
                        LocalDate.of(2025, 12, 3), LocalDate.of(2025, 12, 12))
        );

        OptimizationRequest request = new OptimizationRequest(truck, orders);
        OptimizationResponse response = cut.optimize(request);

        assertFalse(response.getSelectedOrderIds().contains("ord-early")
                        && response.getSelectedOrderIds().contains("ord-late"),
                "Orders with non-overlapping time windows must not be combined");
        assertTrue(response.getTotalWeightLbs() <= 50000);
    }

    @Test
    void optimize_default_mode_is_revenue() {
        Truck truck = new Truck("truck-default", 44000, 3000);
        List<Order> orders = List.of(
                createOrder("ord-001", 300000L, 10000, 800, false),
                createOrder("ord-002", 100000L, 38000, 2500, false)
        );

        OptimizationRequest requestDefault = new OptimizationRequest(truck, orders);
        OptimizationRequest requestRevenue = new OptimizationRequest(truck, orders, OptimizationMode.REVENUE);

        OptimizationResponse r1 = cut.optimize(requestDefault);
        OptimizationResponse r2 = cut.optimize(requestRevenue);

        assertEquals(r1.getSelectedOrderIds(), r2.getSelectedOrderIds());
        assertEquals(r1.getTotalPayoutCents(), r2.getTotalPayoutCents());
    }

    private Order createOrder(String id, long payout, int weight, int volume, boolean hazmat) {
        return createOrder(id, "Los Angeles, CA", "Dallas, TX", payout, weight, volume, hazmat);
    }

    private Order createOrder(String id, long payout, int weight, int volume, boolean hazmat,
                              LocalDate pickup, LocalDate delivery) {
        return new Order(id, payout, weight, volume, "Los Angeles, CA", "Dallas, TX", pickup, delivery, hazmat);
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
