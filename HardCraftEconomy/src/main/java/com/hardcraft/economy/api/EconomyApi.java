package com.hardcraft.economy.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyApi {
    CompletableFuture<Long> balance(UUID owner);
    CompletableFuture<TransactionResult> earn(UUID owner, long amount, String idempotencyKey);
    CompletableFuture<TransactionResult> spend(UUID owner, long amount, String idempotencyKey);
    CompletableFuture<TransferResult> transfer(UUID source, UUID destination, long amount, String idempotencyKey);
    CompletableFuture<List<LedgerEntry>> ledger(UUID owner);
}