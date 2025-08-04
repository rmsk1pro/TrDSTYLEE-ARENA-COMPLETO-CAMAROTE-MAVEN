package br.trcraft.arena.Eventos;

import br.trcraft.arena.Main;
import br.trcraft.arena.Utils.ConfigManager;
import br.trcraft.arena.Utils.ItensArena;
import br.trcraft.arena.Utils.MySQLAPI;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.*;

public class Eventos implements Listener {

    private final Main plugin;

    // Construtor que recebe o plugin
    public Eventos(Main plugin) {
        this.plugin = plugin;
    }

    // Map para controlar o tempo entre kills para cada jogador (killer -> (morto -> timestamp))
    private final Map<UUID, Map<UUID, Long>> ultimasKills = new HashMap<>();
    private final Map<UUID, Long> lastPickupMessageTimes = new HashMap<>();


    @EventHandler(priority = EventPriority.LOWEST)
    public void aoMorrer(PlayerDeathEvent event) {
        Player morto = event.getEntity();

        if (!ItensArena.isInArena(morto)) return;

        event.getDrops().clear();

        Player killer = morto.getKiller();

        if (killer != null && killer != morto && ItensArena.isInArena(killer)) {
            UUID killerId = killer.getUniqueId();
            UUID mortoId = morto.getUniqueId();

            String ipKiller = getIP(killer);
            String ipMorto = getIP(morto);

            if (ipKiller == null || ipMorto == null) {
                killer.sendMessage(ConfigManager.getMensagem("ip_invalido"));
            } else if (ipKiller.equals(ipMorto)) {
                killer.sendMessage(ConfigManager.getMensagem("mesmo_ip"));
            } else {
                long agora = System.currentTimeMillis();
                Map<UUID, Long> mortes = ultimasKills.computeIfAbsent(killerId, k -> new HashMap<>());
                long ultimaKill = mortes.getOrDefault(mortoId, 0L);

                if (agora - ultimaKill >= ConfigManager.getTempoEsperaKillMillis()) {
                    mortes.put(mortoId, agora);

                    ItensArena.resetarKitCompleto(killer);
                    ItensArena.repararItens(killer);
                    ItensArena.renovarMSGPosKill(killer);
                    MySQLAPI.addKillAsync(killerId, 1);

                    killer.sendMessage(ConfigManager.getMensagem("kill_valida"));
                } else {
                    killer.sendMessage(ConfigManager.getMensagem("kill_invalida"));
                }
            }
        }

        morto.sendMessage(ConfigManager.getMensagem("morte_jogador"));
        ItensArena.sairDaArena(morto, false);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void aoTeleportar(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!ItensArena.isInArena(player)) return;

        Location destino = event.getTo();
        Location saida = Main.get().getArenaLocation("SAIR");

        if (saida != null && destino != null && isSameLocation(destino, saida)) {
            return; // Permite teleporte para saída da arena
        }

        event.setCancelled(true);
        player.sendMessage(ConfigManager.getMensagem("teleporte_bloqueado"));
    }

    private boolean isSameLocation(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (!loc1.getWorld().equals(loc2.getWorld())) return false;
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoSair(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.isInArena(player)) {
            ItensArena.sairDaArena(player, false);
        }

        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
            ItensArena.arenaCamarote.remove(player.getUniqueId());
            player.sendMessage(ConfigManager.getMensagem("camarote_removido_sair"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoSerKickado(PlayerKickEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.isInArena(player)) {
            ItensArena.sairDaArena(player, false);
        }

        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
            ItensArena.arenaCamarote.remove(player.getUniqueId());
            // Não envia mensagem pois o jogador está sendo desconectado
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void aoTrocarMundo(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.isInArena(player)) {
            ItensArena.sairDaArena(player, false);
        }

        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
            ItensArena.arenaCamarote.remove(player.getUniqueId());
            player.sendMessage(ConfigManager.getMensagem("camarote_removido_trocar_mundo"));
        }

        forcarSurvivalSeEspectador(player, "trocar de mundo");
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void aoEntrar(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.isInArena(player) && player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(ConfigManager.getMensagem("modo_survival_entrada"));
        }

        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(ConfigManager.getMensagem("modo_survival_camarote_reconectar"));
        }

        forcarSurvivalSeEspectador(player, "reconectar");
    }

