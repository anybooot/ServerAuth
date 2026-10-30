package ro.serverauth;

import org.bukkit.plugin.java.JavaPlugin;
import ro.serverauth.command.ServerAuthCommand;
import ro.serverauth.listener.AuthListener;
import ro.serverauth.manager.AuthManager;

public final class ServerAuthPlugin extends JavaPlugin {

    private AuthManager authManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.authManager = new AuthManager(this);
        authManager.load();

        getCommand("serverauth").setExecutor(new ServerAuthCommand(this));
        getServer().getPluginManager().registerEvents(new AuthListener(this), this);

        getLogger().info("ServerAuth (Maintenance Dialog) enabled.");
    }

    @Override
    public void onDisable() {
        if (authManager != null) {
            authManager.cleanup();
        }
    }

    public AuthManager getAuthManager() {
        return authManager;
    }
}