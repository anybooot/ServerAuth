package ro.serverauth.listener;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ro.serverauth.ServerAuthPlugin;
import ro.serverauth.manager.AuthManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthListener implements Listener {

    private final ServerAuthPlugin plugin;
    private final AuthManager auth;
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Key ACTION_KEY = Key.key("serverauth:submit");

    public AuthListener(ServerAuthPlugin plugin) {
        this.plugin = plugin;
        this.auth = plugin.getAuthManager();
    }

    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent event) {
        if (!auth.isEnabled()) return;

        PlayerConfigurationConnection connection = event.getConnection();
        UUID uuid = connection.getProfile().getId();
        if (uuid == null) return;

        String name = connection.getProfile().getName();
        if (name == null) name = "Player";

        CompletableFuture<Boolean> future = auth.createAwaiting(uuid);

        Audience audience = connection.getAudience();
        Dialog dialog = createPasswordDialog(name);
        audience.showDialog(dialog);

        boolean allowed = future.join();

        if (!allowed) {
            audience.closeDialog();
            Component kickMsg = MM.deserialize(
                    plugin.getConfig().getString("messages.kick-timeout",
                            "<red>Timpul de autentificare a expirat.")
            );
            connection.disconnect(kickMsg);
        }
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!ACTION_KEY.equals(event.getIdentifier())) return;

        DialogResponseView view = event.getDialogResponseView();
        if (view == null) return;

        String password = view.getText("password");
        if (password == null) password = "";

        UUID uuid = null;
        if (event.getCommonConnection() instanceof PlayerConfigurationConnection conf) {
            uuid = conf.getProfile().getId();
        } else if (event.getCommonConnection() instanceof io.papermc.paper.connection.PlayerGameConnection game) {
            if (game.getPlayer() != null) {
                uuid = game.getPlayer().getUniqueId();
            }
        }

        if (uuid == null) return;

        boolean success = auth.tryPassword(password);

        if (success) {
            auth.completeAwaiting(uuid, true);
        } else {
            auth.completeAwaiting(uuid, false);

            Component kick = MM.deserialize(
                    plugin.getConfig().getString("messages.kick-wrong",
                            "<red>Parolă greșită. Acces refuzat.")
            );

            if (event.getCommonConnection() instanceof PlayerConfigurationConnection conf) {
                conf.getAudience().closeDialog();
                conf.disconnect(kick);
            }
        }
    }

    private Dialog createPasswordDialog(String playerName) {
        String title = plugin.getConfig().getString("dialog.title", "<gold>Server Maintenance");
        String body = plugin.getConfig().getString("dialog.body",
                "<yellow>Serverul este în mentenanță.\nIntrodu parola pentru a continua.");
        String buttonText = plugin.getConfig().getString("dialog.button", "Enter");
        String label = plugin.getConfig().getString("dialog.password-label", "Password");

        return Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(MM.deserialize(title))
                        .canCloseWithEscape(false)
                        .body(List.of(
                                DialogBody.plainMessage(MM.deserialize(body.replace("<player>", playerName)))
                        ))
                        .inputs(List.of(
                                DialogInput.text("password", Component.text(label, NamedTextColor.WHITE))
                                        .width(300)
                                        .maxLength(64)
                                        .build()
                        ))
                        .build())
                .type(DialogType.notice(
                        ActionButton.builder(Component.text(buttonText, NamedTextColor.GREEN))
                                .action(DialogAction.customClick(ACTION_KEY, null))
                                .build()
                ))
        );
    }
}