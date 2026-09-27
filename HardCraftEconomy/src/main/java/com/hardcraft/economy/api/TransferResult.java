package com.hardcraft.economy.api;

import java.util.UUID;

public record TransferResult(UUID transactionId, long sourceBalance, long destinationBalance, boolean replayed) {}