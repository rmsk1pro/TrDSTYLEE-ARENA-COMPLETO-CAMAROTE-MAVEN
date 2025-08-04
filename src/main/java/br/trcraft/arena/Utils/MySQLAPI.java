package br.trcraft.arena.Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import br.trcraft.arena.Main;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MySQLAPI {

    private static HikariDataSource ds;
    private static final long CACHE_UPDATE_INTERVAL_TICKS = 20L * 60 * 5; // 5 minutos
    private static volatile List<PlayerKill> cachedTopKills = new CopyOnWriteArrayList<>();

    public static boolean initialize(boolean enabled, String host, int port, String database, String user, String password) {
        if (!enabled) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] MySQL está §c§lDESATIVADO §cna configuração.");
            return false;
        }

        if (!isConfigValid(host, port, database, user, password)) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] §eConfiguração do MySQL está incompleta ou inválida!");
            return false;
        }

        if (!canConnectToMySQL(host, port, database, user, password)) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] §4Não foi possível se conectar ao MySQL.");
            return false;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");

            ds = new HikariDataSource(config);

            Bukkit.getConsoleSender().sendMessage("§a[ARENA] Conexão com MySQL estabelecida com sucesso!");
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] §4Erro ao configurar o pool de conexões.");
            // Remove e.printStackTrace(); para não poluir o console
            return false;
        }

        createTableIfNotExists();
        updateCache();
        scheduleCacheUpdater();

        return true;
    }

    private static boolean isConfigValid(String host, int port, String database, String user, String password) {
        return host != null && !host.isEmpty()
                && port > 0
                && database != null && !database.isEmpty()
                && user != null && !user.isEmpty()
                && password != null;
    }

    private static boolean canConnectToMySQL(String host, int port, String database, String user, String password) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            return true;
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] §4Falha ao conectar no banco de dados MySQL: " + e.getMessage());
            // Remove e.printStackTrace(); para não poluir o console
            return false;
        }
    }

    private static void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS arena_kills (" +
                "player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "kills INT NOT NULL DEFAULT 0" +
                ") CHARACTER SET utf8 COLLATE utf8_general_ci;";
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            Bukkit.getConsoleSender().sendMessage("§a[ARENA] Tabela arena_kills verificada/criada.");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] Erro ao criar/verificar tabela arena_kills: " + e.getMessage());
            // Remove e.printStackTrace(); para não poluir o console
        }
    }

    public static void addKillAsync(UUID playerUUID, int amount) {
        new BukkitRunnable() {
            @Override
            public void run() {
                addKill(playerUUID, amount);
            }
        }.runTaskAsynchronously(Main.get());
    }

    private static void addKill(UUID playerUUID, int amount) {
        String sql = "INSERT INTO arena_kills (player_uuid, kills) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE kills = kills + VALUES(kills)";
        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§c[ARENA] Erro ao atualizar kills para " + playerUUID + ": " + e.getMessage());
            // Remove e.printStackTrace(); para não poluir o console
        }
    }

    private static void updateCache() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<PlayerKill> topKills = new ArrayList<>();
                String sql = "SELECT player_uuid, kills FROM arena_kills ORDER BY kills DESC LIMIT 10";
                try (Connection conn = ds.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        topKills.add(new PlayerKill(rs.getString("player_uuid"), rs.getInt("kills")));
                    }
                } catch (SQLException e) {
                    Bukkit.getConsoleSender().sendMessage("§c[ARENA] Erro ao atualizar o cache do top 10: " + e.getMessage());
                    // Remove e.printStackTrace(); para não poluir o console
                }

                Bukkit.getScheduler().runTask(Main.get(), () -> {
                    cachedTopKills = Collections.unmodifiableList(topKills);
                    Bukkit.getConsoleSender().sendMessage("§b[ARENA] Cache de TOP 10 kills atualizado.");
                });
            }
        }.runTaskAsynchronously(Main.get());
    }

    private static void scheduleCacheUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateCache();
            }
        }.runTaskTimerAsynchronously(Main.get(), CACHE_UPDATE_INTERVAL_TICKS, CACHE_UPDATE_INTERVAL_TICKS);
    }

    public static List<PlayerKill> getCachedTop10Kills() {
        return cachedTopKills;
    }

    public static void forceUpdateCache() {
        updateCache();
    }

    public static void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    public static class PlayerKill {
        private final String playerUUID;
        private final int kills;

        public PlayerKill(String playerUUID, int kills) {
            this.playerUUID = playerUUID;
            this.kills = kills;
        }

        public String getPlayerUUID() {
            return playerUUID;
        }

        public int getKills() {
            return kills;
        }
    }
}
