package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReturnsService {

    private static final double NPS_RATE = 0.0711; // 7.11% annual
    private static final double INDEX_RATE = 0.1449; // 14.49% annual
    private static final int RETIREMENT_AGE = 60;

    @Autowired
    private TaxService taxService;

    /**
     * Calculate NPS returns with tax benefit.
     * Inflation input can be percentage (e.g., 5.5) or decimal (e.g., 0.055).
     * If inflation > 1, treat as percentage and divide by 100.
     */
    public NpsResponse calculateNPS(NpsRequest request) {
        double invested = request.getInvested();
        int years = RETIREMENT_AGE - request.getAge();
        double inflation = normalizeRate(request.getInflation());

        // Compound interest: A = P(1 + r)^t
        double futureValue = invested * Math.pow(1 + NPS_RATE, years);

        // Profit
        double profit = futureValue - invested;

        // Inflation-adjusted value
        double inflationAdjusted = futureValue / Math.pow(1 + inflation, years);

        // Tax benefit
        double taxBenefit = taxService.calculateNPSTaxBenefit(invested, request.getWage());

        return new NpsResponse(
                round2(invested),
                round2(futureValue),
                round2(profit),
                round2(taxBenefit),
                round2(inflationAdjusted));
    }

    /**
     * Calculate Index Fund (NIFTY 50) returns.
     * Inflation input can be percentage (e.g., 5.5) or decimal (e.g., 0.055).
     */
    public IndexResponse calculateIndex(IndexRequest request) {
        double invested = request.getInvested();
        int years = RETIREMENT_AGE - request.getAge();
        double inflation = normalizeRate(request.getInflation());

        // Compound interest: A = P(1 + r)^t
        double futureValue = invested * Math.pow(1 + INDEX_RATE, years);

        // Profit
        double profit = futureValue - invested;

        // Inflation-adjusted value
        double inflationAdjusted = futureValue / Math.pow(1 + inflation, years);

        return new IndexResponse(
                round2(invested),
                round2(futureValue),
                round2(profit),
                round2(inflationAdjusted));
    }

    /**
     * Normalize rate: if |rate| > 1, it's a percentage (e.g., 5.5 → 0.055, -5.5 →
     * -0.055).
     * If |rate| <= 1, it's already a decimal (e.g., 0.055 or -0.055).
     */
    private double normalizeRate(double rate) {
        if (Math.abs(rate) > 1.0) {
            return rate / 100.0;
        }
        return rate;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
