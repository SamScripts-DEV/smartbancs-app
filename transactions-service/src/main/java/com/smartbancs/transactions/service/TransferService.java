package com.smartbancs.transactions.service;

import com.smartbancs.transactions.model.Account;
import com.smartbancs.transactions.model.Transaction;
import com.smartbancs.transactions.model.AccountNotFoundException;
import com.smartbancs.transactions.repository.AccountRepository;
import com.smartbancs.transactions.repository.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final long SLOW_LOCK_THRESHOLD_MS = 300;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferService self;
    private final AiRecommendationService aiRecommendationService;
    private final MeterRegistry meterRegistry;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                            @Lazy TransferService self, AiRecommendationService aiRecommendationService,
                            MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.self = self;
        this.aiRecommendationService = aiRecommendationService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String idempotencyKey) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Transaction result = doTransfer(sourceAccountId, destinationAccountId, amount, idempotencyKey, traceId);
            meterRegistry.counter("transfers.success").increment();
            return result;
        } catch (RuntimeException e) {
            meterRegistry.counter("transfers.failed", "reason", e.getClass().getSimpleName()).increment();
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("transfers.duration"));
            MDC.remove("traceId");
        }
    }

    private Transaction doTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String idempotencyKey, String traceId) {
        log.info("Starting transfer: source={} destination={} amount={}", sourceAccountId, destinationAccountId, amount);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if(existing.isPresent()) {
            return existing.get();
        }

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");

        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Long firstLockId = Math.min(sourceAccountId, destinationAccountId);
        Long secondLockId = Math.max(sourceAccountId, destinationAccountId);


        Account first = lockAccount(firstLockId);
        Account second = lockAccount(secondLockId);


        Account source = sourceAccountId.equals(first.getId()) ? first : second;
        Account destination = sourceAccountId.equals(first.getId()) ? second : first;

        source.debit(amount);
        log.info("Debited {} from account {}", amount, source.getId());

        destination.credit(amount);
        log.info("Credited {} to account {}", amount, destination.getId());

        Transaction transaction = new Transaction(idempotencyKey, sourceAccountId, destinationAccountId, amount, traceId);
        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction {} saved with status {}", saved.getId(), saved.getStatus());

        log.info("Triggering AI analysis for transaction {}", saved.getId());
        aiRecommendationService.analyzeTransaction(saved.getId(), sourceAccountId, amount);

        return saved;
    }

    private Account lockAccount(Long accountId) {
        long start = System.currentTimeMillis();
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > SLOW_LOCK_THRESHOLD_MS) {
            log.warn("Slow lock acquisition: account {} took {} ms", accountId, elapsed);
        }
        return account;
    }

    public Transaction transferSafely(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String idempotencyKey) {
        try {
            return self.transfer(sourceAccountId, destinationAccountId, amount, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
        }
    }
}
