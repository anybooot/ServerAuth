package ro.serverauth.manager;

import org.bukkit.configuration.file.FileConfiguration;
import ro.serverauth.ServerAuthPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuthManager {

    private final ServerAuthPlugin plugin;

    private boolean enabled;
    private String masterPassword;
    private List<String> otpPasswords;
    private int timeoutSeconds;

    private final Map<UUID, CompletableFuture<Boolean>> awaiting = new ConcurrentHashMap<>();

    public AuthManager(ServerAuthPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("enabled", true);
        masterPassword = cfg.getString("master-password", "changeme123");
        otpPasswords = new ArrayList<>(cfg.getStringList("otp-passwords"));
        timeoutSeconds = cfg.getInt("timeout-seconds", 120);
    }

    public void reload() {
        plugin.reloadConfig();
        load();

        for (Map.Entry<UUID, CompletableFuture<Boolean>> entry : awaiting.entrySet()) {
            entry.getValue().complete(false);
        }
        awaiting.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
        plugin.getConfig().set("enabled", value);
        plugin.saveConfig();
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public CompletableFuture<Boolean> createAwaiting(UUID uuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeOnTimeout(false, timeoutSeconds, TimeUnit.SECONDS);
        awaiting.put(uuid, future);
        return future;
    }

    public void completeAwaiting(UUID uuid, boolean success) {
        CompletableFuture<Boolean> future = awaiting.remove(uuid);
        if (future != null) {
            future.complete(success);
        }
    }

    public boolean tryPassword(String input) {
        if (input == null || input.isBlank()) return false;

        if (input.equals(masterPassword)) {
            return true;
        }

        synchronized (otpPasswords) {
            if (otpPasswords.remove(input)) {
                plugin.getConfig().set("otp-passwords", otpPasswords);
                plugin.saveConfig();
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        awaiting.clear();
    }
}