package br.trcraft.arena.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import br.trcraft.arena.Main;

//Em sua classe principal ou utilitária, por exemplo: KitManager.java

public class KitManager {

    private final Main plugin;

    public KitManager(Main plugin) {
        this.plugin = plugin;
    }

    // Salva o inventário atual do jogador na config (kit)
    public void salvarKit(Player player) {
        ItemStack[] itens = player.getInventory().getContents();
        List<Map<String, Object>> serializedItems = new ArrayList<>();

        for (ItemStack item : itens) {
            if (item == null) {
                serializedItems.add(null);
            } else {
                serializedItems.add(item.serialize());
            }
        }

        plugin.getConfig().set("kit.items", serializedItems);
        plugin.saveConfig();
    }

    // Aplica o kit salvo no jogador
    public void aplicarKit(Player player) {
        List<?> serializedItems = plugin.getConfig().getList("kit.items");
        if (serializedItems == null || serializedItems.isEmpty()) {
            player.sendMessage("§cKit da arena não configurado! Use /arena setitens para definir.");
            return;
        }

        List<ItemStack> itens = new ArrayList<>();
        for (Object obj : serializedItems) {
            if (obj == null) {
                itens.add(null);
            } else {
                if (obj instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    ItemStack item = ItemStack.deserialize((Map<String, Object>) map);
                    itens.add(item);
                } else {
                    itens.add(null);
                }
            }
        }

        player.getInventory().setContents(itens.toArray(new ItemStack[0]));
        player.updateInventory();
    }
}
