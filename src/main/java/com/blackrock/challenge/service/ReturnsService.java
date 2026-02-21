package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReturnsService {

    private static final double NPS_RATE = 0.0711;
    private static final double INDEX_RATE = 0.1449;
    private static final int RETIREMENT_AGE = 60;

    private static final DateTimeFormatter STRICT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter LENIENT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.LENIENT);

    /**
     * Calculate NPS returns with tax benefit per k-period.
     */
    public ReturnsResponse calculateNPS(FilterRequest request) {
        return calculateReturns(request, NPS_RATE, true);
    }

    /**
     * Calculate Index Fund returns (no tax benefit) per k-period.
     */
    public ReturnsResponse calculateIndex(FilterRequest request) {
        return calculateReturns(request, INDEX_RATE, false);
    }

    /**
     * Core calculation:
     * 1. Validate transactions (skip negative, duplicate)
     * 2. Enrich valid ones (ceiling, remanent)
     * 3. Apply q rules (replace remanent with fixed)
     * 4. Apply p rules (add extra to remanent)
     * 5. Sum savings per k-period
     * 6. Calculate returns, profit, tax benefit per k-period
     */
    private ReturnsResponse calculateReturns(FilterRequest request, double rate, boolean includeTaxBenefit) {
        List<Expense> expenses = request.getExpenses();
        if (expenses == null)
            expenses = new ArrayList<>();

        List<QPeriod> qPeriods = request.getQ() != null ? request.getQ() : new ArrayList<>();
        List<PPeriod> pPeriods = request.getP() != null ? request.getP() : new ArrayList<>();
        List<KPeriod> kPeriods = request.getK() != null ? request.getK() : new ArrayList<>();

        int age = request.getAge();
        double wage = request.getWage();
        double inflation = request.getInflation();

        // Normalize inflation
        if (Math.abs(inflation) > 1.0) {
            inflation = inflation / 100.0;
        }

        int years = RETIREMENT_AGE - age;

        // Pre-parse period date ranges
        List<LocalDateTime[]> qRanges = parsePeriodRanges(qPeriods);
        List<LocalDateTime[]> pRanges = parsePeriodRangesP(pPeriods);
        List<LocalDateTime[]> kRanges = parsePeriodRangesK(kPeriods);

        // Step 1: Validate and process transactions
        Set<String> seenDates = new HashSet<>();
        List<Double> validAmounts = new ArrayList<>();
        List<Double> validCeilings = new ArrayList<>();
        List<Double> validRemanents = new ArrayList<>();
        List<LocalDateTime> validDates = new ArrayList<>();

        for (Expense expense : expenses) {
            double amount = expense.getAmount();
            String date = expense.getDate();

            // Skip negative amounts
            if (amount < 0)
                continue;
            // Skip amounts >= 500000
            if (amount >= 500000)
                continue;
            // Skip invalid dates
            if (date == null || date.isEmpty())
                continue;

            LocalDateTime expDate;
            try {
                expDate = LocalDateTime.parse(date, STRICT_FORMATTER);
            } catch (Exception e) {
                continue;
            }

            // Skip duplicate dates
            if (!seenDates.add(date))
                continue;

            // Compute ceiling and remanent
            double ceiling = computeCeiling(amount);
            double remanent = ceiling - amount;

            // Apply q-period rules (latest start wins)
            int bestQIndex = -1;
            LocalDateTime bestQStart = null;
            for (int j = 0; j < qPeriods.size(); j++) {
                LocalDateTime qStart = qRanges.get(j)[0];
                LocalDateTime qEnd = qRanges.get(j)[1];
                if (!expDate.isBefore(qStart) && !expDate.isAfter(qEnd)) {
                    if (bestQIndex == -1 || qStart.isAfter(bestQStart)) {
                        bestQIndex = j;
                        bestQStart = qStart;
                    }
                }
            }
            if (bestQIndex != -1) {
                remanent = qPeriods.get(bestQIndex).getFixed();
            }

            // Apply p-period rules (all extras stack)
            for (int j = 0; j < pPeriods.size(); j++) {
                LocalDateTime pStart = pRanges.get(j)[0];
                LocalDateTime pEnd = pRanges.get(j)[1];
                if (!expDate.isBefore(pStart) && !expDate.isAfter(pEnd)) {
                    remanent += pPeriods.get(j).getExtra();
                }
            }

            validAmounts.add(amount);
            validCeilings.add(ceiling);
            validRemanents.add(remanent);
            validDates.add(expDate);
        }

        // Calculate totals
        double totalTransactionAmount = validAmounts.stream().mapToDouble(Double::doubleValue).sum();
        double totalCeiling = validCeilings.stream().mapToDouble(Double::doubleValue).sum();

        // Step 5: Group by k-periods and calculate returns
        List<KPeriodSavings> savingsByDates = new ArrayList<>();

        for (int j = 0; j < kPeriods.size(); j++) {
            LocalDateTime kStart = kRanges.get(j)[0];
            LocalDateTime kEnd = kRanges.get(j)[1];
            double sum = 0;

            for (int i = 0; i < validDates.size(); i++) {
                if (!validDates.get(i).isBefore(kStart) && !validDates.get(i).isAfter(kEnd)) {
                    sum += validRemanents.get(i);
                }
            }

            // Calculate inflation-adjusted returns
            double invested = sum;
            double futureValue = invested * Math.pow(1 + rate, years);
            double inflationAdjusted = futureValue / Math.pow(1 + inflation, years);
            double profit = round2(inflationAdjusted - invested);

            // Tax benefit (only for NPS)
            double taxBenefit = 0.0;
            if (includeTaxBenefit) {
                taxBenefit = round2(calculateTaxBenefit(invested, wage));
            }

            savingsByDates.add(new KPeriodSavings(
                    kPeriods.get(j).getStart(),
                    kPeriods.get(j).getEnd(),
                    sum, profit, taxBenefit));
        }

        return new ReturnsResponse(totalTransactionAmount, totalCeiling, savingsByDates);
    }

    // ========== Helper methods ==========

    private double computeCeiling(double amount) {
        double remainder = amount % 100;
        if (remainder == 0)
            return amount;
        return amount + (100 - remainder);
    }

    private double calculateTaxBenefit(double invested, double wage) {
        double eligibleDeduction = Math.min(invested, Math.min(wage * 0.10, 200000));
        double taxWithout = calculateSimpleTax(wage);
        double taxWith = calculateSimpleTax(wage - eligibleDeduction);
        return taxWithout - taxWith;
    }

    private double calculateSimpleTax(double income) {
        if (income <= 700000)
            return 0;
        double tax = 0;
        if (income > 1500000) {
            tax += (income - 1500000) * 0.30;
            income = 1500000;
        }
        if (income > 1200000) {
            tax += (income - 1200000) * 0.20;
            income = 1200000;
        }
        if (income > 1000000) {
            tax += (income - 1000000) * 0.15;
            income = 1000000;
        }
        if (income > 700000) {
            tax += (income - 700000) * 0.10;
        }
        return tax;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private LocalDateTime parseLenient(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, LENIENT_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(dateStr, STRICT_FORMATTER);
        }
    }

    private List<LocalDateTime[]> parsePeriodRanges(List<QPeriod> periods) {
        List<LocalDateTime[]> ranges = new ArrayList<>();
        for (QPeriod p : periods) {
            ranges.add(new LocalDateTime[] { parseLenient(p.getStart()), parseLenient(p.getEnd()) });
        }
        return ranges;
    }

    private List<LocalDateTime[]> parsePeriodRangesP(List<PPeriod> periods) {
        List<LocalDateTime[]> ranges = new ArrayList<>();
        for (PPeriod p : periods) {
            ranges.add(new LocalDateTime[] { parseLenient(p.getStart()), parseLenient(p.getEnd()) });
        }
        return ranges;
    }

    private List<LocalDateTime[]> parsePeriodRangesK(List<KPeriod> periods) {
        List<LocalDateTime[]> ranges = new ArrayList<>();
        for (KPeriod p : periods) {
            ranges.add(new LocalDateTime[] { parseLenient(p.getStart()), parseLenient(p.getEnd()) });
        }
        return ranges;
    }
}
