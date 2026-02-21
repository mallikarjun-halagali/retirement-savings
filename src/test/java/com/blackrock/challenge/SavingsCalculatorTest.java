package com.blackrock.challenge;

/*
 * ============================================================
 * TEST METADATA
 * ============================================================
 * Test Type: Unit Tests
 * Validation: Core business logic validation for the retirement
 *             micro-savings system including:
 *             - Expense rounding (ceiling/remanent calculation)
 *             - Transaction validation (amount, date, duplicates)
 *             - Filter: q/p/k period rules with inKPeriod flag
 *             - Returns: NPS and Index savings per k-period
 *             - Indian tax slab calculation
 * Command: mvn test
 * ============================================================
 */

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import com.blackrock.challenge.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SavingsCalculatorTest {

    private TransactionService transactionService;
    private TaxService taxService;
    private ReturnsService returnsService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        taxService = new TaxService();
        returnsService = new ReturnsService();
    }

    // ========== PARSE TESTS ==========

    @Test
    @DisplayName("Parse: 1519 → ceiling 1600, remanent 81")
    void testBasicRounding() {
        List<Transaction> result = transactionService.parseList(
                List.of(new Expense("2021-10-01 20:15:00", 1519)));
        assertEquals(1600, result.get(0).getCeiling());
        assertEquals(81, result.get(0).getRemanent());
    }

    @Test
    @DisplayName("Parse: 1500 (multiple of 100) → remanent 0")
    void testMultipleOf100() {
        List<Transaction> result = transactionService.parseList(
                List.of(new Expense("2021-10-01 20:15:00", 1500)));
        assertEquals(1500, result.get(0).getCeiling());
        assertEquals(0, result.get(0).getRemanent());
    }

    @Test
    @DisplayName("Parse: 0 → ceiling 0, remanent 0")
    void testZeroAmount() {
        List<Transaction> result = transactionService.parseList(
                List.of(new Expense("2021-10-01 20:15:00", 0)));
        assertEquals(0, result.get(0).getCeiling());
        assertEquals(0, result.get(0).getRemanent());
    }

    @Test
    @DisplayName("Parse: 1 → ceiling 100, remanent 99")
    void testSmallAmount() {
        List<Transaction> result = transactionService.parseList(
                List.of(new Expense("2021-10-01 20:15:00", 1)));
        assertEquals(100, result.get(0).getCeiling());
        assertEquals(99, result.get(0).getRemanent());
    }

    // ========== VALIDATOR TESTS ==========

    @Test
    @DisplayName("Validator: valid transaction passes")
    void testValidExpense() {
        ValidatorRequest req = new ValidatorRequest();
        req.setTransactions(List.of(new Transaction("2021-10-01 20:15:00", 1519, 1600, 81)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);
        assertEquals(1, resp.getValid().size());
        assertEquals(0, resp.getInvalid().size());
    }

    @Test
    @DisplayName("Validator: negative amount returns message")
    void testNegativeAmount() {
        ValidatorRequest req = new ValidatorRequest();
        req.setTransactions(List.of(new Transaction("2021-10-01 20:15:00", -250, 200, 30)));
        req.setWage(50000);
        ValidatorResponse resp = transactionService.validate(req);
        assertEquals(0, resp.getValid().size());
        assertEquals("Negative amounts are not allowed", resp.getInvalid().get(0).getMessage());
    }

    @Test
    @DisplayName("Validator: duplicate dates → second invalid")
    void testDuplicateDates() {
        ValidatorRequest req = new ValidatorRequest();
        req.setTransactions(List.of(
                new Transaction("2021-10-01 20:15:00", 100, 100, 0),
                new Transaction("2021-10-01 20:15:00", 200, 200, 0)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);
        assertEquals(1, resp.getValid().size());
        assertEquals("Duplicate transaction date", resp.getInvalid().get(0).getMessage());
    }

    @Test
    @DisplayName("Validator: amount >= 500000 invalid")
    void testInvalidAmount() {
        ValidatorRequest req = new ValidatorRequest();
        req.setTransactions(List.of(new Transaction("2021-10-01 20:15:00", 500000, 500000, 0)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);
        assertEquals(0, resp.getValid().size());
        assertEquals(1, resp.getInvalid().size());
    }

    // ========== FILTER TESTS ==========

    @Test
    @DisplayName("Filter: basic enrichment with inKPeriod")
    void testFilterBasic() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));
        FilterResponse resp = transactionService.filter(req);
        assertEquals(1, resp.getValid().size());
        assertEquals(81.0, resp.getValid().get(0).getRemanent());
        assertTrue(resp.getValid().get(0).isInKPeriod());
    }

    @Test
    @DisplayName("Filter: q-period fixed=0 excludes transaction")
    void testFilterQExcludes() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2023-07-15 10:30:00", 620)));
        req.setQ(List.of(new QPeriod("2023-07-01 00:00:00", "2023-07-31 23:59:59", 0)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
        FilterResponse resp = transactionService.filter(req);
        assertEquals(0, resp.getValid().size());
    }

    @Test
    @DisplayName("Filter: p-period adds extra")
    void testFilterPAdds() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2023-10-12 20:15:30", 250)));
        req.setQ(Collections.emptyList());
        req.setP(List.of(new PPeriod("2023-10-01 00:00:00", "2023-12-31 23:59:59", 30)));
        req.setK(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
        FilterResponse resp = transactionService.filter(req);
        assertEquals(80.0, resp.getValid().get(0).getRemanent());
    }

    @Test
    @DisplayName("Filter: full sample matches expected output")
    void testFilterFullSample() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(
                new Expense("2023-02-28 15:49:20", 375),
                new Expense("2023-07-15 10:30:00", 620),
                new Expense("2023-10-12 20:15:30", 250),
                new Expense("2023-10-12 20:15:30", 250),
                new Expense("2023-12-17 08:09:45", -480)));
        req.setQ(List.of(new QPeriod("2023-07-01 00:00:00", "2023-07-31 23:59:59", 0)));
        req.setP(List.of(new PPeriod("2023-10-01 00:00:00", "2023-12-31 23:59:59", 30)));
        req.setK(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
        FilterResponse resp = transactionService.filter(req);

        assertEquals(2, resp.getValid().size());
        assertEquals(25.0, resp.getValid().get(0).getRemanent());
        assertEquals(80.0, resp.getValid().get(1).getRemanent());
        assertEquals(2, resp.getInvalid().size());
    }

    // ========== RETURNS TESTS ==========

    @Test
    @DisplayName("NPS: full sample matches expected output")
    void testNPSFullSample() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(
                new Expense("2023-02-28 15:49:20", 375),
                new Expense("2023-07-01 21:59:00", 620),
                new Expense("2023-10-12 20:15:30", 250),
                new Expense("2023-12-17 08:09:45", 480),
                new Expense("2023-12-17 08:09:45", -10)));
        req.setQ(List.of(new QPeriod("2023-07-01 00:00:00", "2023-07-31 23:59:59", 0)));
        req.setP(List.of(new PPeriod("2023-10-01 08:00:00", "2023-12-31 19:59:59", 25)));
        req.setK(List.of(
                new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59"),
                new KPeriod("2023-03-01 00:00:00", "2023-11-31 23:59:59")));
        req.setAge(29);
        req.setWage(50000);
        req.setInflation(5.5);

        ReturnsResponse resp = returnsService.calculateNPS(req);

        assertEquals(1725.0, resp.getTotalTransactionAmount());
        assertEquals(1900.0, resp.getTotalCeiling());
        assertEquals(2, resp.getSavingsByDates().size());
        assertEquals(145.0, resp.getSavingsByDates().get(0).getAmount());
        assertEquals(86.88, resp.getSavingsByDates().get(0).getProfit());
        assertEquals(0.0, resp.getSavingsByDates().get(0).getTaxBenefit());
        assertEquals(75.0, resp.getSavingsByDates().get(1).getAmount());
        assertEquals(44.94, resp.getSavingsByDates().get(1).getProfit());
    }

    @Test
    @DisplayName("Index: taxBenefit is always 0")
    void testIndexNoTaxBenefit() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2023-02-28 15:49:20", 375)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2023-01-01 00:00:00", "2023-12-31 23:59:59")));
        req.setAge(29);
        req.setWage(50000);
        req.setInflation(5.5);

        ReturnsResponse resp = returnsService.calculateIndex(req);

        assertEquals(1, resp.getSavingsByDates().size());
        assertEquals(0.0, resp.getSavingsByDates().get(0).getTaxBenefit());
        assertTrue(resp.getSavingsByDates().get(0).getProfit() > 0);
    }

    // ========== TAX TESTS ==========

    @Test
    @DisplayName("Tax: income <= 7L → zero")
    void testTaxBelowThreshold() {
        assertEquals(0.0, taxService.calculateTax(500000));
        assertEquals(0.0, taxService.calculateTax(700000));
    }

    @Test
    @DisplayName("Tax: 8L → 10000")
    void testTaxFirstSlab() {
        assertEquals(10000.0, taxService.calculateTax(800000));
    }

    @Test
    @DisplayName("Tax: 12L → 60000")
    void testTaxMultipleSlabs() {
        assertEquals(60000.0, taxService.calculateTax(1200000));
    }

    @Test
    @DisplayName("Tax: 20L → 270000")
    void testTaxAbove15L() {
        assertEquals(270000.0, taxService.calculateTax(2000000));
    }

    @Test
    @DisplayName("Tax: NPS benefit for 15L wage > 0")
    void testNPSTaxBenefit() {
        double benefit = taxService.calculateNPSTaxBenefit(100000, 1500000);
        assertTrue(benefit > 0);
    }
}
