package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.*;
import com.blackrock.challenge.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    // Strict formatter for validation
    private static final DateTimeFormatter STRICT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Lenient formatter for period dates (handles invalid dates like Nov 31 → Dec
    // 1)
    private static final DateTimeFormatter LENIENT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.LENIENT);

    /**
     * Step 1: Parse expenses — compute ceiling and remanent for each.
     * ceiling = next multiple of 100 (round up)
     * remanent = ceiling - amount
     * If amount is already a multiple of 100, remanent = 0.
     */
    public ParseResponse parse(ParseRequest request) {
        List<Transaction> transactions = new ArrayList<>();
        for (Expense expense : request.getExpenses()) {
            long amount = expense.getAmount();
            long ceiling = computeCeiling(amount);
            long remanent = ceiling - amount;
            transactions.add(new Transaction(expense.getDate(), amount, ceiling, remanent));
        }
        return new ParseResponse(transactions);
    }

    /**
     * Validate expenses against constraints:
     * - amount must be >= 0 and < 500000
     * - date must be a valid datetime in format "yyyy-MM-dd HH:mm:ss"
     * - no duplicate dates
     */
    public ValidatorResponse validate(ValidatorRequest request) {
        List<Expense> valid = new ArrayList<>();
        List<Expense> invalid = new ArrayList<>();

        java.util.Set<String> seenDates = new java.util.HashSet<>();

        for (Expense expense : request.getExpenses()) {
            boolean isValid = true;

            // Check amount constraints: 0 <= amount < 500000
            if (expense.getAmount() < 0 || expense.getAmount() >= 500000) {
                isValid = false;
            }

            // Check date format validity (strict — invalid calendar dates like Nov 31 are
            // rejected)
            if (expense.getDate() == null || expense.getDate().isEmpty()) {
                isValid = false;
            } else {
                try {
                    LocalDateTime.parse(expense.getDate(), STRICT_FORMATTER);
                } catch (Exception e) {
                    isValid = false;
                }
            }

            // Check for duplicate dates
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
     * Apply q/p/k period rules and compute savings.
     *
     * Processing order:
     * 1. Calculate ceiling and remanent for each expense
     * 2. Apply q period rules (fixed amount override)
     * 3. Apply p period rules (extra amount addition)
     * 4. Group by k periods and sum remanents
     */
    public FilterResponse filter(FilterRequest request) {
        List<Expense> expenses = request.getExpenses();
        if (expenses == null)
            expenses = new ArrayList<>();

        List<QPeriod> qPeriods = request.getQ() != null ? request.getQ() : new ArrayList<>();
        List<PPeriod> pPeriods = request.getP() != null ? request.getP() : new ArrayList<>();
        List<KPeriod> kPeriods = request.getK() != null ? request.getK() : new ArrayList<>();

        // Pre-parse all period dates with LENIENT formatter (handles dates like Nov 31)
        List<LocalDateTime[]> qRanges = new ArrayList<>();
        for (QPeriod q : qPeriods) {
            qRanges.add(new LocalDateTime[] {
                    parseLenient(q.getStart()),
                    parseLenient(q.getEnd())
            });
        }

        List<LocalDateTime[]> pRanges = new ArrayList<>();
        for (PPeriod p : pPeriods) {
            pRanges.add(new LocalDateTime[] {
                    parseLenient(p.getStart()),
                    parseLenient(p.getEnd())
            });
        }

        List<LocalDateTime[]> kRanges = new ArrayList<>();
        for (KPeriod k : kPeriods) {
            kRanges.add(new LocalDateTime[] {
                    parseLenient(k.getStart()),
                    parseLenient(k.getEnd())
            });
        }

        // Process each expense
        long[] remanents = new long[expenses.size()];
        LocalDateTime[] expenseDates = new LocalDateTime[expenses.size()];

        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            long amount = expense.getAmount();
            LocalDateTime expDate = parseLenient(expense.getDate());
            expenseDates[i] = expDate;

            // Step 1: Calculate ceiling and remanent
            long ceiling = computeCeiling(amount);
            long remanent = ceiling - amount;

            // Step 2: Apply q period rules
            // If multiple q periods match, use the one with the latest start date.
            // If same start date, use the first one in the list.
            int bestQIndex = -1;
            LocalDateTime bestQStart = null;

            for (int j = 0; j < qPeriods.size(); j++) {
                LocalDateTime qStart = qRanges.get(j)[0];
                LocalDateTime qEnd = qRanges.get(j)[1];

                if (!expDate.isBefore(qStart) && !expDate.isAfter(qEnd)) {
                    // Transaction falls within this q period
                    if (bestQIndex == -1 || qStart.isAfter(bestQStart)) {
                        bestQIndex = j;
                        bestQStart = qStart;
                    }
                    // If same start date, keep the first one (lower index)
                }
            }

            if (bestQIndex != -1) {
                remanent = qPeriods.get(bestQIndex).getFixed();
            }

            // Step 3: Apply p period rules
            // Add ALL matching p period extras together
            for (int j = 0; j < pPeriods.size(); j++) {
                LocalDateTime pStart = pRanges.get(j)[0];
                LocalDateTime pEnd = pRanges.get(j)[1];

                if (!expDate.isBefore(pStart) && !expDate.isAfter(pEnd)) {
                    remanent += pPeriods.get(j).getExtra();
                }
            }

            remanents[i] = remanent;
        }

        // Step 4: Group by k periods
        List<Long> savingsByDates = new ArrayList<>();
        long totalSavings = 0;

        for (int j = 0; j < kPeriods.size(); j++) {
            LocalDateTime kStart = kRanges.get(j)[0];
            LocalDateTime kEnd = kRanges.get(j)[1];
            long sum = 0;

            for (int i = 0; i < expenses.size(); i++) {
                if (!expenseDates[i].isBefore(kStart) && !expenseDates[i].isAfter(kEnd)) {
                    sum += remanents[i];
                }
            }

            savingsByDates.add(sum);
            totalSavings += sum;
        }

        return new FilterResponse(savingsByDates, totalSavings);
    }

    /**
     * Compute the ceiling (next multiple of 100).
     * If amount is already a multiple of 100, ceiling = amount (remanent = 0).
     */
    private long computeCeiling(long amount) {
        if (amount % 100 == 0) {
            return amount;
        }
        return ((amount / 100) + 1) * 100;
    }

    /**
     * Parse a date string leniently — handles invalid calendar dates like
     * "2023-11-31"
     * by rolling over (Nov 31 → Dec 1).
     */
    private LocalDateTime parseLenient(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, LENIENT_FORMATTER);
        } catch (Exception e) {
            // Fallback to strict
            return LocalDateTime.parse(dateStr, STRICT_FORMATTER);
        }
    }
}
