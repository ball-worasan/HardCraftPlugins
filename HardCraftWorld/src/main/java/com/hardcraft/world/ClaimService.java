package com.hardcraft.world;
import java.util.*;import java.util.concurrent.*;
final class ClaimService implements AutoCloseable{
 private final ClaimRepository repo;private final ExecutorService db=Executors.newSingleThreadExecutor(Thread.ofPlatform().name("HardCraftWorld-DB").factory());private volatile List<Claim> claims;
 ClaimService(ClaimRepository r){repo=r;claims=List.copyOf(r.all());}
 Optional<Claim> at(String w,int x,int z){return claims.stream().filter(c->c.contains(w,x,z)).findFirst();}
 List<Claim> snapshot(){return claims;}
 CompletableFuture<Claim> create(UUID o,String w,int x,int z,int radius,int max){return work(()->{if(claims.stream().filter(c->c.owner().equals(o)).count()>=max)throw new IllegalArgumentException("claim limit reached");return repo.create(o,w,x-radius,x+radius,z-radius,z+radius);});}
 CompletableFuture<Void> delete(long id,UUID actor,boolean admin){return work(()->{Claim c=require(id);if(!admin&&!c.owner().equals(actor))throw new SecurityException("not claim owner");repo.delete(id,actor);return null;});}
 CompletableFuture<Void> trust(long id,UUID player,UUID actor,boolean add){return work(()->{Claim c=require(id);if(!c.owner().equals(actor))throw new SecurityException("not claim owner");if(c.owner().equals(player))throw new IllegalArgumentException("owner trust cannot change");repo.trust(id,player,actor,add);return null;});}
 private Claim require(long id){return claims.stream().filter(c->c.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("claim not found"));}
 private <T> CompletableFuture<T> work(Callable<T> x){return CompletableFuture.supplyAsync(()->{try{T result=x.call();claims=List.copyOf(repo.all());return result;}catch(RuntimeException e){throw e;}catch(Exception e){throw new CompletionException(e);}},db);}
 public void close(){db.shutdown();try{if(!db.awaitTermination(15,TimeUnit.SECONDS))db.shutdownNow();}catch(InterruptedException e){db.shutdownNow();Thread.currentThread().interrupt();}}
}