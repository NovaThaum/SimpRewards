package com.example.rewards;

import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlayerRewardsDAO {
    
    private final RewardsPlugin plugin;
    private Connection connection;
    private boolean isMySQL;
    
    public PlayerRewardsDAO(RewardsPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            ConfigManager config = plugin.getConfigManager();
            isMySQL = config.getDatabaseType().equalsIgnoreCase("mysql");
            
            if (isMySQL) {
                // MySQL 连接
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort() + 
                           "/" + config.getMysqlDatabase() + "?useSSL=false&characterEncoding=utf8";
                connection = DriverManager.getConnection(url, config.getMysqlUser(), config.getMysqlPassword());
            } else {
                // SQLite 连接
                Class.forName("org.sqlite.JDBC");
                plugin.getDataFolder().mkdirs();
                String url = "jdbc:sqlite:" + plugin.getDataFolder().getPath() + File.separator + "rewards.db";
                connection = DriverManager.getConnection(url);
            }
            
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void createTables() throws SQLException {
        String tableSQL = """
            CREATE TABLE IF NOT EXISTS player_rewards (
                uuid VARCHAR(36) PRIMARY KEY,
                last_sign_time BIGINT DEFAULT 0,
                sign_streak INT DEFAULT 0,
                total_seconds INT DEFAULT 0,
                daily_seconds INT DEFAULT 0,
                weekly_seconds INT DEFAULT 0,
                consecutive_days INT DEFAULT 0,
                last_active_date VARCHAR(10) DEFAULT '',
                claimed_milestones TEXT DEFAULT '[]',
                claimed_consecutive TEXT DEFAULT '[]',
                daily_claimed_date VARCHAR(10) DEFAULT '',
                weekly_claimed_week VARCHAR(10) DEFAULT ''
            );
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(tableSQL);
        }
    }
    
    public CompletableFuture<PlayerRewardsData> getPlayerData(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM player_rewards WHERE uuid = ?")) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToData(rs);
                    } else {
                        // 创建新记录
                        PlayerRewardsData newData = new PlayerRewardsData(uuid);
                        insertPlayerData(newData);
                        return newData;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get player data for " + uuid + ": " + e.getMessage());
                return new PlayerRewardsData(uuid);
            }
        });
    }
    
    public CompletableFuture<Boolean> savePlayerData(PlayerRewardsData data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (existsPlayerData(data.getUuid())) {
                    return updatePlayerData(data);
                } else {
                    return insertPlayerData(data);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save player data for " + data.getUuid() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    private boolean existsPlayerData(String uuid) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT COUNT(*) as count FROM player_rewards WHERE uuid = ?")) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("count") > 0;
            }
        }
    }
    
    private boolean insertPlayerData(PlayerRewardsData data) throws SQLException {
        String sql = """
            INSERT INTO player_rewards (
                uuid, last_sign_time, sign_streak, total_seconds, daily_seconds,
                weekly_seconds, consecutive_days, last_active_date,
                claimed_milestones, claimed_consecutive,
                daily_claimed_date, weekly_claimed_week
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, data.getUuid());
            stmt.setLong(2, data.getLastSignTime());
            stmt.setInt(3, data.getSignStreak());
            stmt.setInt(4, data.getTotalSeconds());
            stmt.setInt(5, data.getDailySeconds());
            stmt.setInt(6, data.getWeeklySeconds());
            stmt.setInt(7, data.getConsecutiveDays());
            stmt.setString(8, data.getLastActiveDate());
            stmt.setString(9, toJsonArray(data.getClaimedMilestones()));
            stmt.setString(10, toJsonArray(data.getClaimedConsecutive()));
            stmt.setString(11, data.getDailyClaimedDate());
            stmt.setString(12, data.getWeeklyClaimedWeek());
            return stmt.executeUpdate() > 0;
        }
    }
    
    private boolean updatePlayerData(PlayerRewardsData data) throws SQLException {
        String sql = """
            UPDATE player_rewards SET
                last_sign_time = ?,
                sign_streak = ?,
                total_seconds = ?,
                daily_seconds = ?,
                weekly_seconds = ?,
                consecutive_days = ?,
                last_active_date = ?,
                claimed_milestones = ?,
                claimed_consecutive = ?,
                daily_claimed_date = ?,
                weekly_claimed_week = ?
            WHERE uuid = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, data.getLastSignTime());
            stmt.setInt(2, data.getSignStreak());
            stmt.setInt(3, data.getTotalSeconds());
            stmt.setInt(4, data.getDailySeconds());
            stmt.setInt(5, data.getWeeklySeconds());
            stmt.setInt(6, data.getConsecutiveDays());
            stmt.setString(7, data.getLastActiveDate());
            stmt.setString(8, toJsonArray(data.getClaimedMilestones()));
            stmt.setString(9, toJsonArray(data.getClaimedConsecutive()));
            stmt.setString(10, data.getDailyClaimedDate());
            stmt.setString(11, data.getWeeklyClaimedWeek());
            stmt.setString(12, data.getUuid());
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean claimMilestone(String uuid, String milestoneId) throws SQLException {
        String sql = """
            UPDATE player_rewards 
            SET claimed_milestones = JSON_ARRAY_APPEND(claimed_milestones, '$', ?)
            WHERE uuid = ? AND NOT JSON_CONTAINS(claimed_milestones, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, milestoneId);
            stmt.setString(2, uuid);
            stmt.setString(3, "\"" + milestoneId + "\"");
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean claimConsecutive(String uuid, String consecutiveId) throws SQLException {
        String sql = """
            UPDATE player_rewards 
            SET claimed_consecutive = JSON_ARRAY_APPEND(claimed_consecutive, '$', ?)
            WHERE uuid = ? AND NOT JSON_CONTAINS(claimed_consecutive, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, consecutiveId);
            stmt.setString(2, uuid);
            stmt.setString(3, "\"" + consecutiveId + "\"");
            return stmt.executeUpdate() > 0;
        }
    }
    
    private PlayerRewardsData mapResultSetToData(ResultSet rs) throws SQLException {
        PlayerRewardsData data = new PlayerRewardsData(rs.getString("uuid"));
        data.setLastSignTime(rs.getLong("last_sign_time"));
        data.setSignStreak(rs.getInt("sign_streak"));
        data.setTotalSeconds(rs.getInt("total_seconds"));
        data.setDailySeconds(rs.getInt("daily_seconds"));
        data.setWeeklySeconds(rs.getInt("weekly_seconds"));
        data.setConsecutiveDays(rs.getInt("consecutive_days"));
        data.setLastActiveDate(rs.getString("last_active_date"));
        data.setClaimedMilestones(fromJsonArray(rs.getString("claimed_milestones")));
        data.setClaimedConsecutive(fromJsonArray(rs.getString("claimed_consecutive")));
        data.setDailyClaimedDate(rs.getString("daily_claimed_date"));
        data.setWeeklyClaimedWeek(rs.getString("weekly_claimed_week"));
        return data;
    }
    
    private String toJsonArray(Set<String> set) {
        return "[" + String.join(",", set.stream().map(s -> "\"" + s + "\"").toList()) + "]";
    }
    
    private Set<String> fromJsonArray(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return new HashSet<>();
        }
        // 简单的JSON数组解析
        json = json.replace("[", "").replace("]", "").replace("\"", "");
        String[] items = json.split(",");
        Set<String> result = new HashSet<>();
        for (String item : items) {
            if (!item.trim().isEmpty()) {
                result.add(item.trim());
            }
        }
        return result;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
        }
    }
}
