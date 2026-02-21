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
 *             - q-period override rules (latest start wins)
 *             - p-period addition rules (all extras stack)
 *             - k-period grouping (overlapping ranges)
 *             - Indian tax slab calculation
 *             - NPS tax benefit calculation
 *             - NPS & Index Fund compound interest returns
 * Command: mvn test
 *          OR: docker run --rm blk-hacking-ind-mallikarjun-halagali mvn test
 * ============================================================
 */

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import com.blackrock.challenge.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
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
        // Inject taxService into returnsService via reflection for unit test
        try {
            var field = ReturnsService.class.getDeclaredField("taxService");
            field.setAccessible(true);
            field.set(returnsService, taxService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== PARSE TESTS ==========

    @Test
    @DisplayName("Parse: 1519 → ceiling 1600, remanent 81")
    void testBasicRounding() {
        ParseRequest req = new ParseRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        ParseResponse resp = transactionService.parse(req);

        assertEquals(1, resp.getTransactions().size());
        Transaction t = resp.getTransactions().get(0);
        assertEquals(1600, t.getCeiling());
        assertEquals(81, t.getRemanent());
    }

    @Test
    @DisplayName("Parse: 1500 (multiple of 100) → remanent 0")
    void testMultipleOf100() {
        ParseRequest req = new ParseRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1500)));
        ParseResponse resp = transactionService.parse(req);

        Transaction t = resp.getTransactions().get(0);
        assertEquals(1500, t.getCeiling());
        assertEquals(0, t.getRemanent());
    }

    @Test
    @DisplayName("Parse: 0 → ceiling 0, remanent 0")
    void testZeroAmount() {
        ParseRequest req = new ParseRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 0)));
        ParseResponse resp = transactionService.parse(req);

        Transaction t = resp.getTransactions().get(0);
        assertEquals(0, t.getCeiling());
        assertEquals(0, t.getRemanent());
    }

    @Test
    @DisplayName("Parse: 1 → ceiling 100, remanent 99")
    void testSmallAmount() {
        ParseRequest req = new ParseRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1)));
        ParseResponse resp = transactionService.parse(req);

        Transaction t = resp.getTransactions().get(0);
        assertEquals(100, t.getCeiling());
        assertEquals(99, t.getRemanent());
    }

    // ========== VALIDATOR TESTS ==========

    @Test
    @DisplayName("Validator: valid expense passes all checks")
    void testValidExpense() {
        ValidatorRequest req = new ValidatorRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);

        assertEquals(1, resp.getValid().size());
        assertEquals(0, resp.getInvalid().size());
    }

    @Test
    @DisplayName("Validator: amount >= 500000 is invalid")
    void testInvalidAmount() {
        ValidatorRequest req = new ValidatorRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 500000)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);

        assertEquals(0, resp.getValid().size());
        assertEquals(1, resp.getInvalid().size());
    }

    @Test
    @DisplayName("Validator: duplicate dates → second is invalid")
    void testDuplicateDates() {
        ValidatorRequest req = new ValidatorRequest();
        req.setExpenses(List.of(
                new Expense("2021-10-01 20:15:00", 100),
                new Expense("2021-10-01 20:15:00", 200)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);

        assertEquals(1, resp.getValid().size());
        assertEquals(1, resp.getInvalid().size());
    }

    // ========== FILTER TESTS ==========

    @Test
    @DisplayName("Filter: no q/p periods, single k groups correctly")
    void testFilterNoPeriodsNoK() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(
                new Expense("2021-10-01 20:15:00", 1519),
                new Expense("2021-10-02 10:00:00", 250)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(1, resp.getSavingsByDates().size());
        assertEquals(131.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter q-period: remanent replaced with fixed amount")
    void testQPeriodOverride() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(List.of(new QPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 50)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(50.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter q-period: multiple overlap → latest start wins")
    void testQPeriodLatestStartWins() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-15 12:00:00", 1519)));
        req.setQ(List.of(
                new QPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 50),
                new QPeriod("2021-10-10 00:00:00", "2021-10-20 23:59:59", 75)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(75.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter q-period: same start date → first in list wins")
    void testQPeriodSameStartFirstInList() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-15 12:00:00", 1519)));
        req.setQ(List.of(
                new QPeriod("2021-10-10 00:00:00", "2021-10-20 23:59:59", 50),
                new QPeriod("2021-10-10 00:00:00", "2021-10-25 23:59:59", 75)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(50.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter p-period: extra added to remanent")
    void testPPeriodAddition() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(List.of(new PPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 20)));
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(101.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter p-period: multiple overlapping p-periods stack additively")
    void testMultiplePPeriodsStack() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(List.of(
                new PPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 20),
                new PPeriod("2021-10-01 00:00:00", "2021-10-15 23:59:59", 10)));
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(111.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter q+p combined: q replaces, then p adds on top")
    void testQThenPCombined() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(List.of(new QPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 50)));
        req.setP(List.of(new PPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 20)));
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(70.0, resp.getSavingsByDates().get(0).getAmount());
    }

    @Test
    @DisplayName("Filter k-period: multiple k ranges group independently")
    void testMultipleKPeriods() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(
                new Expense("2021-10-01 20:15:00", 1519),
                new Expense("2021-11-01 10:00:00", 250)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(
                new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59"),
                new KPeriod("2021-11-01 00:00:00", "2021-11-30 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(2, resp.getSavingsByDates().size());
        assertEquals(81.0, resp.getSavingsByDates().get(0).getAmount());
        assertEquals(50.0, resp.getSavingsByDates().get(1).getAmount());
    }

    @Test
    @DisplayName("Filter k-period: overlapping k ranges count same transaction in both")
    void testOverlappingKPeriods() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-15 12:00:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(
                new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59"),
                new KPeriod("2021-10-10 00:00:00", "2021-10-20 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(2, resp.getSavingsByDates().size());
        assertEquals(81.0, resp.getSavingsByDates().get(0).getAmount());
        assertEquals(81.0, resp.getSavingsByDates().get(1).getAmount());
    }

    @Test
    @DisplayName("Filter: empty expenses → all k-sums are 0")
    void testEmptyExpenses() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(Collections.emptyList());
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(1, resp.getSavingsByDates().size());
        assertEquals(0.0, resp.getSavingsByDates().get(0).getAmount());
    }

    // ========== TAX TESTS ==========

    @Test
    @DisplayName("Tax: income ≤ 7L → zero tax")
    void testTaxBelowThreshold() {
        assertEquals(0.0, taxService.calculateTax(500000));
        assertEquals(0.0, taxService.calculateTax(700000));
    }

    @Test
    @DisplayName("Tax: 8L → 10% on excess over 7L = ₹10,000")
    void testTaxFirstSlab() {
        assertEquals(10000.0, taxService.calculateTax(800000));
    }

    @Test
    @DisplayName("Tax: 12L → multiple slabs = ₹60,000")
    void testTaxMultipleSlabs() {
        assertEquals(60000.0, taxService.calculateTax(1200000));
    }

    @Test
    @DisplayName("Tax: 20L → all slabs up to 30% = ₹2,70,000")
    void testTaxAbove15L() {
        assertEquals(270000.0, taxService.calculateTax(2000000));
    }

    @Test
    @DisplayName("Tax: NPS benefit calculated correctly for 15L wage")
    void testNPSTaxBenefit() {
        double benefit = taxService.calculateNPSTaxBenefit(100000, 1500000);
        assertTrue(benefit > 0);
    }

    // ========== RETURNS TESTS ==========

    @Test
    @DisplayName("Returns NPS: compound interest + inflation adjustment + tax benefit")
    void testNPSReturns() {
        NpsRequest req = new NpsRequest();
        req.setInvested(10000);
        req.setWage(1500000);
        req.setAge(25);
        req.setInflation(0.06);
        NpsResponse resp = returnsService.calculateNPS(req);

        assertTrue(resp.getReturns() > resp.getInvested());
        assertTrue(resp.getProfit() > 0);
        assertTrue(resp.getInflationAdjusted() < resp.getReturns());
        assertTrue(resp.getTaxBenefit() >= 0);
    }

    @Test
    @DisplayName("Returns Index: compound interest + inflation adjustment, no tax benefit")
    void testIndexReturns() {
        IndexRequest req = new IndexRequest();
        req.setInvested(10000);
        req.setAge(25);
        req.setInflation(0.06);
        IndexResponse resp = returnsService.calculateIndex(req);

        assertTrue(resp.getReturns() > resp.getInvested());
        assertTrue(resp.getProfit() > 0);
        assertTrue(resp.getInflationAdjusted() < resp.getReturns());
    }
}
