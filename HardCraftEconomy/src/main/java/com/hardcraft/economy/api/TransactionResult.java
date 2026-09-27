package com.hardcraft.economy.api;

import java.util.UUID;

public record TransactionResult(UUID transactionId, long balance, boolean replayed) {}