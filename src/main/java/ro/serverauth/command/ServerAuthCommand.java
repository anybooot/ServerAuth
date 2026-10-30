package ro.serverauth.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ro.serverauth.ServerAuthPlugin;
import ro.serverauth.manager.AuthManager;

public class ServerAuthCommand implements CommandExecutor {

    private final ServerAuthPlugin plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public ServerAuthCommand(ServerAuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverauth.admin")) {
            sender.sendMessage(MM.deserialize(
                    plugin.getConfig().getString("messages.no-permission", "<red>Nu ai permisiune.")));
            return true;
        }

        AuthManager auth = plugin.getAuthManager();

        if (args.length == 0) {
            sender.sendMessage(MM.deserialize("<yellow>/serverauth <toggle|reload>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                boolean newState = !auth.isEnabled();
                auth.setEnabled(newState);
                sender.sendMessage(MM.deserialize(newState
                        ? plugin.getConfig().getString("messages.toggle-on")
                        : plugin.getConfig().getString("messages.toggle-off")));
            }
            case "reload" -> {
                auth.reload();
                sender.sendMessage(MM.deserialize(
                        plugin.getConfig().getString("messages.reload-success", "<green>Reload OK")));
            }
            default -> sender.sendMessage(MM.deserialize("<yellow>/serverauth <toggle|reload>"));
        }
        return true;
    }
}