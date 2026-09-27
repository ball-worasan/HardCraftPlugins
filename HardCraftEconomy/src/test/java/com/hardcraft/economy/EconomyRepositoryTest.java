package com.hardcraft.economy;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EconomyRepositoryTest {
    @Test void earnSpendTransferLedgerAndRetry(@TempDir Path directory){
        EconomyRepository r=repository(directory);UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        var earn=r.earn(a,100,"earn");assertEquals(100,earn.balance());
        assertTrue(r.earn(a,100,"earn").replayed());assertEquals(100,r.balance(a));
        r.spend(a,30,"spend");var transfer=r.transfer(a,b,25,"transfer");
        assertEquals(45,transfer.sourceBalance());assertEquals(25,transfer.destinationBalance());
        assertEquals(3,r.ledger(a).size());assertEquals(1,r.ledger(b).size());
        assertThrows(IllegalArgumentException.class,()->r.earn(a,1,"earn"));
    }
    @Test void insufficientFundsRollsBackEverything(@TempDir Path directory){
        EconomyRepository r=repository(directory);UUID a=UUID.randomUUID(),b=UUID.randomUUID();r.earn(a,10,"seed");
        assertThrows(InsufficientFundsException.class,()->r.transfer(a,b,11,"failed"));
        assertEquals(10,r.balance(a));assertEquals(0,r.balance(b));assertEquals(1,r.ledger(a).size());assertTrue(r.ledger(b).isEmpty());
        assertThrows(InsufficientFundsException.class,()->r.spend(a,11,"failed-spend"));assertEquals(10,r.balance(a));
    }
    @Test void concurrentTransfersNeverOverdrawAndConserveMoney(@TempDir Path directory)throws Exception{
        EconomyRepository r=repository(directory);UUID a=UUID.randomUUID(),b=UUID.randomUUID();r.earn(a,100,"seed");
        try(ExecutorService pool=Executors.newFixedThreadPool(16)){
            var futures=new java.util.ArrayList<Future<Boolean>>();
            for(int i=0;i<40;i++){int n=i;futures.add(pool.submit(()->{try{r.transfer(a,b,10,"transfer-"+n);return true;}catch(InsufficientFundsException failure){return false;}}));}
            int succeeded=0;for(var future:futures)if(future.get())succeeded++;
            assertEquals(10,succeeded);assertEquals(0,r.balance(a));assertEquals(100,r.balance(b));assertEquals(100,r.balance(a)+r.balance(b));
        }
    }
    @Test void restartPreservesBalancesLedgerAndIdempotency(@TempDir Path directory){
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();EconomyRepository first=repository(directory);var result=first.earn(a,50,"earn");first.transfer(a,b,20,"transfer");
        EconomyRepository restarted=repository(directory);assertEquals(30,restarted.balance(a));assertEquals(20,restarted.balance(b));assertEquals(2,restarted.ledger(a).size());
        var replay=restarted.earn(a,50,"earn");assertTrue(replay.replayed());assertEquals(result.transactionId(),replay.transactionId());assertEquals(30,restarted.balance(a));
    }
    @Test void validatesAmountsAndOverflow(@TempDir Path directory){
        EconomyRepository r=repository(directory);UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->r.earn(a,0,"zero"));assertThrows(IllegalArgumentException.class,()->r.transfer(a,a,1,"self"));
        r.earn(a,Long.MAX_VALUE,"max");assertThrows(IllegalArgumentException.class,()->r.earn(a,1,"overflow"));assertEquals(Long.MAX_VALUE,r.balance(a));assertEquals(1,r.ledger(a).size());
    }
    private static EconomyRepository repository(Path directory){EconomyRepository r=new EconomyRepository(directory.resolve("hardcraft.db"));r.migrate();return r;}
}