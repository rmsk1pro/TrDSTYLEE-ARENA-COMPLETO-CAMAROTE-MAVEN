package br.trcraft.arena.Utils;

import br.trcraft.arena.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ItensConfig {

    private static File itensFile;
    private static FileConfiguration itensConfig;

    /**
     * Carrega a configuração itens.yml para memória.
     * Deve ser chamado no onEnable() do plugin.
     */
    public static void loadItensConfig() {
        if (itensFile == null) {
            itensFile = new File(Main.get().getDataFolder(), "itens.yml");
        }

        if (!itensFile.exists()) {
            // Copia o arquivo itens.yml padrão do JAR para a pasta do plugin
            Main.get().saveResource("itens.yml", false);
        }

        itensConfig = YamlConfiguration.loadConfiguration(itensFile);
    }

    /**
     * Salva quaisquer alterações feitas na configuração itens.yml no disco.
     */
    public static void saveItensConfig() {
        if (itensConfig == null || itensFile == null) return;

        try {
            itensConfig.save(itensFile);
        } catch (IOException e) {
            Main.get().getLogger().warning("Erro ao salvar itens.yml: " + e.getMessage());
        }
    }

    /**
     * Retorna o FileConfiguration da itens.yml para leitura e escrita.
     * Garante que a config foi carregada antes de retornar.
     */
    public static FileConfiguration getItensConfig() {
        if (itensConfig == null) loadItensConfig();
        return itensConfig;
    }

    /**
     * Redefine (limpa) os dados do kit na config.
     */
    public static void limparKitArena() {
        getItensConfig().set("kit", null);
        saveItensConfig();
    }
}
