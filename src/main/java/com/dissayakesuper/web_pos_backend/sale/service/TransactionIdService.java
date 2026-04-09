package com.dissayakesuper.web_pos_backend.sale.service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.repository.SaleRepository;

@Service
@Transactional
public class TransactionIdService {

    private static final String PREFIX = "TRX-";
    private static final int DIGITS = 6;
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("^TRX-(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^(\\d+)$");

    private final SaleRepository saleRepository;
    private final Long configuredSeed;
    private final AtomicLong manualSeed = new AtomicLong(-1L);
    private final AtomicLong lastIssued = new AtomicLong(-1L);

    public TransactionIdService(
            SaleRepository saleRepository,
            @Value("${INITIAL_TRX_ID:}") String initialTrxId
    ) {
        this.saleRepository = saleRepository;
        this.configuredSeed = parseSeedValue(initialTrxId).orElse(-1L);
    }

    public synchronized String nextTransactionId() {
        long base = resolveCurrentBaseNumber();
        long next = base + 1;
        lastIssued.set(next);
        return formatTransactionId(next);
    }

    public synchronized String peekNextTransactionId() {
        return formatTransactionId(resolveCurrentBaseNumber() + 1);
    }

    public synchronized String setMigrationSeed(String lastTransactionId) {
        if (saleRepository.count() > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transaction seed can only be set while sales history is empty."
            );
        }

        long parsed = parseTransactionNumber(lastTransactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid transaction id. Use TRX-XXXXXX or a numeric value."
                ));

        manualSeed.set(parsed);
        lastIssued.updateAndGet(current -> Math.max(current, parsed));
        return formatTransactionId(parsed);
    }

    @Transactional(readOnly = true)
    public synchronized String getCurrentBaseTransactionId() {
        long currentBase = resolveCurrentBaseNumber();
        if (currentBase <= 0) {
            return formatTransactionId(0);
        }
        return formatTransactionId(currentBase);
    }

    private long resolveCurrentBaseNumber() {
        long fromDatabase = saleRepository
                .findFirstByReceiptNoStartingWithOrderByReceiptNoDesc(PREFIX)
                .map(Sale::getReceiptNo)
                .flatMap(this::parseTransactionNumber)
                .orElse(-1L);

        long fromSeed = saleRepository.count() == 0
                ? Math.max(configuredSeed, manualSeed.get())
                : -1L;

        return Math.max(Math.max(fromDatabase, fromSeed), lastIssued.get());
    }

    private Optional<Long> parseSeedValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return parseTransactionNumber(raw);
    }

    private Optional<Long> parseTransactionNumber(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        Matcher trxMatcher = TRANSACTION_ID_PATTERN.matcher(trimmed);
        if (trxMatcher.matches()) {
            return parsePositiveLong(trxMatcher.group(1));
        }

        Matcher numericMatcher = NUMERIC_PATTERN.matcher(trimmed);
        if (numericMatcher.matches()) {
            return parsePositiveLong(numericMatcher.group(1));
        }

        return Optional.empty();
    }

    private Optional<Long> parsePositiveLong(String numeric) {
        try {
            long parsed = Long.parseLong(numeric);
            if (parsed < 0) {
                return Optional.empty();
            }
            return Optional.of(parsed);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String formatTransactionId(long sequence) {
        return PREFIX + String.format("%0" + DIGITS + "d", Math.max(0, sequence));
    }
}
