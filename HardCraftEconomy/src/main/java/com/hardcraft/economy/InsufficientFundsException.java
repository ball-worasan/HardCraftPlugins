package com.hardcraft.economy;

public final class InsufficientFundsException extends IllegalStateException {
    public InsufficientFundsException() { super("Insufficient funds"); }
}