package br.trcraft.arena.comandos;

import br.trcraft.arena.Eventos.ItemIntegrityFixer;
import br.trcraft.arena.Main;
import br.trcraft.arena.Utils.ConfigManager;
import br.trcraft.arena.Utils.ItensArena;
import br.trcraft.arena.Utils.ItensConfig;
import br.trcraft.arena.Utils.MySQLAPI;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ArenaCommand implements CommandExecutor {

    private final Main plugin;

    public ArenaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfig().getString("mensagens.nao_esta_na_arena", "§6§lARENA §cApenas jogadores podem usar este comando."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "entrar" -> {
                if (!player.hasPermission("arena.use")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                entrarNaArena(player);
            }

            case "sair", "abandonar" -> {
                if (!player.hasPermission("arena.use")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                ItensArena.sairDaArena(player);
            }

            case "setentrada" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                plugin.setArenaLocation("ARENA", player.getLocation());
                player.sendMessage(plugin.getConfig().getString("mensagens.setou_entrada", "§a✔ Você setou a entrada da Arena!"));
            }

            case "setsaida" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                plugin.setArenaLocation("SAIR", player.getLocation());
                player.sendMessage(plugin.getConfig().getString("mensagens.setou_saida", "§a✔ Você setou a saída da Arena!"));
            }

            case "setitens" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }

                if (ItensConfig.getItensConfig().get("kit") != null) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.kit_existente", "§eJá existe um kit salvo. Use §a/arena limparkit §eantes de salvar um novo kit."));
                    return true;
                }

                ItensArena.setItensArena(player);
            }

            case "limparkit" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                ItensConfig.limparKitArena();
                ItensConfig.saveItensConfig();
                player.sendMessage(plugin.getConfig().getString("mensagens.kit_limpo", "§a✔ Kit da arena removido com sucesso do itens.yml!"));
            }

            case "top" -> {
                if (!player.hasPermission("arena.use")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                showTopKills(player);
            }

            case "reload" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }

                plugin.reloadConfig();
                ConfigManager.reloadConfig();
                plugin.loadArenasConfig();
                ItensConfig.loadItensConfig();

                if (plugin.itemIntegrityFixer != null) {
                    HandlerList.unregisterAll(plugin.itemIntegrityFixer);
                }

                plugin.itemIntegrityFixer = new ItemIntegrityFixer(plugin.getConfig());
                Bukkit.getPluginManager().registerEvents(plugin.itemIntegrityFixer, plugin);

                player.sendMessage(plugin.getConfig().getString("mensagens.reload_sucesso", "§a✔ Configuração da Arena recarregada com sucesso!"));
            }

            case "camarote" -> {
                if (!player.hasPermission("arena.use")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }

                if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
                    ItensArena.sairDoCamarote(player);
                } else {
                    ItensArena.entrarNoCamarote(player);
                }
            }

            case "lista" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }

                ItensArena.listarTodosJogadoresArenaECamarote(sender);
                player.sendMessage(plugin.getConfig().getString("mensagens.lista_camarote_sucesso", "§a✔ Todos os jogadores no camarote foram listados no console."));
            }

            case "setcamarote" -> {
                if (!player.hasPermission("arena.admin")) {
                    player.sendMessage(plugin.getConfig().getString("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando."));
                    return true;
                }
                plugin.setCamaroteLocation(player.getLocation());
                player.sendMessage(plugin.getConfig().getString("mensagens.setou_camarote", "§a✔ Você setou a localização do camarote!"));
            }

            default -> {
                player.sendMessage("§cSubcomando inválido.");
                sendHelp(player);
            }
        }

        return true;
    }

    private void entrarNaArena(Player player) {
        if (ItensArena.isInArena(player)) {
            player.sendMessage(plugin.getConfig().getString("mensagens.ja_na_arena", "§6§lARENA §cVocê já está na arena!"));
            return;
        }
        ItensArena.entrarNaArena(player);
    }

    private void showTopKills(Player player) {
        List<MySQLAPI.PlayerKill> topKills = MySQLAPI.getCachedTop10Kills();

        player.sendMessage("§d§m------------------------------------------");
        player.sendMessage("          §6§lTOP 10 KILLS ARENA");
        player.sendMessage("");

        if (topKills.isEmpty()) {
            player.sendMessage("§c» §eNenhum registro encontrado.");
        } else {
            int pos = 1;
            for (MySQLAPI.PlayerKill pk : topKills) {
                String medal = switch (pos) {
                    case 1 -> "§6🥇";
                    case 2 -> "§7🥈";
                    case 3 -> "§f🥉";
                    default -> "§f•";
                };

                String playerUUID = pk.getPlayerUUID();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUUID));
                String displayName = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerUUID;

                player.sendMessage(medal + " §b" + pos + " §f" + displayName + " §7- §c" + pk.getKills() + " kill" + (pk.getKills() > 1 ? "s" : ""));
                pos++;
            }
        }

        player.sendMessage("");
        player.sendMessage("§d§m------------------------------------------");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("       §6§l»» §3§lARENA §6§l««");
        sender.sendMessage("");
        sender.sendMessage("§a§l✔ §e/arena entrar §7- §fEntrar na §eArena");
        sender.sendMessage("§a§l✔ §e/arena sair §7- §fSair da §eArena");
        sender.sendMessage("§a§l✔ §e/arena camarote §7- §fEntrar no camarote");
        sender.sendMessage("§a§l✔ §e/arena lista §7- §fListar jogadoes no camarote é na arena");
        sender.sendMessage("§a§l✔ §e/arena top §7- §fMostrar ranking de kills");

        if (!(sender instanceof Player) || sender.hasPermission("arena.admin")) {
            sender.sendMessage("§a§l✔ §e/arena reload §7- §fRecarregar configuração §7(§3admin§7)");
            sender.sendMessage("§a§l✔ §e/arena setentrada §7- §fSetar a entrada da Arena §7(§3admin§7)");
            sender.sendMessage("§a§l✔ §e/arena setsaida §7- §fSetar a saída da Arena §7(§3admin§7)");
            sender.sendMessage("§a§l✔ §e/arena setitens §7- §fSalvar kit atual como kit da Arena §7(§3admin§7)");
            sender.sendMessage("§a§l✔ §e/arena limparkit §7- §fRemover kit salvo da Arena §7(§3admin§7)");
            sender.sendMessage("§a§l✔ §e/arena setcamarote §7- §fSetar a localização do camarote §7(§3admin§7)");
        }

        sender.sendMessage("");
    }
}
