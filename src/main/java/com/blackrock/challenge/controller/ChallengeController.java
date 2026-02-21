package com.blackrock.challenge.controller;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blackrock/challenge/v1")
public class ChallengeController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ReturnsService returnsService;

    @Autowired
    private PerformanceService performanceService;

    /**
     * POST /transactions:parse
     * Enrich expenses with ceiling and remanent fields.
     */
    @PostMapping("/transactions:parse")
    public ResponseEntity<ParseResponse> parseTransactions(@RequestBody ParseRequest request) {
        return ResponseEntity.ok(transactionService.parse(request));
    }

    /**
     * POST /transactions:validator
     * Validate transactions against constraints.
     */
    @PostMapping("/transactions:validator")
    public ResponseEntity<ValidatorResponse> validateTransactions(@RequestBody ValidatorRequest request) {
        return ResponseEntity.ok(transactionService.validate(request));
    }

    /**
     * POST /transactions:filter
     * Apply q, p, k temporal constraints and calculate savings.
     */
    @PostMapping("/transactions:filter")
    public ResponseEntity<FilterResponse> filterTransactions(@RequestBody FilterRequest request) {
        return ResponseEntity.ok(transactionService.filter(request));
    }

    /**
     * POST /returns:nps
     * Calculate NPS returns with tax benefit.
     */
    @PostMapping("/returns:nps")
    public ResponseEntity<NpsResponse> calculateNPS(@RequestBody NpsRequest request) {
        return ResponseEntity.ok(returnsService.calculateNPS(request));
    }

    /**
     * POST /returns:index
     * Calculate Index Fund (NIFTY 50) returns.
     */
    @PostMapping("/returns:index")
    public ResponseEntity<IndexResponse> calculateIndex(@RequestBody IndexRequest request) {
        return ResponseEntity.ok(returnsService.calculateIndex(request));
    }

    /**
     * GET /performance
     * System performance metrics.
     */
    @GetMapping("/performance")
    public ResponseEntity<PerformanceResponse> getPerformance() {
        return ResponseEntity.ok(performanceService.getPerformance());
    }
}
