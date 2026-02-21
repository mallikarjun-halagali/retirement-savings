package com.blackrock.challenge.controller;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import com.blackrock.challenge.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Accepts a plain JSON array of expenses, returns enriched transactions.
     */
    @PostMapping("/transactions:parse")
    public ResponseEntity<List<Transaction>> parseTransactions(@RequestBody List<Expense> expenses) {
        return ResponseEntity.ok(transactionService.parseList(expenses));
    }

    /**
     * POST /transactions:validator
     * Validates transactions against constraints.
     */
    @PostMapping("/transactions:validator")
    public ResponseEntity<ValidatorResponse> validateTransactions(@RequestBody ValidatorRequest request) {
        return ResponseEntity.ok(transactionService.validate(request));
    }

    /**
     * POST /transactions:filter
     * Validates transactions according to q, p, k period rules.
     * Returns valid (with inKPeriod) and invalid (with message).
     */
    @PostMapping("/transactions:filter")
    public ResponseEntity<FilterResponse> filterTransactions(@RequestBody FilterRequest request) {
        return ResponseEntity.ok(transactionService.filter(request));
    }

    /**
     * POST /returns:nps
     * Calculates NPS returns with tax benefit, grouped by k-periods.
     */
    @PostMapping("/returns:nps")
    public ResponseEntity<ReturnsResponse> calculateNPS(@RequestBody FilterRequest request) {
        return ResponseEntity.ok(returnsService.calculateNPS(request));
    }

    /**
     * POST /returns:index
     * Calculates Index Fund returns (no tax benefit), grouped by k-periods.
     */
    @PostMapping("/returns:index")
    public ResponseEntity<ReturnsResponse> calculateIndex(@RequestBody FilterRequest request) {
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
