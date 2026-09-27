package com.hardcraft.economy.api;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(long sequence, UUID transactionId, UUID wallet, long delta, long balanceAfter,
                          String type, String idempotencyKey, Instant createdAt) {}