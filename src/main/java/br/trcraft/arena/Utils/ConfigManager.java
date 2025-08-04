package br.trcraft.arena.Utils;

import br.trcraft.arena.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {

    private static FileConfiguration config;
    private static File configFile;

    private static final Main plugin = Main.get();

    // Variável para guardar o nome do item de proteção já formatado
    private static String currentProtectionItemName;

    // Inicializa e carrega a config na memória
    public static void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Atualiza a variável do nome do item de proteção
        reloadProtectionItemName();
    }

    // Salva a config na memória no disco
    public static void saveConfig() {
        if (config == null || configFile == null) return;
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar config.yml");
            e.printStackTrace();
        }
    }

    // Recarrega a config da memória do arquivo no disco
    public static void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        checkAndFixConfig();
        saveConfig(); // Salva no configFile corretamente
    }


    // Atualiza a variável interna do nome do item de proteção
    public static void reloadProtectionItemName() {
        String raw = getConfig().getString("protection-items.name", "&e&lARENA");
        currentProtectionItemName = org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    // Retorna o nome atual do item de proteção formatado (cacheado)
    public static String getCurrentProtectionItemName() {
        if (currentProtectionItemName == null) {
            reloadProtectionItemName();
        }
        return currentProtectionItemName;
    }

    // Retorna a config carregada
    public static FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    // Método prático para pegar o nome do item de proteção diretamente da config
    public static String getProtectionItemName() {
        String raw = getConfig().getString("protection-items.name", "&e&lARENA");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    // Pega o tempo de espera para kills (em milissegundos)
    public static long getTempoEsperaKillMillis() {
        return getConfig().getLong("configuracoes.tempo_espera_kill_millis", 180000);
    }

    // Pega uma mensagem customizada da config
    public static String getMensagem(String chave) {
        String raw = getConfig().getString("mensagens." + chave, "§cMensagem não definida: " + chave);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    // Pega a lista de comandos liberados da config
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

        // Mensagens (exemplo com algumas, deve repetir para todas)
        if (!config.isSet("mensagens.ip_invalido")) {
            config.set("mensagens.ip_invalido", "§6§lARENA §cIP inválido, kill não foi contada.");
        }
        if (!config.isSet("mensagens.mesmo_ip")) {
            config.set("mensagens.mesmo_ip", "§6§lARENA §cVocê não pode ganhar kills de jogadores com mesmo IP.");
        }
        if (!config.isSet("mensagens.kill_valida")) {
            config.set("mensagens.kill_valida", "§a§l+1 kill §7(kit restaurado)");
        }
        // Repita para todas as mensagens da sua lista completa...

        // Comandos liberados
        if (!config.isSet("comandos_liberados")) {
            List<String> comandosPadrao = Arrays.asList("/arena sair", "/arena lista", "/tell", "/r", "/ping", "/report", "/g", "/.");
            config.set("comandos_liberados", comandosPadrao);
        }

        // Após corrigir tudo, salvar a config para atualizar o arquivo config.yml
        plugin.saveConfig();
    }

}