    private void forcarSurvivalSeEspectador(Player player, String motivo) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            String msg = ConfigManager.getMensagem("modo_survival_forcado").replace("{motivo}", motivo);
            player.sendMessage(msg);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTomarDano(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (ItensArena.isInArena(player) && ItensArena.sairTasks.containsKey(player.getUniqueId())) {
            ItensArena.cancelarContagemSaida(player);
            player.sendMessage(ConfigManager.getMensagem("saida_cancelada_dano_recebido"));
            player.sendTitle(ConfigManager.getMensagem("saida_titulo"), ConfigManager.getMensagem("saida_cancelada_dano_recebido"), 10, 40, 10);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCausarDano(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (ItensArena.isInArena(player) && ItensArena.sairTasks.containsKey(player.getUniqueId())) {
            ItensArena.cancelarContagemSaida(player);
            player.sendMessage(ConfigManager.getMensagem("saida_cancelada_dano_causado"));
            player.sendTitle(ConfigManager.getMensagem("saida_titulo"), ConfigManager.getMensagem("saida_cancelada_dano_causado"), 10, 40, 10);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoDropar(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (ItensArena.isInArena(player)) {
            event.setCancelled(true);
            player.sendMessage(ConfigManager.getMensagem("drop_bloqueado"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoExecutarComando(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!ItensArena.isInArena(player)) return;

        String message = event.getMessage().toLowerCase().trim();
        List<String> comandosLiberados = ConfigManager.getComandosLiberados();

        boolean permitido = comandosLiberados.stream().anyMatch(cmd -> {
            if (cmd.endsWith("*")) {
                String prefixo = cmd.substring(0, cmd.length() - 1);
                return message.startsWith(prefixo);
            }
            return message.equals(cmd);
        });

        if (permitido) return;

        event.setCancelled(true);
        player.sendMessage(ConfigManager.getMensagem("comando_bloqueado"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoComandoEspecial(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

        if (message.equals("/arena:")) {
            event.setCancelled(true);
            Bukkit.dispatchCommand(player, "arena");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoMover(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {

            Location currentLocation = event.getTo();
            Location camaroteLocation = plugin.getCamaroteLocation();

            if (camaroteLocation == null) {
                return;
            }
            double distance = currentLocation.distance(camaroteLocation);

            if (distance > 100) {
                player.teleport(camaroteLocation);
                player.sendMessage(ConfigManager.getMensagem("camarote_teleportado_de_volta"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoDroparItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.arenaCamarote.contains(player.getUniqueId()) || player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
                player.sendMessage(ConfigManager.getMensagem("camarote_drop_bloqueado"));
            }
            else if (player.getGameMode() == GameMode.SPECTATOR) {
                player.sendMessage(ConfigManager.getMensagem("espectador_drop_bloqueado"));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void aoPegarItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        if (ItensArena.arenaCamarote.contains(player.getUniqueId())) {
            event.setCancelled(true);

            long currentTime = System.currentTimeMillis();
            long lastTime = lastPickupMessageTimes.getOrDefault(player.getUniqueId(), 0L);
            long delay = 5000;

            if (currentTime - lastTime > delay) {
                lastPickupMessageTimes.put(player.getUniqueId(), currentTime);

                player.sendMessage(ConfigManager.getMensagem("camarote_pickup_bloqueado"));
            }
        }
    }

    private String getIP(Player player) {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) return null;
        return player.getAddress().getAddress().getHostAddress();
    }
}
