package net.khanwarden.projectjavaplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Projectjavaplugin extends JavaPlugin implements Listener {

    private Connection connection;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setupDatabase();
        this.getCommand("register").setExecutor(this::onCommand);
        this.getCommand("login").setExecutor(this::onCommand);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isAuthenticated(player)) {
            player.sendMessage("Enter your login \"/login username password\" or register by \"/register username password\"");;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) throws SQLException {
        Player player = event.getPlayer();
        PreparedStatement statement = connection.prepareStatement("UPDATE players SET authenticated=? WHERE username=?");
        statement.setBoolean(1, false);
        statement.setString(2, player.getName());
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("login") && sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2) {
                String username = args[0];
                String password = args[1];

                if (checkCredentials(player, username, password)) {
                    player.sendMessage("Successful!");
                    setAuthenticated(player, true);
                    return true;
                } else {
                    player.sendMessage("Incorrect username or password.");
                    return false;
                }
            } else {
                player.sendMessage("Use: /login username password");
                return false;
            }
        } else if (cmd.getName().equalsIgnoreCase("register") && sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2) {
                String username = args[0];
                String password = args[1];

                if (registerPlayer(player, username, password)) {
                    player.sendMessage("Successful!");
                    setAuthenticated(player, true);
                    return true;
                } else {
                    player.sendMessage("Error! Maybe the player exists");
                    return false;
                }
            } else {
                player.sendMessage("Use: /register username password");
                return false;
            }
        }
        return false;
    }

    private boolean checkCredentials(Player player, String username, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE username=? AND password=?");
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerPlayer(Player player, String username, String password) {
        try {
            PreparedStatement checkStatement = connection.prepareStatement("SELECT * FROM players WHERE username=?");
            checkStatement.setString(1, username);
            ResultSet resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                return false; // игрок существует
            }

            PreparedStatement registerStatement = connection.prepareStatement("INSERT INTO players (username, password, authenticated) VALUES (?, ?, ?)");
            registerStatement.setString(1, username);
            registerStatement.setString(2, password);
            registerStatement.setBoolean(3, true);
            registerStatement.executeUpdate();
            registerStatement.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isAuthenticated(Player player) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE username=? AND authenticated=?");
            statement.setString(1, player.getName());
            statement.setBoolean(2, true);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setAuthenticated(Player player, boolean authenticated) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE players SET authenticated=? WHERE username=?");
            statement.setBoolean(1, authenticated);
            statement.setString(2, player.getName());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupDatabase() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/postgres";
            String user = "postgres";
            String password = "12345";
            connection = DriverManager.getConnection(url, user, password);

            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS players (id SERIAL PRIMARY KEY, username VARCHAR(255), password VARCHAR(255), authenticated BOOLEAN);");
            statement.executeUpdate();
            statement.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
