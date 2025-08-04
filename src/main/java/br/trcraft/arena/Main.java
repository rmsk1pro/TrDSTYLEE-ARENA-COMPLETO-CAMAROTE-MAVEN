package br.trcraft.arena;

import br.trcraft.arena.Eventos.Eventos;
import br.trcraft.arena.Eventos.ItemIntegrityFixer;
import br.trcraft.arena.Utils.ConfigManager;
import br.trcraft.arena.Utils.ItensArena;
import br.trcraft.arena.Utils.ItensConfig;
import br.trcraft.arena.Utils.MySQLAPI;
import br.trcraft.arena.comandos.ArenaCommand;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin implements Listener {

    private File arenasFile;
    private FileConfiguration arenasConfig;
    public ItemIntegrityFixer itemIntegrityFixer;
    private static Main instance;

    public static Main get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        printStatus(true);

        // Config padrão (config.yml)
        saveDefaultConfig();

        // Carregar configurações customizadas
        ConfigManager.loadConfig();
        loadArenasConfig();
        ItensConfig.loadItensConfig();

        // Registrar eventos e comandos
        registerEvents();
        registerCommands();

        // Inicializar conexão MySQL
        initializeMySQL();
    }

    @Override
    public void onDisable() {
        printStatus(false);

        // Salvar configs customizadas
        saveArenasConfig();
        ItensConfig.saveItensConfig();

        // Fechar conexão MySQL
        MySQLAPI.close();

        // Cancelar tarefas pendentes relacionadas à saída da arena
        if (!ItensArena.sairTasks.isEmpty()) {
            for (BukkitRunnable task : ItensArena.sairTasks.values()) {
                task.cancel();
            }
            ItensArena.sairTasks.clear();
        }

        ItensArena.arena.clear();
        ItensArena.arenaCamarote.clear();
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new Eventos(this), this);

        // Remove listener antigo, se houver, e registra um novo com config atualizada
        if (itemIntegrityFixer != null) {
            HandlerList.unregisterAll(itemIntegrityFixer);
        }
        itemIntegrityFixer = new ItemIntegrityFixer(getConfig());
        Bukkit.getPluginManager().registerEvents(itemIntegrityFixer, this);
    }

    private void registerCommands() {
        getCommand("arena").setExecutor(new ArenaCommand(this));
    }

    private void initializeMySQL() {
        FileConfiguration config = getConfig();
        ConfigurationSection mysqlSection = config.getConfigurationSection("mysql");

        if (mysqlSection == null) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] Seção §emysql §cnão encontrada na §fconfig.yml§c!");
            disablePlugin();
            return;
        }

        boolean enabled = mysqlSection.getBoolean("enable", false);
        String host = mysqlSection.getString("host", "");
        int port = mysqlSection.getInt("port", 3306);
        String database = mysqlSection.getString("database", "");
        String user = mysqlSection.getString("user", "");
        String password = mysqlSection.getString("password", "");

        if (!enabled) {
            Bukkit.getConsoleSender().sendMessage("§e[ARENA] MySQL está §cdesativado §ena configuração. O plugin funcionará sem banco de dados.");
            disablePlugin();
            return;
        }

        if (host.isEmpty() || database.isEmpty() || user.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] Configuração incompleta do MySQL! Host, database e user são obrigatórios.");
            disablePlugin();
            return;
        }

        Bukkit.getConsoleSender().sendMessage("§a[ARENA] Inicializando conexão com o MySQL...");
        Bukkit.getConsoleSender().sendMessage("§a[ARENA] §7Host: §f" + host + "§7 | Porta: §f" + port);
        Bukkit.getConsoleSender().sendMessage("§a[ARENA] §7Banco: §f" + database + "§7 | Usuário: §f" + user);

        boolean connected = MySQLAPI.initialize(enabled, host, port, database, user, password);

        if (!connected) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] Não foi possível conectar ao MySQL. Desabilitando plugin...");
            disablePlugin();
        }
    }

    private void disablePlugin() {
        Bukkit.getPluginManager().disablePlugin(this);
    }

    /**
     * Carrega a configuração arenas.yml para uso.
     */
    public void loadArenasConfig() {
        arenasFile = new File(getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            saveResource("arenas.yml", false);
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }

    /**
     * Salva a configuração arenas.yml no disco.
     */
    public void saveArenasConfig() {
        if (arenasFile == null || arenasConfig == null) return;
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            getLogger().severe("Erro ao salvar arenas.yml:");
            e.printStackTrace();
        }
    }

    /**
     * Recarrega todas as configurações do plugin.
     * Inclui config.yml, arenas.yml, itens.yml e configurações auxiliares.
     */
    public void reloadAllConfigs() {
        reloadConfig();
        loadArenasConfig();
        ConfigManager.reloadConfig();
        ItensConfig.loadItensConfig();

        // Atualizar listener itemIntegrityFixer após reload
        if (itemIntegrityFixer != null) {
            HandlerList.unregisterAll(itemIntegrityFixer);
        }
        itemIntegrityFixer = new ItemIntegrityFixer(getConfig());
        Bukkit.getPluginManager().registerEvents(itemIntegrityFixer, this);
    }

    /**
     * Retorna a localização salva da arena (entrada/saída) no arenas.yml.
     * @param path caminho da localização (exemplo: "ARENA" ou "SAIR")
     * @return Location, ou null se não configurada
     */
    public Location getArenaLocation(String path) {
        if (arenasConfig == null || !arenasConfig.contains(path)) return null;

        String worldName = arenasConfig.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = arenasConfig.getDouble(path + ".x");
        double y = arenasConfig.getDouble(path + ".y");
        double z = arenasConfig.getDouble(path + ".z");
        float yaw = (float) arenasConfig.getDouble(path + ".yaw");
        float pitch = (float) arenasConfig.getDouble(path + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Salva uma localização (entrada/saída) da arena em arenas.yml.
     * @param path caminho para salvar (exemplo: "ARENA" ou "SAIR")
     * @param loc localização a ser salva
     */
    public void setArenaLocation(String path, Location loc) {
        if (arenasConfig == null || loc == null) return;

        arenasConfig.set(path + ".world", loc.getWorld().getName());
        arenasConfig.set(path + ".x", loc.getX());
        arenasConfig.set(path + ".y", loc.getY());
        arenasConfig.set(path + ".z", loc.getZ());
        arenasConfig.set(path + ".yaw", loc.getYaw());
        arenasConfig.set(path + ".pitch", loc.getPitch());

        saveArenasConfig();
    }

    //######################
    // Métodos do Camarote
    public Location getCamaroteLocation() {
        return getArenaLocation("CAMAROTE");
    }

    public void setCamaroteLocation(Location location) {
        setArenaLocation("CAMAROTE", location);
    }
    //######################

    /**
     * Imprime uma mensagem de status no console na ativação/desativação do plugin.
     * @param ativando true se ativando, false se desativando
     */

    /**
     * Imprime uma mensagem de status no console na ativação/desativação do plugin.
     * @param ativando true se ativando, false se desativando
     */
    private void printStatus(boolean ativando) {
        String cor = ativando ? "§9" : "§c";
        String status = ativando ? "§a§lATIVADO" : "§4§lDESATIVADO";
        String[] msgs = {
                "",
                cor + " █████╗ ██████╗ ███████╗███╗   ██╗ █████╗ ",
                cor + "██╔══██╗██╔══██╗██╔════╝████╗  ██║██╔══██╗",
                cor + "███████║██████╔╝█████╗  ██╔██╗ ██║███████║",
                cor + "██╔══██║██╔══██╗██╔══╝  ██║╚██╗██║██╔══██║",
                cor + "██║  ██║██║  ██║███████╗██║ ╚████║██║  ██║",
                cor + "╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝╚═╝  ╚═╝",
                "",
                "§fStatus: " + status + " §f| §eARENA §6§lRANKS, TOPKILL",
                "         §fExclusivo §b§lTRCRAFT NETWORK",
                ""
        };

        for (String msg : msgs) {
            Bukkit.getConsoleSender().sendMessage(msg);
        }
    }
}
