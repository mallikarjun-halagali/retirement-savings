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
public class TransactionService {

    private static final DateTimeFormatter STRICT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter LENIENT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.LENIENT);

    /**
     * Parse: accepts a plain list of expenses, returns enriched transactions.
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
     * Parse from wrapped request.
     */
    public ParseResponse parse(ParseRequest request) {
        return new ParseResponse(parseList(request.getExpenses()));
    }

    /**
     * Validator: validates transactions, returns valid/invalid with error messages.
     */
    public ValidatorResponse validate(ValidatorRequest request) {
        List<Transaction> valid = new ArrayList<>();
        List<InvalidTransaction> invalid = new ArrayList<>();
        Set<String> seenDates = new HashSet<>();

        for (Transaction txn : request.getTransactions()) {
            String errorMsg = null;

            if (txn.getAmount() < 0) {
                errorMsg = "Negative amounts are not allowed";
            } else if (txn.getAmount() >= 500000) {
                errorMsg = "Amount exceeds maximum allowed value of 500000";
            }

            if (errorMsg == null) {
                if (txn.getDate() == null || txn.getDate().isEmpty()) {
                    errorMsg = "Date is required";
                } else {
                    try {
                        LocalDateTime.parse(txn.getDate(), STRICT_FORMATTER);
                    } catch (Exception e) {
                        errorMsg = "Invalid date format. Expected: YYYY-MM-DD HH:mm:ss";
                    }
                }
            }

            if (errorMsg == null && !seenDates.add(txn.getDate())) {
                errorMsg = "Duplicate transaction date";
            }

            if (errorMsg == null) {
                valid.add(txn);
            } else {
                invalid.add(new InvalidTransaction(txn, errorMsg));
            }
        }

        return new ValidatorResponse(valid, invalid);
    }

    /**
     * Filter: validates transactions according to q, p, k period rules.
     * 1. Validate (negative amounts, duplicates) → invalid with message
     * 2. Enrich valid ones with ceiling/remanent
     * 3. Apply q-period rules (replace remanent with fixed value)
     * 4. Apply p-period rules (add extra to remanent)
     * 5. Check k-period membership (inKPeriod flag)
     * 6. Exclude transactions with remanent = 0
     */
    public FilterResponse filter(FilterRequest request) {
        List<Expense> expenses = request.getExpenses();
        if (expenses == null)
            expenses = new ArrayList<>();

        List<QPeriod> qPeriods = request.getQ() != null ? request.getQ() : new ArrayList<>();
        List<PPeriod> pPeriods = request.getP() != null ? request.getP() : new ArrayList<>();
        List<KPeriod> kPeriods = request.getK() != null ? request.getK() : new ArrayList<>();

        // Pre-parse period date ranges
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

        List<ValidTransaction> valid = new ArrayList<>();
        List<InvalidTransaction> invalid = new ArrayList<>();
        Set<String> seenDates = new HashSet<>();

        for (Expense expense : expenses) {
            double amount = expense.getAmount();
            String date = expense.getDate();

            // Step 1: Validate
            if (amount < 0) {
                invalid.add(new InvalidTransaction(date, amount, 0, 0, "Negative amounts are not allowed"));
                continue;
            }

            if (amount >= 500000) {
                invalid.add(
                        new InvalidTransaction(date, amount, 0, 0, "Amount exceeds maximum allowed value of 500000"));
                continue;
            }

            if (date == null || date.isEmpty()) {
                invalid.add(new InvalidTransaction(date, amount, 0, 0, "Date is required"));
                continue;
            }

            LocalDateTime expDate;
            try {
                expDate = LocalDateTime.parse(date, STRICT_FORMATTER);
            } catch (Exception e) {
                invalid.add(new InvalidTransaction(date, amount, 0, 0, "Invalid date format"));
                continue;
            }

            if (!seenDates.add(date)) {
                invalid.add(new InvalidTransaction(date, amount, 0, 0, "Duplicate transaction"));
                continue;
            }

            // Step 2: Compute ceiling and remanent
            double ceiling = computeCeiling(amount);
            double remanent = ceiling - amount;

            // Step 3: Apply q-period rules (latest start wins)
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

            // Step 4: Apply p-period rules (all extras stack)
            for (int j = 0; j < pPeriods.size(); j++) {
                LocalDateTime pStart = pRanges.get(j)[0];
                LocalDateTime pEnd = pRanges.get(j)[1];
                if (!expDate.isBefore(pStart) && !expDate.isAfter(pEnd)) {
                    remanent += pPeriods.get(j).getExtra();
                }
            }

            // Step 5: Skip transactions with zero remanent (e.g., q fixed=0 with no p)
            if (remanent == 0) {
                continue;
            }

            // Step 6: Check k-period membership
            boolean inKPeriod = false;
            for (int j = 0; j < kPeriods.size(); j++) {
                LocalDateTime kStart = kRanges.get(j)[0];
                LocalDateTime kEnd = kRanges.get(j)[1];
                if (!expDate.isBefore(kStart) && !expDate.isAfter(kEnd)) {
                    inKPeriod = true;
                    break;
                }
            }

            valid.add(new ValidTransaction(date, amount, ceiling, remanent, inKPeriod));
        }

        return new FilterResponse(valid, invalid);
    }

    /**
     * Round up to next multiple of 100. If already a multiple, return as-is.
     */
    private double computeCeiling(double amount) {
        double remainder = amount % 100;
        if (remainder == 0)
            return amount;
        return amount + (100 - remainder);
    }

    private LocalDateTime parseLenient(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, LENIENT_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(dateStr, STRICT_FORMATTER);
        }
    }
}
