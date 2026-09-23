package com.smartbancs.transactions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);
    private static final BigDecimal BUDGET_ALERT_THRESHOLD = new BigDecimal("500");

    @Async
    public void analyzeTransaction(Long transactionId, Long accountId, BigDecimal amount) {
        log.info("AI analysis started for transactionId={} accountId={} amount={}", transactionId, accountId, amount);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String recommendation = amount.compareTo(BUDGET_ALERT_THRESHOLD) > 0
                ? "Consider setting a budget alert"
                : "No action needed";

        log.info("AI analysis finished for transactionId={} recommendation=\"{}\"", transactionId, recommendation);
    }
}
