package br.trcraft.arena.Eventos;

import br.trcraft.arena.Utils.ItensArena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemIntegrityFixer implements Listener {

    private final Map<UUID, Long> lastPickupWarn = new ConcurrentHashMap<>();
    private final long cooldownMillis = 5000; // 5 segundos
    private final String protectedName;
    private final FileConfiguration config;

    public ItemIntegrityFixer(FileConfiguration config) {
        this.config = config;
        // Carrega o nome base da config, remove cores, deixa minúsculo e trim para comparação
        String rawName = config.getString("protection-items.name", "&e&lARENA");
        this.protectedName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', rawName)).toLowerCase().trim();
    }

    private String getMsg(String path, String fallback) {
        return config.getString("mensagens." + path, fallback);
    }

    private boolean isProtectedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasDisplayName()) return false;

        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase().trim();
        return itemName.contains(protectedName);
    }

    private void removeProtectedItems(Player player) {
        PlayerInventory inv = player.getInventory();

        for (ItemStack item : inv.getContents()) {
            if (isProtectedItem(item)) inv.remove(item);
        }

        if (isProtectedItem(inv.getHelmet())) inv.setHelmet(null);
        if (isProtectedItem(inv.getChestplate())) inv.setChestplate(null);
        if (isProtectedItem(inv.getLeggings())) inv.setLeggings(null);
        if (isProtectedItem(inv.getBoots())) inv.setBoots(null);

        player.updateInventory();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!ItensArena.isInArena(player)) removeProtectedItems(player);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!ItensArena.isInArena(player)) removeProtectedItems(player);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (ItensArena.isInArena(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (isProtectedItem(item)) {
            event.setCancelled(true);
            event.getItemDrop().remove();
            player.sendMessage(getMsg("item_protegido_drop_fora", "§6§lARENA §cVocê não pode dropar itens protegidos fora da arena."));
            player.updateInventory();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (ItensArena.isInArena(player)) return;

        ItemStack item = event.getItem();
        if (!isProtectedItem(item)) return;

        Material type = item.getType();
        if (type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE")
                || type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS")) {

            event.setCancelled(true);

            EquipmentSlot slot = event.getHand();
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else if (slot == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            }

            player.sendMessage(getMsg("item_protegido_equipar_fora", "§6§lARENA §cVocê não pode equipar itens protegidos fora da arena."));
            player.updateInventory();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (ItensArena.isInArena(player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean cursorProtected = isProtectedItem(cursor);
        boolean currentProtected = isProtectedItem(current);
        boolean topInventory = event.getView().getTopInventory().getType() != InventoryType.PLAYER;

        boolean block = false;

        if (topInventory && (cursorProtected || currentProtected)) {
            block = true;
        } else if (cursorProtected || currentProtected) {
            block = switch (event.getAction()) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME,
                     SWAP_WITH_CURSOR, HOTBAR_SWAP,
                     MOVE_TO_OTHER_INVENTORY, HOTBAR_MOVE_AND_READD,
                     PICKUP_ALL, PICKUP_HALF, PICKUP_ONE,
                     PICKUP_SOME, COLLECT_TO_CURSOR,
                     CLONE_STACK, UNKNOWN -> true;
                default -> false;
            };
        }

        if (block) {
            event.setCancelled(true);
            if (cursorProtected) event.getView().setCursor(null);
            if (currentProtected) event.setCurrentItem(null);

            player.sendMessage(getMsg("item_protegido_manipular_fora", "§6§lARENA §cVocê não pode manipular itens protegidos fora da arena."));
            player.updateInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (ItensArena.isInArena(player)) return;

        ItemStack dragged = event.getOldCursor();
        if (isProtectedItem(dragged)) {
            event.setCancelled(true);
            event.setCursor(null);
            player.sendMessage(getMsg("item_protegido_arrastar_fora", "§6§lARENA §cVocê não pode arrastar itens protegidos fora da arena."));
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (ItensArena.isInArena(player)) return;

        if (isProtectedItem(event.getMainHandItem()) || isProtectedItem(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage(getMsg("item_protegido_trocar_maos_fora", "§6§lARENA §cVocê não pode trocar itens protegidos de mão fora da arena."));
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (ItensArena.isInArena(player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (isProtectedItem(item)) {
            event.setCancelled(true);
            event.getItem().remove();

            long now = System.currentTimeMillis();
            UUID uuid = player.getUniqueId();

            if (!lastPickupWarn.containsKey(uuid) || now - lastPickupWarn.get(uuid) > cooldownMillis) {
                player.sendMessage(getMsg("item_protegido_pegar_fora", "§6§lARENA §cVocê não pode pegar itens protegidos fora da arena."));
                lastPickupWarn.put(uuid, now);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (ItensArena.isInArena(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (isProtectedItem(item)) {
            event.setCancelled(true);
            player.sendMessage(getMsg("item_protegido_usar_fora", "§6§lARENA §cVocê não pode usar itens protegidos fora da arena."));
        }
    }
}
