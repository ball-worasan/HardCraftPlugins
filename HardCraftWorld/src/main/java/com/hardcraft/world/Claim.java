package com.hardcraft.world;
import java.util.Set;import java.util.UUID;
public record Claim(long id,UUID owner,String world,int minX,int maxX,int minZ,int maxZ,Set<UUID> trusted){
 public Claim{trusted=Set.copyOf(trusted);if(minX>maxX||minZ>maxZ)throw new IllegalArgumentException("invalid bounds");}
 public boolean contains(String w,int x,int z){return world.equals(w)&&x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ;}
 public boolean overlaps(Claim c){return world.equals(c.world)&&minX<=c.maxX&&maxX>=c.minX&&minZ<=c.maxZ&&maxZ>=c.minZ;}
 public boolean permits(UUID player){return owner.equals(player)||trusted.contains(player);}
}