package br.trcraft.arena.Utils;

import br.trcraft.arena.Main;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static br.trcraft.arena.Utils.ItensConfig.getItensConfig;
import static br.trcraft.arena.Utils.ItensConfig.saveItensConfig;

public class ItensArena {

    private static final Main plugin = Main.get(); // Instância principal do plugin

    public static final Set<UUID> arena = new HashSet<>();
    public static final Set<UUID> cooldownarena = new HashSet<>();
    public static final Set<UUID> arenaCamarote = new HashSet<>();
    public static final Map<UUID, BukkitRunnable> sairTasks = new HashMap<>();
    public static final int CONTAGEM_SEGUNDOS = 5;

    // Verifica se jogador está na arena
    public static boolean isInArena(Player player) {
        return arena.contains(player.getUniqueId());
    }

    // Entrar na arena: teleporta, aplica kit e efeitos
    public static void entrarNaArena(Player player) {
        // Verificar se o jogador está no camarote
        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
            player.sendMessage(ConfigManager.getMensagem("arena_no_camarote_entrada"));
            return; // Impede o jogador de entrar na arena se estiver no camarote
        }

        if (isInArena(player)) {
            player.sendMessage(ConfigManager.getMensagem("ja_na_arena"));
            return;
        }

        if (!isInventarioVazio(player)) {
            player.sendMessage(ConfigManager.getMensagem("inventario_nao_vazio"));
            return;
        }

        if (!kitEstaSalvo()) {
            player.sendMessage(ConfigManager.getMensagem("kit_nao_configurado"));
            return;
        }

        Location arenaLoc = plugin.getArenaLocation("ARENA");
        if (arenaLoc == null) {
            player.sendMessage(ConfigManager.getMensagem("localizacao_nao_configurada"));
            return;
        }

        player.teleport(arenaLoc);
        arena.add(player.getUniqueId());

        clearInventory(player);
        removerEfeitos(player);
        repararItens(player);
        aplicarEfeitos(player);

        aplicarKit(player); // Aplica o kit salvo

