package com.blackrock.challenge;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import com.blackrock.challenge.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void testMultipleOf100() {
        ParseRequest req = new ParseRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1500)));
        ParseResponse resp = transactionService.parse(req);

        Transaction t = resp.getTransactions().get(0);
        assertEquals(1500, t.getCeiling());
        assertEquals(0, t.getRemanent());
    }

    @Test
    void testZeroAmount() {
        ParseRequest req = new ParseRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 0)));
        ParseResponse resp = transactionService.parse(req);

        Transaction t = resp.getTransactions().get(0);
        assertEquals(0, t.getCeiling());
        assertEquals(0, t.getRemanent());
    }

    @Test
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
    void testValidExpense() {
        ValidatorRequest req = new ValidatorRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);

        assertEquals(1, resp.getValid().size());
        assertEquals(0, resp.getInvalid().size());
    }

    @Test
    void testInvalidAmount() {
        ValidatorRequest req = new ValidatorRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 500000)));
        req.setWage(1000000);
        ValidatorResponse resp = transactionService.validate(req);

        assertEquals(0, resp.getValid().size());
        assertEquals(1, resp.getInvalid().size());
    }

    @Test
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
    void testFilterNoPeriodsNoK() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(
                new Expense("2021-10-01 20:15:00", 1519),
                new Expense("2021-10-02 10:00:00", 250)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // 1519 -> ceil 1600, rem 81
        // 250 -> ceil 300, rem 50
        // k sum = 131
        assertEquals(1, resp.getSavingsByDates().size());
        assertEquals(131, resp.getSavingsByDates().get(0));
        assertEquals(131, resp.getTotalSavings());
    }

    @Test
    void testQPeriodOverride() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(List.of(new QPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 50)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // q replaces remanent with 50
        assertEquals(50, resp.getSavingsByDates().get(0));
    }

    @Test
    void testQPeriodLatestStartWins() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-15 12:00:00", 1519)));
        req.setQ(List.of(
                new QPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 50),
                new QPeriod("2021-10-10 00:00:00", "2021-10-20 23:59:59", 75)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // Second q period has later start (Oct 10 > Oct 1), so fixed=75 wins
        assertEquals(75, resp.getSavingsByDates().get(0));
    }

    @Test
    void testQPeriodSameStartFirstInList() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-15 12:00:00", 1519)));
        req.setQ(List.of(
                new QPeriod("2021-10-10 00:00:00", "2021-10-20 23:59:59", 50),
                new QPeriod("2021-10-10 00:00:00", "2021-10-25 23:59:59", 75)));
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // Same start date → first in list wins → fixed=50
        assertEquals(50, resp.getSavingsByDates().get(0));
    }

    @Test
    void testPPeriodAddition() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(List.of(new PPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 20)));
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // remanent=81 + extra=20 = 101
        assertEquals(101, resp.getSavingsByDates().get(0));
    }

    @Test
    void testMultiplePPeriodsStack() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(List.of(
                new PPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 20),
                new PPeriod("2021-10-01 00:00:00", "2021-10-15 23:59:59", 10)));
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // remanent=81 + 20 + 10 = 111
        assertEquals(111, resp.getSavingsByDates().get(0));
    }

    @Test
    void testQThenPCombined() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-01 20:15:00", 1519)));
        req.setQ(List.of(new QPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 50)));
        req.setP(List.of(new PPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59", 20)));
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // q replaces remanent with 50, then p adds 20 → 70
        assertEquals(70, resp.getSavingsByDates().get(0));
    }

    @Test
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
        assertEquals(81, resp.getSavingsByDates().get(0)); // Oct: 1519 → rem 81
        assertEquals(50, resp.getSavingsByDates().get(1)); // Nov: 250 → rem 50
        assertEquals(131, resp.getTotalSavings());
    }

    @Test
    void testOverlappingKPeriods() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(List.of(new Expense("2021-10-15 12:00:00", 1519)));
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(
                new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59"),
                new KPeriod("2021-10-10 00:00:00", "2021-10-20 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        // Transaction falls in both k periods
        assertEquals(2, resp.getSavingsByDates().size());
        assertEquals(81, resp.getSavingsByDates().get(0));
        assertEquals(81, resp.getSavingsByDates().get(1));
        assertEquals(162, resp.getTotalSavings());
    }

    @Test
    void testEmptyExpenses() {
        FilterRequest req = new FilterRequest();
        req.setExpenses(Collections.emptyList());
        req.setQ(Collections.emptyList());
        req.setP(Collections.emptyList());
        req.setK(List.of(new KPeriod("2021-10-01 00:00:00", "2021-10-31 23:59:59")));

        FilterResponse resp = transactionService.filter(req);

        assertEquals(1, resp.getSavingsByDates().size());
        assertEquals(0, resp.getSavingsByDates().get(0));
        assertEquals(0, resp.getTotalSavings());
    }

    // ========== TAX TESTS ==========

    @Test
    void testTaxBelowThreshold() {
        assertEquals(0.0, taxService.calculateTax(500000));
        assertEquals(0.0, taxService.calculateTax(700000));
    }

    @Test
    void testTaxFirstSlab() {
        // 800000: (800000 - 700000) * 0.10 = 10000
        assertEquals(10000.0, taxService.calculateTax(800000));
    }

    @Test
    void testTaxMultipleSlabs() {
        // 1200000: (1000000-700000)*0.10 + (1200000-1000000)*0.15
        // = 30000 + 30000 = 60000
        assertEquals(60000.0, taxService.calculateTax(1200000));
    }

    @Test
    void testTaxAbove15L() {
        // 2000000: (1000000-700000)*0.10 + (1200000-1000000)*0.15 +
        // (1500000-1200000)*0.20 + (2000000-1500000)*0.30
        // = 30000 + 30000 + 60000 + 150000 = 270000
        assertEquals(270000.0, taxService.calculateTax(2000000));
    }

    @Test
    void testNPSTaxBenefit() {
        // wage = 1500000, invested = 100000
        // eligible = min(100000, 150000, 200000) = 100000
        // benefit = Tax(1500000) - Tax(1400000)
        double benefit = taxService.calculateNPSTaxBenefit(100000, 1500000);
        assertTrue(benefit > 0);
    }

    // ========== RETURNS TESTS ==========

    @Test
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
