package br.trcraft.arena.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MensagensArena {

    public static void enviarMensagemEntrada(Player player) {
        // Pega as mensagens da config
        String titulo = ConfigManager.getMensagem("entrada_titulo");
        String subtitulo = ConfigManager.getMensagem("entrada_subtitulo");
        String linha1 = ConfigManager.getMensagem("entrada_chat_linha1").replace("{player}", player.getName());
        String linha2 = ConfigManager.getMensagem("entrada_chat_linha2");
        String broadcast = ConfigManager.getMensagem("broadcast_entrada").replace("{player}", player.getName());
        String somNome = ConfigManager.getMensagem("som_entrada");

        // Envia título e subtítulo
        player.sendTitle(titulo, subtitulo, 10, 60, 20);

        // Toca som se válido
        try {
            @SuppressWarnings("deprecation")
            Sound som = Sound.valueOf(somNome);
            player.playSound(player.getLocation(), som, 1.0f, 1.0f);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Som inválido ou não definido, ignora
        }

        // Envia mensagens no chat para o próprio jogador
        player.sendMessage("");
        player.sendMessage(linha1);
        player.sendMessage(linha2);
        player.sendMessage("");

        // Envia broadcast para todos online
        Bukkit.getServer().broadcastMessage(broadcast);
    }
}