        MensagensArena.enviarMensagemEntrada(player);
    }

    // Sair da arena com ou sem contagem
    public static void sairDaArena(Player player, boolean comContagem) {
        if (!isInArena(player)) {
            player.sendMessage(ConfigManager.getMensagem("nao_esta_na_arena"));
            return;
        }

        if (comContagem) {
            iniciarContagemSaida(player);
        } else {
            removerPlayerDaArena(player);
            player.sendMessage(ConfigManager.getMensagem("saiu_da_arena"));
        }
    }

    //######################################################################

    // Método para o jogador sair do camarote
    public static void sairDoCamarote(Player player) {
        if (!arenaCamarote.contains(player.getUniqueId())) {
            player.sendMessage(ConfigManager.getMensagem("nao_esta_no_camarote"));
            return;
        }

        // Pega a localização de saída
        Location saidaLocation = plugin.getArenaLocation("SAIR");

        if (saidaLocation == null) {
            player.sendMessage(ConfigManager.getMensagem("localizacao_saida_nao_configurada"));
            return;
        }

        // Teleporta o jogador para a localização de saída
        player.teleport(saidaLocation);

        // Coloca o jogador de volta ao modo normal (adapte conforme necessário)
        player.setGameMode(GameMode.SURVIVAL); // Retorna ao modo Survival

        // Remove o UUID do jogador da lista de camarote
        arenaCamarote.remove(player.getUniqueId());

        // Limpar possíveis efeitos e status do jogador (caso tenha sido modificado no camarote)
        ItensArena.resetarKitCompleto(player);
        ItensArena.repararItens(player);

        // Enviar mensagem para o jogador
        player.sendMessage(ConfigManager.getMensagem("saiu_do_camarote"));
    }

    // Método para o jogador entrar no camarote
    public static void entrarNoCamarote(Player player) {
        if (isInArena(player)) {
            player.sendMessage(ConfigManager.getMensagem("arena_proibe_camarote"));
            return;
        }

        if (player.isDead()) {
            player.sendMessage(ConfigManager.getMensagem("morto_proibe_camarote"));
            return;
        }

        if (arenaCamarote.contains(player.getUniqueId())) {
            player.sendMessage(ConfigManager.getMensagem("ja_no_camarote"));
            return;
        }

        Location camaroteLocation = plugin.getCamaroteLocation(); // Pega a localização do camarote

        if (camaroteLocation == null) {
            player.sendMessage(ConfigManager.getMensagem("localizacao_camarote_nao_configurada"));
            return;
        }

        // Teleporta o jogador para o camarote e coloca em modo espectador
        player.teleport(camaroteLocation);
        player.setGameMode(GameMode.SPECTATOR);

        // Adiciona o UUID do jogador à lista do camarote
        arenaCamarote.add(player.getUniqueId());

        player.sendMessage(ConfigManager.getMensagem("entrou_no_camarote"));
    }

    public static void listarTodosJogadoresArenaECamarote(CommandSender sender) {
        // Jogadores na arena
        sender.sendMessage("§aJogadores na arena:");
        if (arena.isEmpty()) {
            sender.sendMessage("§eNão há jogadores na arena no momento.");
        } else {
            List<String> nomesArena = new ArrayList<>();
            for (UUID uuid : arena) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    nomesArena.add("§f" + player.getName());
                }
            }
            String listaArena = String.join("§a, ", nomesArena);
            sender.sendMessage(listaArena);
        }

        // Linha em branco entre seções
        sender.sendMessage(" ");

        // Jogadores no camarote
        sender.sendMessage("§aJogadores no camarote:");
        if (arenaCamarote.isEmpty()) {
            sender.sendMessage("§eNão há jogadores no camarote no momento.");
        } else {
            List<String> nomesCamarote = new ArrayList<>();
            for (UUID uuid : arenaCamarote) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    nomesCamarote.add("§f" + player.getName());
                }
            }
            String listaCamarote = String.join("§a, ", nomesCamarote);
            sender.sendMessage(listaCamarote);
        }
    }

    //######################################################################

    public static void sairDaArena(Player player) {
        sairDaArena(player, true);
    }

    public static boolean kitEstaSalvo() {
        return getItensConfig().contains("kit");
    }

    // Inicia a contagem regressiva para saída da arena
    public static void iniciarContagemSaida(Player player) {
        UUID uuid = player.getUniqueId();

        if (sairTasks.containsKey(uuid)) {
            // Não envia mensagem para não poluir chat se já estiver na contagem
            return;
        }

        player.sendMessage(ConfigManager.getMensagem("saida_contagem_iniciada")
                .replace("{segundos}", String.valueOf(CONTAGEM_SEGUNDOS)));

        Location destino = plugin.getArenaLocation("SAIR");
        if (destino == null) destino = player.getWorld().getSpawnLocation();

        BukkitRunnable task = new BukkitRunnable() {
            int contador = CONTAGEM_SEGUNDOS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    sairTasks.remove(uuid);
                    cancel();
                    return;
                }

                if (contador <= 0) {
                    sairTasks.remove(uuid);
                    removerPlayerDaArena(player);

                    cancel();
                    return;
                }

                player.sendTitle(
                        ConfigManager.getMensagem("saida_titulo"),
                        ConfigManager.getMensagem("saida_subtitulo").replace("{segundos}", String.valueOf(contador)),
                        0, 20, 0);

                contador--;
            }
        };

        sairTasks.put(uuid, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    // Cancela a contagem regressiva de saída, se houver
    public static void cancelarContagemSaida(Player player) {
        BukkitRunnable task = sairTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    // Remove jogador da arena: teleporta, limpa e remove dos sets
    public static void removerPlayerDaArena(Player player) {
        UUID uuid = player.getUniqueId();

        cancelarContagemSaida(player);
        clearInventory(player);
        removerEfeitos(player);
        //player.sendMessage(ConfigManager.getMensagem("saiu_da_arena"));
        Location loc = plugin.getArenaLocation("SAIR");
        player.teleport(loc != null ? loc : player.getWorld().getSpawnLocation());

        arena.remove(uuid);
        cooldownarena.remove(uuid);

    }

    // Registra a morte do jogador na arena
    public static void registrarMorte(Player player) {
        if (player == null || !player.isOnline() || !arena.contains(player.getUniqueId())) return;

        cancelarContagemSaida(player);
        clearInventory(player);
        removerEfeitos(player);
        arena.remove(player.getUniqueId());
        cooldownarena.remove(player.getUniqueId());

        Location spawn = plugin.getArenaLocation("SAIR");
        player.teleport(spawn != null ? spawn : player.getWorld().getSpawnLocation());
        player.sendMessage(ConfigManager.getMensagem("morreu_na_arena"));
    }

    // Renova efeitos e vida do jogador após matar alguém
    public static void renovarMSGPosKill(Player killer) {
        if (killer == null || !killer.isOnline()) return;

        killer.sendMessage("");
        killer.sendMessage(ConfigManager.getMensagem("bonus_pos_kill_titulo"));
        killer.sendMessage(ConfigManager.getMensagem("bonus_pos_kill_subtitulo"));
        killer.sendMessage(ConfigManager.getMensagem("bonus_pos_kill_rodape"));
        killer.sendMessage("");
    }

    @SuppressWarnings("deprecation")
    public static void resetarKitCompleto(Player player) {
        if (player == null || !player.isOnline() || !isInArena(player)) return;

        clearInventory(player);
        removerEfeitos(player);
        aplicarEfeitos(player);

        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(Main.get(), () -> {
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                try {
                    ItensArena.aplicarKit(onlinePlayer);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[Arena] Erro ao aplicar kit para " + onlinePlayer.getName());
                    e.printStackTrace();
                    return;
                }

                onlinePlayer.setHealth(onlinePlayer.getMaxHealth());
                onlinePlayer.setFoodLevel(20);
                onlinePlayer.setSaturation(20f);
                onlinePlayer.setFireTicks(0);
            }
        }, 1L);
    }

    // Aplica efeitos de poção padrão da arena
    public static void aplicarEfeitos(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60 * 20, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60 * 20, 1));
    }

    // Remove todos efeitos ativos do jogador
    public static void removerEfeitos(Player player) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    // Limpa inventário e armadura
    public static void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
    }

    // Verifica se inventário, armadura e offhand estão vazios
    public static boolean isInventarioVazio(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) return false;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        return offhand == null || offhand.getType() == Material.AIR;
    }

    // Repara durabilidade dos itens e armaduras
    public static void repararItens(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            reparar(item);
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            reparar(armor);
        }
        reparar(player.getInventory().getItemInOffHand());
    }

    private static void reparar(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            item.setItemMeta(meta);
        }
    }

    public static String formatarNomeMaterial(Material material) {
        if (material == null) return "";

        String nome = material.name();  // Ex: DIAMOND_SWORD
        nome = nome.toLowerCase().replace('_', ' ');

        StringBuilder nomeFormatado = new StringBuilder();
        for (String palavra : nome.split(" ")) {
            if (palavra.isEmpty()) continue;
            nomeFormatado.append(Character.toUpperCase(palavra.charAt(0)))
                    .append(palavra.substring(1))
                    .append(" ");
        }
        return nomeFormatado.toString().trim();
    }

    // Método que usa esse formatarNomeMaterial:
    public static Map<String, Object> serializeItemComNome(ItemStack item, String nomeProtecao) {
        if (item == null) return null;

        // Formata o nome do material: Exemplo "Diamond Sword"
        String nomeMaterial = formatarNomeMaterial(item.getType());

        // Adiciona cor branca ao nome do material e mantém o nomeProtecao colorido
        String nomeFinal = ChatColor.WHITE + nomeMaterial + " " + nomeProtecao;

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nomeFinal);  // Define o nome colorido
            clone.setItemMeta(meta);
        }
        return clone.serialize();
    }

    // Salva kit do jogador no itens.yml via ItensConfig renomeando itens com nome de proteção colorido
    public static void setItensArena(Player player) {
        // Puxa nome da proteção da config, aceita cores com &
        String nomeProtecaoRaw = ConfigManager.getConfig().getString("protection-items.name", "&cARENA");
        String nomeProtecao = ChatColor.translateAlternateColorCodes('&', nomeProtecaoRaw);

        Map<String, Object> kitMap = new LinkedHashMap<>();

        // Inventário principal (slots 0-35)
        List<Map<String, Object>> inventario = new ArrayList<>();
        ItemStack[] invContents = player.getInventory().getContents();
        for (int slot = 0; slot < invContents.length; slot++) {
            ItemStack item = invContents[slot];
            Map<String, Object> slotMap = new LinkedHashMap<>();
            slotMap.put("slot", slot);
            slotMap.put("item", item == null ? null : serializeItemComNome(item, nomeProtecao));
            inventario.add(slotMap);
        }

        // Armadura (capacete, peitoral, calça, bota - slots 0-3)
        List<Map<String, Object>> armadura = new ArrayList<>();
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (int slot = 0; slot < armorContents.length; slot++) {
            ItemStack item = armorContents[slot];
            Map<String, Object> slotMap = new LinkedHashMap<>();
            slotMap.put("slot", slot);
            slotMap.put("item", item == null ? null : serializeItemComNome(item, nomeProtecao));
            armadura.add(slotMap);
        }

        // Item off-hand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        Map<String, Object> offhandMap = (offhand == null) ? null : serializeItemComNome(offhand, nomeProtecao);

        // Monta mapa final para salvar no config
        kitMap.put("inventario", inventario);
        kitMap.put("armadura", armadura);
        kitMap.put("offhand", offhandMap);

        // Salva no arquivo usando FileConfiguration do ItensConfig
        getItensConfig().set("kit", kitMap);
        saveItensConfig();

        player.sendMessage(ConfigManager.getMensagem("kit_salvo_sucesso"));
        ItensConfig.loadItensConfig();
    }

    public static void limparKitArena() {
        FileConfiguration config = getItensConfig();
        config.set("kit", null); // Remove toda a seção "kit"
        saveItensConfig();       // Salva no disco

        Main.get().getLogger().info(ConfigManager.getMensagem("kit_limpo"));
    }

    // Aplica o kit salvo no itens.yml ao jogador (inventário, armadura e off-hand)
    @SuppressWarnings("unchecked")
    public static void aplicarKit(Player player) {
        FileConfiguration config = getItensConfig();

        if (!config.contains("kit")) {
            player.sendMessage(ConfigManager.getMensagem("kit_nao_salvo"));
            return;
        }

        ConfigurationSection kitSection = config.getConfigurationSection("kit");
        if (kitSection == null) {
            player.sendMessage(ConfigManager.getMensagem("erro_ao_carregar_kit"));
            return;
        }

        PlayerInventory inv = player.getInventory();

        // Limpa o inventário
        inv.clear();
        inv.setArmorContents(null);
        inv.setItemInOffHand(null);

        // Carregar inventário principal (slots 0-35)
        ItemStack[] contents = new ItemStack[36];
        List<?> inventarioList = kitSection.getMapList("inventario");

        if (inventarioList != null) {
            for (Object obj : inventarioList) {
                if (obj instanceof Map<?, ?> slotMap) {
                    Object slotObj = slotMap.get("slot");
                    Object itemObj = slotMap.get("item");

                    if (!(slotObj instanceof Integer slot)) continue;

                    if (slot < 0 || slot >= contents.length) {
                        player.sendMessage("§cSlot inválido no inventário: " + slot);
                        continue;
                    }

                    if (itemObj instanceof Map<?, ?> itemMap) {
                        try {
                            contents[slot] = ItemStack.deserialize((Map<String, Object>) itemMap);
                        } catch (Exception e) {
                            player.sendMessage("§cErro ao carregar item no slot " + slot);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // Carregar armadura (0 = boots, 1 = leggings, 2 = chestplate, 3 = helmet)
        ItemStack[] armor = new ItemStack[4];
        List<?> armorList = kitSection.getMapList("armadura");

        if (armorList != null) {
            for (Object obj : armorList) {
                if (obj instanceof Map<?, ?> slotMap) {
                    Object slotObj = slotMap.get("slot");
                    Object itemObj = slotMap.get("item");

                    if (!(slotObj instanceof Integer slot)) continue;

                    if (slot < 0 || slot >= armor.length) {
                        player.sendMessage("§cSlot inválido na armadura: " + slot);
                        continue;
                    }

                    if (itemObj instanceof Map<?, ?> itemMap) {
                        try {
                            armor[slot] = ItemStack.deserialize((Map<String, Object>) itemMap);
                        } catch (Exception e) {
                            player.sendMessage("§cErro ao carregar armadura no slot " + slot);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // Carregar offhand
        ItemStack offhand = null;
        Object offhandObj = kitSection.get("offhand");

        if (offhandObj instanceof Map<?, ?> offhandMap) {
            try {
                offhand = ItemStack.deserialize((Map<String, Object>) offhandMap);
            } catch (Exception e) {
                player.sendMessage(ConfigManager.getMensagem("erro_ao_carregar_item_offhand"));
                e.printStackTrace();
            }
        }

        // Aplicar tudo
        inv.setContents(contents);
        inv.setArmorContents(armor);
        inv.setItemInOffHand(offhand);

        player.sendMessage(ConfigManager.getMensagem("kit_aplicado_sucesso"));
    }

}
