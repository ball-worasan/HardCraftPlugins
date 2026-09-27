package com.hardcraft.economy;

import com.hardcraft.economy.api.LedgerEntry;
import com.hardcraft.economy.api.TransactionResult;
import com.hardcraft.economy.api.TransferResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EconomyRepository {
    private final String url;

    public EconomyRepository(Path database) { url = "jdbc:sqlite:" + database.toAbsolutePath(); }

    public void migrate() {
        try {
            Path path = Path.of(url.substring("jdbc:sqlite:".length()));
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (Connection connection = open()) {
                transaction(connection, () -> {
                    try (Statement sql = connection.createStatement()) {
                        sql.execute("CREATE TABLE IF NOT EXISTS hardcraft_economy_schema_version(version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
                        sql.execute("CREATE TABLE IF NOT EXISTS hardcraft_economy_wallets(owner_uuid TEXT PRIMARY KEY, balance INTEGER NOT NULL DEFAULT 0 CHECK(balance >= 0))");
                        sql.execute("CREATE TABLE IF NOT EXISTS hardcraft_economy_operations(idempotency_key TEXT PRIMARY KEY, fingerprint TEXT NOT NULL, transaction_id TEXT NOT NULL UNIQUE, source_balance INTEGER, destination_balance INTEGER, created_at INTEGER NOT NULL)");
                        sql.execute("CREATE TABLE IF NOT EXISTS hardcraft_economy_ledger(sequence INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id TEXT NOT NULL, owner_uuid TEXT NOT NULL, delta INTEGER NOT NULL CHECK(delta <> 0), balance_after INTEGER NOT NULL CHECK(balance_after >= 0), entry_type TEXT NOT NULL, idempotency_key TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(owner_uuid) REFERENCES hardcraft_economy_wallets(owner_uuid), FOREIGN KEY(idempotency_key) REFERENCES hardcraft_economy_operations(idempotency_key))");
                        sql.execute("CREATE INDEX IF NOT EXISTS hardcraft_economy_ledger_owner ON hardcraft_economy_ledger(owner_uuid,sequence)");
                        sql.executeUpdate("INSERT OR IGNORE INTO hardcraft_economy_schema_version VALUES(1,CURRENT_TIMESTAMP)");
                    }
                    return null;
                });
            }
        } catch (Exception failure) { throw failed("migration", failure); }
    }

    public long balance(UUID owner) {
        Objects.requireNonNull(owner, "owner");
        try (Connection connection = open(); PreparedStatement sql = connection.prepareStatement("SELECT balance FROM hardcraft_economy_wallets WHERE owner_uuid=?")) {
            sql.setString(1, owner.toString());
            try (ResultSet row = sql.executeQuery()) { return row.next() ? row.getLong(1) : 0; }
        } catch (SQLException failure) { throw failed("balance lookup", failure); }
    }

    public TransactionResult earn(UUID owner, long amount, String key) { return change(owner, amount, key, "EARN"); }
    public TransactionResult spend(UUID owner, long amount, String key) { return change(owner, -positive(amount), key, "SPEND"); }

    private TransactionResult change(UUID owner, long delta, String key, String type) {
        Objects.requireNonNull(owner, "owner");
        if (delta == 0) throw new IllegalArgumentException("amount must be positive");
        validateKey(key);
        String fingerprint = type + ":" + owner + ":" + Math.abs(delta);
        try (Connection connection = open()) {
            return transaction(connection, () -> {
                Replay replay = replay(connection, key, fingerprint);
                if (replay != null) return new TransactionResult(replay.transactionId, replay.sourceBalance, true);
                ensureWallet(connection, owner);
                long current = walletBalance(connection, owner);
                final long next;
                try { next = Math.addExact(current, delta); }
                catch (ArithmeticException overflow) { throw new IllegalArgumentException("balance overflow", overflow); }
                if (next < 0) throw new InsufficientFundsException();
                UUID transactionId = UUID.randomUUID();
                updateBalance(connection, owner, next);
                operation(connection, key, fingerprint, transactionId, next, null);
                ledger(connection, transactionId, owner, delta, next, type, key);
                return new TransactionResult(transactionId, next, false);
            });
        } catch (InsufficientFundsException | IllegalArgumentException failure) { throw failure; }
        catch (Exception failure) { throw failed(type.toLowerCase(), failure); }
    }

    public TransferResult transfer(UUID source, UUID destination, long amount, String key) {
        Objects.requireNonNull(source, "source"); Objects.requireNonNull(destination, "destination");
        positive(amount); validateKey(key);
        if (source.equals(destination)) throw new IllegalArgumentException("source and destination must differ");
        String fingerprint = "TRANSFER:" + source + ":" + destination + ":" + amount;
        try (Connection connection = open()) {
            return transaction(connection, () -> {
                Replay replay = replay(connection, key, fingerprint);
                if (replay != null) return new TransferResult(replay.transactionId, replay.sourceBalance, replay.destinationBalance, true);
                ensureWallet(connection, source); ensureWallet(connection, destination);
                long from = walletBalance(connection, source);
                if (from < amount) throw new InsufficientFundsException();
                long to;
                try { to = Math.addExact(walletBalance(connection, destination), amount); }
                catch (ArithmeticException overflow) { throw new IllegalArgumentException("balance overflow", overflow); }
                long remaining = from - amount;
                UUID transactionId = UUID.randomUUID();
                updateBalance(connection, source, remaining); updateBalance(connection, destination, to);
                operation(connection, key, fingerprint, transactionId, remaining, to);
                ledger(connection, transactionId, source, -amount, remaining, "TRANSFER_OUT", key);
                ledger(connection, transactionId, destination, amount, to, "TRANSFER_IN", key);
                return new TransferResult(transactionId, remaining, to, false);
            });
        } catch (InsufficientFundsException | IllegalArgumentException failure) { throw failure; }
        catch (Exception failure) { throw failed("transfer", failure); }
    }

    public List<LedgerEntry> ledger(UUID owner) {
        Objects.requireNonNull(owner, "owner");
        try (Connection connection = open(); PreparedStatement sql = connection.prepareStatement("SELECT sequence,transaction_id,delta,balance_after,entry_type,idempotency_key,created_at FROM hardcraft_economy_ledger WHERE owner_uuid=? ORDER BY sequence")) {
            sql.setString(1, owner.toString()); List<LedgerEntry> entries = new ArrayList<>();
            try (ResultSet rows = sql.executeQuery()) { while (rows.next()) entries.add(new LedgerEntry(rows.getLong(1), UUID.fromString(rows.getString(2)), owner, rows.getLong(3), rows.getLong(4), rows.getString(5), rows.getString(6), Instant.ofEpochMilli(rows.getLong(7)))); }
            return List.copyOf(entries);
        } catch (SQLException failure) { throw failed("ledger lookup", failure); }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement sql = connection.createStatement()) { sql.execute("PRAGMA foreign_keys=ON"); sql.execute("PRAGMA busy_timeout=15000"); }
        return connection;
    }
    private static <T> T transaction(Connection connection, SqlSupplier<T> work) throws Exception {
        try (Statement sql = connection.createStatement()) { sql.execute("BEGIN IMMEDIATE"); }
        try {
            T result = work.get();
            try (Statement sql = connection.createStatement()) { sql.execute("COMMIT"); }
            return result;
        } catch (Exception failure) {
            try (Statement sql = connection.createStatement()) { sql.execute("ROLLBACK"); }
            catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            throw failure;
        }
    }
    private static void ensureWallet(Connection c, UUID owner) throws SQLException { try (PreparedStatement s=c.prepareStatement("INSERT OR IGNORE INTO hardcraft_economy_wallets(owner_uuid,balance) VALUES(?,0)")){s.setString(1,owner.toString());s.executeUpdate();} }
    private static long walletBalance(Connection c, UUID owner) throws SQLException { try(PreparedStatement s=c.prepareStatement("SELECT balance FROM hardcraft_economy_wallets WHERE owner_uuid=?")){s.setString(1,owner.toString());try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("wallet missing");return r.getLong(1);}} }
    private static void updateBalance(Connection c, UUID owner, long balance) throws SQLException { try(PreparedStatement s=c.prepareStatement("UPDATE hardcraft_economy_wallets SET balance=? WHERE owner_uuid=?")){s.setLong(1,balance);s.setString(2,owner.toString());if(s.executeUpdate()!=1)throw new SQLException("wallet update failed");} }
    private static void operation(Connection c,String key,String fingerprint,UUID tx,long source,Long destination)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO hardcraft_economy_operations VALUES(?,?,?,?,?,?)")){s.setString(1,key);s.setString(2,fingerprint);s.setString(3,tx.toString());s.setLong(4,source);if(destination==null)s.setNull(5,Types.BIGINT);else s.setLong(5,destination);s.setLong(6,System.currentTimeMillis());s.executeUpdate();}}
    private static void ledger(Connection c,UUID tx,UUID owner,long delta,long balance,String type,String key)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT INTO hardcraft_economy_ledger(transaction_id,owner_uuid,delta,balance_after,entry_type,idempotency_key,created_at) VALUES(?,?,?,?,?,?,?)")){s.setString(1,tx.toString());s.setString(2,owner.toString());s.setLong(3,delta);s.setLong(4,balance);s.setString(5,type);s.setString(6,key);s.setLong(7,System.currentTimeMillis());s.executeUpdate();}}
    private static Replay replay(Connection c,String key,String fingerprint)throws SQLException{try(PreparedStatement s=c.prepareStatement("SELECT fingerprint,transaction_id,source_balance,destination_balance FROM hardcraft_economy_operations WHERE idempotency_key=?")){s.setString(1,key);try(ResultSet r=s.executeQuery()){if(!r.next())return null;if(!r.getString(1).equals(fingerprint))throw new IllegalArgumentException("idempotency key reused for a different operation");long destination=r.getLong(4);return new Replay(UUID.fromString(r.getString(2)),r.getLong(3),r.wasNull()?null:destination);}}}
    private static long positive(long amount){if(amount<=0)throw new IllegalArgumentException("amount must be positive");return amount;}
    private static void validateKey(String key){if(key==null||key.isBlank()||key.length()>128)throw new IllegalArgumentException("idempotency key must contain 1-128 characters");}
    private static IllegalStateException failed(String operation,Exception failure){return new IllegalStateException("Economy "+operation+" failed: "+failure.getMessage(),failure);}
    private record Replay(UUID transactionId,long sourceBalance,Long destinationBalance){}
    @FunctionalInterface private interface SqlSupplier<T>{T get()throws Exception;}
}