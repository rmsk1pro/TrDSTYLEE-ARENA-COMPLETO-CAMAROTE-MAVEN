package br.trcraft.arena.Utils;

import br.trcraft.arena.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static FileConfiguration config;
    private static File configFile;
    private static final Main plugin = Main.get();
    private static String currentProtectionItemName;

    public static void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        reloadProtectionItemName();
        checkAndFixConfig();
    }

    public static void saveConfig() {
        if (config == null || configFile == null) return;
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar config.yml");
            e.printStackTrace();
        }
    }

    public static void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        checkAndFixConfig();
        saveConfig();
    }

    public static FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static void reloadProtectionItemName() {
        String raw = getConfig().getString("protection-items.name", "&e&lARENA");
        currentProtectionItemName = ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static String getCurrentProtectionItemName() {
        if (currentProtectionItemName == null) {
            reloadProtectionItemName();
        }
        return currentProtectionItemName;
    }

    public static String getProtectionItemName() {
        String raw = getConfig().getString("protection-items.name", "&e&lARENA");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static long getTempoEsperaKillMillis() {
        return getConfig().getLong("configuracoes.tempo_espera_kill_millis", 180000);
    }

    public static String getMensagem(String chave) {
        String raw = getConfig().getString("mensagens." + chave, "§cMensagem não definida: " + chave);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static List<String> getComandosLiberados() {
        return getConfig().getStringList("comandos_liberados");
    }

    public static void checkAndFixConfig() {
        FileConfiguration config = plugin.getConfig();

        // Verificar e setar valores padrão se não existirem
        if (!config.isSet("mysql.enable")) {
            config.set("mysql.enable", false);
        }
        if (!config.isSet("mysql.host")) {
            config.set("mysql.host", "localhost");
        }
        if (!config.isSet("mysql.port")) {
            config.set("mysql.port", 3306);
        }
        if (!config.isSet("mysql.database")) {
            config.set("mysql.database", "arena");
        }
        if (!config.isSet("mysql.user")) {
            config.set("mysql.user", "root");
        }
        if (!config.isSet("mysql.password")) {
            config.set("mysql.password", "");
        }

        if (!config.isSet("protection-items.name")) {
            config.set("protection-items.name", "&e&lARENA");
        }

        if (!config.isSet("configuracoes.tempo_espera_kill_millis")) {
            config.set("configuracoes.tempo_espera_kill_millis", 180000);
        }

        // Mensagens obrigatórias com valores padrão para preenchimento
        Map<String, String> mensagensPadrao = new HashMap<>();
        mensagensPadrao.put("ip_invalido", "§6§lARENA §cIP inválido, kill não foi contada.");
        mensagensPadrao.put("mesmo_ip", "§6§lARENA §cVocê não pode ganhar kills de jogadores com mesmo IP.");
        mensagensPadrao.put("kill_valida", "§a§l+1 kill §7(kit restaurado)");
        mensagensPadrao.put("kill_invalida", "§6§lARENA §eVocê já matou esse jogador recentemente. Kill não foi contada.");
        mensagensPadrao.put("morreu_na_arena", "§6§lARENA §cVocê morreu na arena, seus itens foram limpos.");
        mensagensPadrao.put("morte_jogador", "§6§lARENA §cVocê morreu na arena!");
        mensagensPadrao.put("nao_esta_na_arena", "§6§lARENA §cVocê não está na arena!");
        mensagensPadrao.put("ja_na_arena", "§6§lARENA §cVocê já está na arena!");
        mensagensPadrao.put("inventario_nao_vazio", "§6§lARENA §cVocê só pode entrar na arena com inventário vazio!");
        mensagensPadrao.put("localizacao_nao_configurada", "§6§lARENA §cLocalização da arena não configurada! Contate um administrador.");
        mensagensPadrao.put("camarote_removido_sair", "§6§lARENA §cVocê foi removido do camarote ao sair do servidor.");
        mensagensPadrao.put("camarote_removido_trocar_mundo", "§6§lARENA §cVocê foi removido do camarote ao trocar de mundo.");
        mensagensPadrao.put("modo_survival_entrada", "§6§lARENA §cVocê entrou e foi colocado no modo survival.");
        mensagensPadrao.put("modo_survival_camarote_reconectar", "§6§lARENA §cVocê foi colocado em modo survival após reconectar ao camarote.");
        mensagensPadrao.put("modo_survival_forcado", "§6§lARENA §cVocê foi colocado no modo survival após {motivo}.");
        mensagensPadrao.put("camarote_teleportado_de_volta", "§6§lARENA §cVocê foi teleportado de volta ao camarote, pois ultrapassou a distância de 100 blocos.");
        mensagensPadrao.put("camarote_drop_bloqueado", "§6§lARENA §cVocê não pode dropar itens enquanto estiver no camarote.");
        mensagensPadrao.put("espectador_drop_bloqueado", "§6§lARENA §cVocê não pode dropar itens enquanto estiver em modo espectador.");
        mensagensPadrao.put("camarote_pickup_bloqueado", "§6§lARENA §cVocê não pode pegar itens enquanto estiver no camarote.");
        mensagensPadrao.put("sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando.");
        mensagensPadrao.put("setou_entrada", "§a✔ Você setou a entrada da Arena!");
        mensagensPadrao.put("setou_saida", "§a✔ Você setou a saída da Arena!");
        mensagensPadrao.put("kit_existente", "§eJá existe um kit salvo. Use §a/arena limparkit §eantes de salvar um novo kit.");
        mensagensPadrao.put("kit_limpo", "§a✔ Kit da arena removido com sucesso do itens.yml!");
        mensagensPadrao.put("reload_sucesso", "§a✔ Configuração da Arena recarregada com sucesso!");
        mensagensPadrao.put("lista_camarote_sucesso", "§a✔ Todos os jogadores no camarote foram listados no console.");
        mensagensPadrao.put("setou_camarote", "§a✔ Você setou a localização do camarote!");
        mensagensPadrao.put("item_protegido_drop_fora", "§6§lARENA §cVocê não pode dropar itens protegidos fora da arena.");
        mensagensPadrao.put("item_protegido_equipar_fora", "§6§lARENA §cVocê não pode equipar itens protegidos fora da arena.");
        mensagensPadrao.put("item_protegido_manipular_fora", "§6§lARENA §cVocê não pode manipular itens protegidos fora da arena.");
        mensagensPadrao.put("item_protegido_arrastar_fora", "§6§lARENA §cVocê não pode arrastar itens protegidos fora da arena.");
        mensagensPadrao.put("item_protegido_trocar_maos_fora", "§6§lARENA §cVocê não pode trocar itens protegidos de mão fora da arena.");
        mensagensPadrao.put("item_protegido_pegar_fora", "§6§lARENA §cVocê não pode pegar itens protegidos fora da arena.");
        mensagensPadrao.put("item_protegido_usar_fora", "§6§lARENA §cVocê não pode usar itens protegidos fora da arena.");
        mensagensPadrao.put("saida_contagem_ja_ativa", "§6§lARENA §cVocê já está saindo da arena!");
        mensagensPadrao.put("saida_contagem_iniciada", "§6§lARENA §eSaída da arena iniciada. Aguarde {segundos} segundos e não tome dano para não cancelar!");
        mensagensPadrao.put("saiu_da_arena", "§a✔ Você saiu da Arena!");
        mensagensPadrao.put("saida_titulo", "§6Saindo da Arena...");
        mensagensPadrao.put("saida_subtitulo", "§e{segundos} segundo(s) restantes");
        mensagensPadrao.put("saida_cancelada_dano_recebido", "§6§lARENA §cSaída da arena cancelada porque você tomou dano!");
        mensagensPadrao.put("saida_cancelada_dano_causado", "§6§lARENA §cSaída da arena cancelada porque você causou dano!");
        mensagensPadrao.put("teleporte_bloqueado", "§6§lARENA §cVocê não pode teleportar na arena!");
        mensagensPadrao.put("drop_bloqueado", "§6§lARENA §cVocê não pode dropar itens na arena");
        mensagensPadrao.put("comando_bloqueado", "§6§lARENA §c§l✖ §fComando bloqueado durante a partida!");
        mensagensPadrao.put("bonus_pos_kill_titulo", "§6➤ Bônus de Vida Completa por kill!");
        mensagensPadrao.put("bonus_pos_kill_subtitulo", "§7Recuperando ❤ e efeitos...");
        mensagensPadrao.put("bonus_pos_kill_rodape", "§f❤ HP 100% ▎ Saturação 100% ▎ Efeitos Renovados!");
        mensagensPadrao.put("entrada_titulo", "§6⚔ Arena ⚔");
        mensagensPadrao.put("entrada_subtitulo", "§7Prepare-se para a batalha!");
        mensagensPadrao.put("entrada_chat_linha1", "§6§l➤ §eBem-vindo à §6Arena§e, §f{player}§e!");
        mensagensPadrao.put("entrada_chat_linha2", "§7Use seu talento para se destacar e §cmostrar quem manda!");
        mensagensPadrao.put("broadcast_entrada", "§6§l[Arena] §fO jogador §e{player} §fentrou na arena! Prepare-se para a batalha!");
        mensagensPadrao.put("som_entrada", "ENTITY_WITHER_SPAWN");
        mensagensPadrao.put("arena_no_camarote_entrada", "§6§lARENA §cVocê não pode entrar na arena enquanto estiver no camarote.");
        mensagensPadrao.put("kit_nao_configurado", "§6§lARENA §cKit da arena não configurado. Configure antes de entrar.");
        mensagensPadrao.put("localizacao_saida_nao_configurada", "§6§lARENA §cLocalização de saída da arena não configurada. Contate um administrador.");
        mensagensPadrao.put("nao_esta_no_camarote", "§6§lARENA §cVocê não está no camarote.");
        mensagensPadrao.put("localizacao_camarote_nao_configurada", "§6§lARENA §cLocalização do camarote não configurada. Contate um administrador.");
        mensagensPadrao.put("entrou_no_camarote", "§6§lARENA §aVocê entrou no camarote.");
        mensagensPadrao.put("saiu_do_camarote", "§6§lARENA §aVocê saiu do camarote.");
        mensagensPadrao.put("arena_proibe_camarote", "§6§lARENA §cVocê não pode entrar no camarote enquanto estiver na arena.");
        mensagensPadrao.put("morto_proibe_camarote", "§6§lARENA §cJogadores mortos não podem entrar no camarote.");
        mensagensPadrao.put("ja_no_camarote", "§6§lARENA §cVocê já está no camarote.");
        mensagensPadrao.put("kit_salvo_sucesso", "§a✔ Kit da arena salvo com sucesso!");
        mensagensPadrao.put("kit_nao_salvo", "§6§lARENA §cNenhum kit salvo na configuração.");
        mensagensPadrao.put("erro_ao_carregar_kit", "§6§lARENA §cErro ao carregar o kit da arena.");
        mensagensPadrao.put("erro_ao_carregar_item_offhand", "§6§lARENA §cErro ao carregar item da mão secundária.");

        // Verificar cada mensagem obrigatória
        List<String> faltando = new ArrayList<>();
        for (Map.Entry<String, String> entry : mensagensPadrao.entrySet()) {
            String chave = "mensagens." + entry.getKey();
            if (!config.isSet(chave)) {
                config.set(chave, entry.getValue());
                faltando.add(chave);
            }
        }

        // Logar mensagens faltando para avisar o admin
        if (!faltando.isEmpty()) {
            plugin.getLogger().warning("⚠️ Mensagens faltando na config.yml e adicionadas automaticamente:");
            for (String msg : faltando) {
                plugin.getLogger().warning(" - " + msg);
            }
            plugin.getLogger().warning("Total: " + faltando.size() + " mensagem(ns) ausente(s) corrigida(s).");
        }

        // Comandos liberados padrão
        if (!config.isSet("comandos_liberados")) {
            List<String> comandosPadrao = Arrays.asList("/arena sair", "/arena lista", "/tell", "/r", "/ping", "/report", "/g", "/.");
            config.set("comandos_liberados", comandosPadrao);
        }

        // Salvar as alterações na config.yml
        plugin.saveConfig();
    }


    // Debug no onEnable
    public static void debugMensagens() {
        if (getConfig().isConfigurationSection("mensagens")) {
            plugin.getLogger().info("Mensagens carregadas da config:");
            for (String chave : getConfig().getConfigurationSection("mensagens").getKeys(false)) {
                plugin.getLogger().info(" - mensagens." + chave + " = " + getMensagem(chave));
            }
        } else {
            plugin.getLogger().warning("Seção 'mensagens' não encontrada na config!");
        }
    }
}
