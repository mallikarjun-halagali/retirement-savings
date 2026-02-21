package com.blackrock.challenge.service;

import org.springframework.stereotype.Service;

/**
 * Indian simplified tax calculator.
 * Tax slabs (new regime, simplified - no standard deductions):
 * 0 - 7,00,000 : 0%
 * 7,00,001 - 10,00,000: 10%
 * 10,00,001 - 12,00,000: 15%
 * 12,00,001 - 15,00,000: 20%
 * Above 15,00,000 : 30%
 */
@Service
public class TaxService {

    public double calculateTax(double income) {
        if (income <= 700000) {
            return 0.0;
        }

        double tax = 0.0;

        // Above 15L: 30%
        if (income > 1500000) {
            tax += (income - 1500000) * 0.30;
            income = 1500000;
        }

        // 12L - 15L: 20%
        if (income > 1200000) {
            tax += (income - 1200000) * 0.20;
            income = 1200000;
        }

        // 10L - 12L: 15%
        if (income > 1000000) {
            tax += (income - 1000000) * 0.15;
            income = 1000000;
        }

        // 7L - 10L: 10%
        if (income > 700000) {
            tax += (income - 700000) * 0.10;
        }

        return tax;
    }

    /**
     * Calculate NPS tax benefit.
     * Eligible deduction = min(invested, 10% of wage, 2,00,000)
     * Benefit = Tax(wage) - Tax(wage - eligible_deduction)
     */
    public double calculateNPSTaxBenefit(double invested, double wage) {
        double eligibleDeduction = Math.min(invested, Math.min(wage * 0.10, 200000));
        double taxWithoutDeduction = calculateTax(wage);
        double taxWithDeduction = calculateTax(wage - eligibleDeduction);
        return taxWithoutDeduction - taxWithDeduction;
    }
}
