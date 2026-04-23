package com.teleport.loadplanner.controller;

import com.teleport.loadplanner.model.OptimizationRequest;
import com.teleport.loadplanner.model.OptimizationResponse;
import com.teleport.loadplanner.service.LoadOptimizerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/load-optimizer")
public class LoadOptimizerController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadOptimizerController.class);
    
    private final LoadOptimizerService optimizerService;
    
    public LoadOptimizerController(LoadOptimizerService optimizerService) {
        this.optimizerService = optimizerService;
    }
    
    @PostMapping("/optimize")
    public ResponseEntity<OptimizationResponse> optimize(@Valid @RequestBody OptimizationRequest request) {
        logger.info("Received optimization request for truck: {}", request.getTruck().getId());
        
        OptimizationResponse response = optimizerService.optimize(request);
        
        return ResponseEntity.ok(response);
    }
}
