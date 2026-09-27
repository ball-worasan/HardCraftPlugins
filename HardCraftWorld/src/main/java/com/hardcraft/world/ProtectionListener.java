package com.hardcraft.world;
import java.util.*;import org.bukkit.Location;import org.bukkit.block.Block;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.block.*;import org.bukkit.event.entity.*;import org.bukkit.event.hanging.HangingBreakByEntityEvent;import org.bukkit.event.inventory.InventoryMoveItemEvent;import org.bukkit.event.player.PlayerInteractEvent;
final class ProtectionListener implements Listener{
 private final ClaimService service;private final boolean pvp;ProtectionListener(ClaimService s,boolean p){service=s;pvp=p;}
 private Optional<Claim> at(Location l){return service.at(l.getWorld().getUID().toString(),l.getBlockX(),l.getBlockZ());}
 private boolean denied(Player p,Location l){return at(l).filter(c->!c.permits(p.getUniqueId())).isPresent();}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void breakBlock(BlockBreakEvent e){if(denied(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void place(BlockPlaceEvent e){if(denied(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){if(e.getClickedBlock()!=null&&denied(e.getPlayer(),e.getClickedBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void piston(BlockPistonExtendEvent e){for(Block b:e.getBlocks())if(!same(at(b.getLocation()),at(b.getRelative(e.getDirection()).getLocation()))){e.setCancelled(true);return;}}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void retract(BlockPistonRetractEvent e){for(Block b:e.getBlocks())if(!same(at(b.getLocation()),at(b.getRelative(e.getDirection()).getLocation()))){e.setCancelled(true);return;}}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void fluid(BlockFromToEvent e){if(!same(at(e.getBlock().getLocation()),at(e.getToBlock().getLocation())))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void explode(EntityExplodeEvent e){e.blockList().removeIf(b->at(b.getLocation()).isPresent());}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void explode(BlockExplodeEvent e){e.blockList().removeIf(b->at(b.getLocation()).isPresent());}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void hopper(InventoryMoveItemEvent e){Location a=e.getSource().getLocation(),b=e.getDestination().getLocation();if(a!=null&&b!=null&&!same(at(a),at(b)))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void hanging(HangingBreakByEntityEvent e){if(e.getRemover() instanceof Player p&&denied(p,e.getEntity().getLocation()))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST) public void damage(EntityDamageByEntityEvent e){if(e.getEntity() instanceof Player victim&&e.getDamager() instanceof Player attacker&&(!pvp||at(victim.getLocation()).isPresent()||at(attacker.getLocation()).isPresent()))e.setCancelled(true);}
 private boolean same(Optional<Claim>a,Optional<Claim>b){return a.map(Claim::id).equals(b.map(Claim::id));}
}