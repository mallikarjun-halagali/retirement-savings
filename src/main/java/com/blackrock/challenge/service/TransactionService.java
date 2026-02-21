package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private static final DateTimeFormatter STRICT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter LENIENT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.LENIENT);

    /**
     * Parse expenses: compute ceiling and remanent for each.
     * Accepts a plain list of expenses (not wrapped in an object).
     */
    public List<Transaction> parseList(List<Expense> expenses) {
        List<Transaction> transactions = new ArrayList<>();
        for (Expense expense : expenses) {
            double amount = expense.getAmount();
            double ceiling = computeCeiling(amount);
            double remanent = ceiling - amount;
            transactions.add(new Transaction(expense.getDate(), amount, ceiling, remanent));
        }
        return transactions;
    }

    /**
     * Parse expenses from a wrapped request object.
     */
    public ParseResponse parse(ParseRequest request) {
        return new ParseResponse(parseList(request.getExpenses()));
    }

    /**
     * Validate expenses against constraints.
     */
    public ValidatorResponse validate(ValidatorRequest request) {
        List<Expense> valid = new ArrayList<>();
        List<Expense> invalid = new ArrayList<>();

        java.util.Set<String> seenDates = new java.util.HashSet<>();

        for (Expense expense : request.getExpenses()) {
            boolean isValid = true;

            if (expense.getAmount() < 0 || expense.getAmount() >= 500000) {
                isValid = false;
            }

            if (expense.getDate() == null || expense.getDate().isEmpty()) {
                isValid = false;
            } else {
                try {
                    LocalDateTime.parse(expense.getDate(), STRICT_FORMATTER);
                } catch (Exception e) {
                    isValid = false;
                }
            }

            if (isValid && !seenDates.add(expense.getDate())) {
                isValid = false;
            }

            if (isValid) {
                valid.add(expense);
            } else {
                invalid.add(expense);
            }
        }

        return new ValidatorResponse(valid, invalid);
    }

    /**
     * Apply q/p/k period rules and compute savings with returns.
     */
    public FilterResponse filter(FilterRequest request) {
        List<Expense> expenses = request.getExpenses();
        if (expenses == null)
            expenses = new ArrayList<>();

        List<QPeriod> qPeriods = request.getQ() != null ? request.getQ() : new ArrayList<>();
        List<PPeriod> pPeriods = request.getP() != null ? request.getP() : new ArrayList<>();
        List<KPeriod> kPeriods = request.getK() != null ? request.getK() : new ArrayList<>();

        int age = request.getAge();
        double wage = request.getWage();
        double inflation = request.getInflation();

        if (Math.abs(inflation) > 1.0) {
            inflation = inflation / 100.0;
        }

        int years = 60 - age;
        double npsRate = 0.0711;

        List<LocalDateTime[]> qRanges = new ArrayList<>();
        for (QPeriod q : qPeriods) {
            qRanges.add(new LocalDateTime[] { parseLenient(q.getStart()), parseLenient(q.getEnd()) });
        }

        List<LocalDateTime[]> pRanges = new ArrayList<>();
        for (PPeriod p : pPeriods) {
            pRanges.add(new LocalDateTime[] { parseLenient(p.getStart()), parseLenient(p.getEnd()) });
        }

        List<LocalDateTime[]> kRanges = new ArrayList<>();
        for (KPeriod k : kPeriods) {
            kRanges.add(new LocalDateTime[] { parseLenient(k.getStart()), parseLenient(k.getEnd()) });
        }

        double[] remanents = new double[expenses.size()];
        LocalDateTime[] expenseDates = new LocalDateTime[expenses.size()];

        double totalTransactionAmount = 0;
        double totalCeiling = 0;

        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            double amount = expense.getAmount();
            LocalDateTime expDate = parseLenient(expense.getDate());
            expenseDates[i] = expDate;

            double ceiling = computeCeiling(amount);
            double remanent = ceiling - amount;

            totalTransactionAmount += amount;
            totalCeiling += ceiling;

            // q period rules: latest start wins
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

            // p period rules: all extras stack
            for (int j = 0; j < pPeriods.size(); j++) {
                LocalDateTime pStart = pRanges.get(j)[0];
                LocalDateTime pEnd = pRanges.get(j)[1];
                if (!expDate.isBefore(pStart) && !expDate.isAfter(pEnd)) {
                    remanent += pPeriods.get(j).getExtra();
                }
            }

            remanents[i] = remanent;
        }

        // Group by k periods and calculate returns
        List<KPeriodSavings> savingsByDates = new ArrayList<>();

        for (int j = 0; j < kPeriods.size(); j++) {
            LocalDateTime kStart = kRanges.get(j)[0];
            LocalDateTime kEnd = kRanges.get(j)[1];
            double sum = 0;

            for (int i = 0; i < expenses.size(); i++) {
                if (!expenseDates[i].isBefore(kStart) && !expenseDates[i].isAfter(kEnd)) {
                    sum += remanents[i];
                }
            }

            double invested = sum;
            double futureValue = invested * Math.pow(1 + npsRate, years);
            double inflationAdjusted = futureValue / Math.pow(1 + inflation, years);
            double profit = round2(inflationAdjusted - invested);
            double taxBenefit = round2(calculateTaxBenefit(invested, wage));

            savingsByDates.add(new KPeriodSavings(
                    kPeriods.get(j).getStart(),
                    kPeriods.get(j).getEnd(),
                    sum,
                    profit,
                    taxBenefit));
        }

        return new FilterResponse(totalTransactionAmount, totalCeiling, savingsByDates);
    }

    /**
     * Round up to next multiple of 100. If already a multiple, return as-is.
     */
    private double computeCeiling(double amount) {
        double remainder = amount % 100;
        if (remainder == 0) {
            return amount;
        }
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
}
