package com.hardcraft.items;

import com.hardcraft.items.api.ItemData;
import com.hardcraft.items.api.ItemStatus;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

final class ItemRules implements Listener {
    private final DefaultItemsService items;
    ItemRules(DefaultItemsService items) { this.items = items; }

    @EventHandler public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) if (custom(item)) { event.getInventory().setResult(null); return; }
    }
    @EventHandler public void onAnvil(PrepareAnvilEvent event) {
        if (custom(event.getInventory().getFirstItem()) || custom(event.getInventory().getSecondItem())) event.setResult(null);
    }
    @EventHandler public void onTrade(TradeSelectEvent event) {
        for (ItemStack item : event.getInventory().getContents()) if (custom(item)) { event.setCancelled(true); return; }
    }
    @EventHandler public void onHopper(InventoryMoveItemEvent event) {
        ItemData data = items.read(event.getItem());
        if (data.status() != ItemStatus.NOT_CUSTOM && (!valid(data) || !data.definition().orElseThrow().transferable())) event.setCancelled(true);
    }
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory destination = event.getClickedInventory();
        ItemStack moving = event.getCursor();
        if (event.isShiftClick()) { destination = event.getView().getTopInventory(); moving = event.getCurrentItem(); }
        if (moving != null && !allowed(moving, destination, player.getUniqueId())) event.setCancelled(true);
    }
    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!allowed(event.getOldCursor(), event.getView().getTopInventory(), player.getUniqueId())) event.setCancelled(true);
    }

    private boolean allowed(ItemStack item, Inventory destination, UUID player) {
        ItemData data = items.read(item);
        if (data.status() == ItemStatus.NOT_CUSTOM) return true;
        if (!valid(data)) return false;
        if (data.definition().orElseThrow().transferable()) return true;
        if (destination == null) return false;
        return destination.getHolder() instanceof Player holder
                && holder.getUniqueId().equals(player)
                && data.owner().filter(player::equals).isPresent();
    }
    private boolean valid(ItemData data) { return data.status() == ItemStatus.KNOWN; }
    private boolean custom(ItemStack item) { return items.read(item).status() != ItemStatus.NOT_CUSTOM; }
}