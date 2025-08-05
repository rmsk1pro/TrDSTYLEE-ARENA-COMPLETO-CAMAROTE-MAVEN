package br.trcraft.arena.Utils;

import br.trcraft.arena.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.Arrays;
import java.util.List;

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
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        checkAndFixConfig();
        saveConfig();
        reloadProtectionItemName();
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
        if (config == null) {
            loadConfig();
        }

        // MySQL
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

        // Protection items
        if (!config.isSet("protection-items.name")) {
            config.set("protection-items.name", "&e&lARENA");
        }

        // Configurações gerais
        if (!config.isSet("configuracoes.tempo_espera_kill_millis")) {
            config.set("configuracoes.tempo_espera_kill_millis", 180000);
        }

        // Mensagens - verifique e defina padrão para todas as mensagens abaixo
        if (!config.isSet("mensagens.ip_invalido")) {
            config.set("mensagens.ip_invalido", "§6§lARENA §cIP inválido, kill não foi contada.");
        }
        if (!config.isSet("mensagens.mesmo_ip")) {
            config.set("mensagens.mesmo_ip", "§6§lARENA §cVocê não pode ganhar kills de jogadores com mesmo IP.");
        }
        if (!config.isSet("mensagens.kill_valida")) {
            config.set("mensagens.kill_valida", "§a§l+1 kill §7(kit restaurado)");
        }
        if (!config.isSet("mensagens.kill_invalida")) {
            config.set("mensagens.kill_invalida", "§6§lARENA §eVocê já matou esse jogador recentemente. Kill não foi contada.");
        }
        if (!config.isSet("mensagens.morreu_na_arena")) {
            config.set("mensagens.morreu_na_arena", "§6§lARENA §cVocê morreu na arena, seus itens foram limpos.");
        }
        if (!config.isSet("mensagens.morte_jogador")) {
            config.set("mensagens.morte_jogador", "§6§lARENA §cVocê morreu na arena!");
        }
        if (!config.isSet("mensagens.inventario_nao_vazio")) {
            config.set("mensagens.inventario_nao_vazio", "§6§lARENA §cVocê só pode entrar na arena com inventário vazio!");
        }
        if (!config.isSet("mensagens.localizacao_nao_configurada")) {
            config.set("mensagens.localizacao_nao_configurada", "§6§lARENA §cLocalização da arena não configurada! Contate um administrador.");
        }
        if (!config.isSet("mensagens.camarote_removido_sair")) {
            config.set("mensagens.camarote_removido_sair", "§6§lARENA §cVocê foi removido do camarote ao sair do servidor.");
        }
        if (!config.isSet("mensagens.camarote_removido_trocar_mundo")) {
            config.set("mensagens.camarote_removido_trocar_mundo", "§6§lARENA §cVocê foi removido do camarote ao trocar de mundo.");
        }
        if (!config.isSet("mensagens.modo_survival_entrada")) {
            config.set("mensagens.modo_survival_entrada", "§6§lARENA §cVocê entrou e foi colocado no modo survival.");
        }
        if (!config.isSet("mensagens.modo_survival_camarote_reconectar")) {
            config.set("mensagens.modo_survival_camarote_reconectar", "§6§lARENA §cVocê foi colocado em modo survival após reconectar ao camarote.");
        }
        if (!config.isSet("mensagens.modo_survival_forcado")) {
            config.set("mensagens.modo_survival_forcado", "§6§lARENA §cVocê foi colocado no modo survival após {motivo}.");
        }
        if (!config.isSet("mensagens.camarote_teleportado_de_volta")) {
            config.set("mensagens.camarote_teleportado_de_volta", "§6§lARENA §cVocê foi teleportado de volta ao camarote, pois ultrapassou a distância de 100 blocos.");
        }
        if (!config.isSet("mensagens.camarote_drop_bloqueado")) {
            config.set("mensagens.camarote_drop_bloqueado", "§6§lARENA §cVocê não pode dropar itens enquanto estiver no camarote.");
        }
        if (!config.isSet("mensagens.espectador_drop_bloqueado")) {
            config.set("mensagens.espectador_drop_bloqueado", "§6§lARENA §cVocê não pode dropar itens enquanto estiver em modo espectador.");
        }
        if (!config.isSet("mensagens.camarote_pickup_bloqueado")) {
            config.set("mensagens.camarote_pickup_bloqueado", "§6§lARENA §cVocê não pode pegar itens enquanto estiver no camarote.");
        }
        if (!config.isSet("mensagens.sem_permissao")) {
            config.set("mensagens.sem_permissao", "§6§lARENA §cVocê não tem permissão para usar este comando.");
        }
        if (!config.isSet("mensagens.setou_entrada")) {
            config.set("mensagens.setou_entrada", "§a✔ Você setou a entrada da Arena!");
        }
        if (!config.isSet("mensagens.setou_saida")) {
            config.set("mensagens.setou_saida", "§a✔ Você setou a saída da Arena!");
        }
        if (!config.isSet("mensagens.kit_existente")) {
            config.set("mensagens.kit_existente", "§eJá existe um kit salvo. Use §a/arena limparkit §eantes de salvar um novo kit.");
        }
        if (!config.isSet("mensagens.kit_limpo")) {
            config.set("mensagens.kit_limpo", "§a✔ Kit da arena removido com sucesso do itens.yml!");
        }
        if (!config.isSet("mensagens.reload_sucesso")) {
            config.set("mensagens.reload_sucesso", "§a✔ Configuração da Arena recarregada com sucesso!");
        }
        if (!config.isSet("mensagens.lista_camarote_sucesso")) {
            config.set("mensagens.lista_camarote_sucesso", "§a✔ Todos os jogadores no camarote foram listados no console.");
        }
        if (!config.isSet("mensagens.setou_camarote")) {
            config.set("mensagens.setou_camarote", "§a✔ Você setou a localização do camarote!");
        }
        if (!config.isSet("mensagens.nao_esta_na_arena")) {
            config.set("mensagens.nao_esta_na_arena", "§6§lARENA §cApenas jogadores podem usar este comando.");
        }
        if (!config.isSet("mensagens.ja_na_arena")) {
            config.set("mensagens.ja_na_arena", "§6§lARENA §cVocê já está na arena!");
        }
        if (!config.isSet("mensagens.item_protegido_drop_fora")) {
            config.set("mensagens.item_protegido_drop_fora", "§6§lARENA §cVocê não pode dropar itens protegidos fora da arena.");
        }
        if (!config.isSet("mensagens.item_protegido_equipar_fora")) {
            config.set("mensagens.item_protegido_equipar_fora", "§6§lARENA §cVocê não pode equipar itens protegidos fora da arena.");
        }
        if (!config.isSet("mensagens.item_protegido_manipular_fora")) {
            config.set("mensagens.item_protegido_manipular_fora", "§6§lARENA §cVocê não pode manipular itens protegidos fora da arena.");
        }
        if (!config.isSet("mensagens.item_protegido_arrastar_fora")) {
            config.set("mensagens.item_protegido_arrastar_fora", "§6§lARENA §cVocê não pode arrastar itens protegidos fora da arena.");
        }
        if (!config.isSet("mensagens.item_protegido_trocar_maos_fora")) {
            config.set("mensagens.item_protegido_trocar_maos_fora", "§6§lARENA §cVocê não pode trocar itens protegidos de mão fora da arena.");
        }
        if (!config.isSet("mensagens.item_protegido_pegar_fora")) {
            config.set("mensagens.item_protegido_pegar_fora", "§6§lARENA §cVocê não pode pegar itens protegidos fora da arena.");
        }
        if (!config.isSet("mensagens.item_protegido_usar_fora")) {
            config.set("mensagens.item_protegido_usar_fora", "§6§lARENA §cVocê não pode usar itens protegidos fora da arena.");
        }
        if (!config.isSet("mensagens.saida_contagem_ja_ativa")) {
            config.set("mensagens.saida_contagem_ja_ativa", "§6§lARENA §cVocê já está saindo da arena!");
        }
        if (!config.isSet("mensagens.saida_contagem_iniciada")) {
            config.set("mensagens.saida_contagem_iniciada", "§6§lARENA §eSaída da arena iniciada. Aguarde {segundos} segundos e não tome dano para não cancelar!");
        }
        if (!config.isSet("mensagens.saiu_da_arena")) {
            config.set("mensagens.saiu_da_arena", "§a✔ Você saiu da Arena!");
        }
        if (!config.isSet("mensagens.saida_titulo")) {
            config.set("mensagens.saida_titulo", "§6Saindo da Arena...");
        }
        if (!config.isSet("mensagens.saida_subtitulo")) {
            config.set("mensagens.saida_subtitulo", "§e{segundos} segundo(s) restantes");
        }
        if (!config.isSet("mensagens.saida_cancelada_dano_recebido")) {
            config.set("mensagens.saida_cancelada_dano_recebido", "§6§lARENA §cSaída da arena cancelada porque você tomou dano!");
        }
        if (!config.isSet("mensagens.saida_cancelada_dano_causado")) {
            config.set("mensagens.saida_cancelada_dano_causado", "§6§lARENA §cSaída da arena cancelada porque você causou dano!");
        }
        if (!config.isSet("mensagens.teleporte_bloqueado")) {
            config.set("mensagens.teleporte_bloqueado", "§6§lARENA §cVocê não pode teleportar na arena!");
        }
        if (!config.isSet("mensagens.drop_bloqueado")) {
            config.set("mensagens.drop_bloqueado", "§6§lARENA §cVocê não pode dropar itens na arena");
        }
        if (!config.isSet("mensagens.comando_bloqueado")) {
            config.set("mensagens.comando_bloqueado", "§6§lARENA §c§l✖ §fComando bloqueado durante a partida!");
        }
        if (!config.isSet("mensagens.bonus_pos_kill_titulo")) {
            config.set("mensagens.bonus_pos_kill_titulo", "§6➤ Bônus de Vida Completa por kill!");
        }
        if (!config.isSet("mensagens.bonus_pos_kill_subtitulo")) {
            config.set("mensagens.bonus_pos_kill_subtitulo", "§7Recuperando ❤ e efeitos...");
        }
        if (!config.isSet("mensagens.bonus_pos_kill_rodape")) {
            config.set("mensagens.bonus_pos_kill_rodape", "§f❤ HP 100% ▎ Saturação 100% ▎ Efeitos Renovados!");
        }
        if (!config.isSet("mensagens.entrada_titulo")) {
            config.set("mensagens.entrada_titulo", "§6⚔ Arena ⚔");
        }
        if (!config.isSet("mensagens.entrada_subtitulo")) {
            config.set("mensagens.entrada_subtitulo", "§7Prepare-se para a batalha!");
        }
        if (!config.isSet("mensagens.entrada_chat_linha1")) {
            config.set("mensagens.entrada_chat_linha1", "§6§l➤ §eBem-vindo à §6Arena§e, §f{player}§e!");
        }
        if (!config.isSet("mensagens.entrada_chat_linha2")) {
            config.set("mensagens.entrada_chat_linha2", "§7Use seu talento para se destacar e §cmostrar quem manda!");
        }
        if (!config.isSet("mensagens.broadcast_entrada")) {
            config.set("mensagens.broadcast_entrada", "§6§l[Arena] §fO jogador §e{player} §fentrou na arena! Prepare-se para a batalha!");
        }
        if (!config.isSet("mensagens.som_entrada")) {
            config.set("mensagens.som_entrada", "ENTITY_WITHER_SPAWN");
        }
        if (!config.isSet("mensagens.arena_no_camarote_entrada")) {
            config.set("mensagens.arena_no_camarote_entrada", "§6§lARENA §cVocê não pode entrar na arena enquanto estiver no camarote.");
        }
        if (!config.isSet("mensagens.kit_nao_configurado")) {
            config.set("mensagens.kit_nao_configurado", "§6§lARENA §cKit da arena não configurado. Configure antes de entrar.");
        }
        if (!config.isSet("mensagens.localizacao_saida_nao_configurada")) {
            config.set("mensagens.localizacao_saida_nao_configurada", "§6§lARENA §cLocalização de saída da arena não configurada. Contate um administrador.");
        }
        if (!config.isSet("mensagens.nao_esta_no_camarote")) {
            config.set("mensagens.nao_esta_no_camarote", "§6§lARENA §cVocê não está no camarote.");
        }
        if (!config.isSet("mensagens.localizacao_camarote_nao_configurada")) {
            config.set("mensagens.localizacao_camarote_nao_configurada", "§6§lARENA §cLocalização do camarote não configurada. Contate um administrador.");
        }
        if (!config.isSet("mensagens.entrou_no_camarote")) {
            config.set("mensagens.entrou_no_camarote", "§6§lARENA §aVocê entrou no camarote.");
        }
        if (!config.isSet("mensagens.saiu_do_camarote")) {
            config.set("mensagens.saiu_do_camarote", "§6§lARENA §aVocê saiu do camarote.");
        }
        if (!config.isSet("mensagens.arena_proibe_camarote")) {
            config.set("mensagens.arena_proibe_camarote", "§6§lARENA §cVocê não pode entrar no camarote enquanto estiver na arena.");
        }
        if (!config.isSet("mensagens.morto_proibe_camarote")) {
            config.set("mensagens.morto_proibe_camarote", "§6§lARENA §cJogadores mortos não podem entrar no camarote.");
        }
        if (!config.isSet("mensagens.ja_no_camarote")) {
            config.set("mensagens.ja_no_camarote", "§6§lARENA §cVocê já está no camarote.");
        }
        if (!config.isSet("mensagens.kit_salvo_sucesso")) {
            config.set("mensagens.kit_salvo_sucesso", "§a✔ Kit da arena salvo com sucesso!");
        }
        if (!config.isSet("mensagens.kit_nao_salvo")) {
            config.set("mensagens.kit_nao_salvo", "§6§lARENA §cNenhum kit salvo na configuração.");
        }
        if (!config.isSet("mensagens.erro_ao_carregar_kit")) {
            config.set("mensagens.erro_ao_carregar_kit", "§6§lARENA §cErro ao carregar o kit da arena.");
        }
        if (!config.isSet("mensagens.erro_ao_carregar_item_offhand")) {
            config.set("mensagens.erro_ao_carregar_item_offhand", "§6§lARENA §cErro ao carregar item da mão secundária.");
        }
        if (!config.isSet("mensagens.kit_aplicado_sucesso")) {
            config.set("mensagens.kit_aplicado_sucesso", "§a✔ Kit da arena aplicado com sucesso!");
        }

        // Comandos liberados
        if (!config.isSet("comandos_liberados")) {
            List<String> comandosPadrao = Arrays.asList("/arena sair", "/arena lista", "/tell", "/r", "/ping", "/report", "/g", "/.");
            config.set("comandos_liberados", comandosPadrao);
        }
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
