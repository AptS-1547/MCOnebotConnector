package net.esaps.MCOnebotConnector;

import com.google.inject.Inject;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import java.nio.file.Path;

@Getter
@Plugin(
        id = "mc-onebot-connector",
        name = "MCOnebotConnector",
        version = BuildConstants.VERSION,
        url = "https://mc.esaps.net/plugins/qqmcmessage",
        description = "A plugin that sends messages to a QQ group and receives messages from a QQ group via onebot v11",
        authors = {"AptS:1547"}
)

public class MCOnebotConnector {

    @Inject
    private Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;

    @Inject
    public MCOnebotConnector(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing QQMCMessage Plugin...");
        ConfigHandler.initConfig(this);
        WebsocketHandler.initConnection(this);

    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {

        if (!WebsocketHandler.isConnected()) {
            logger.error("OnebotWSClient is not connected");
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(
                    "<rainbow>[MCOnebotConnector]</rainbow> <red>消息发送失败，未连接至机器人</red>"));
            return;
        }

        if (event.getMessage().startsWith(ConfigHandler.getConfig().prefixMc)) {
            String message = event.getMessage().replace(ConfigHandler.getConfig().prefixMc, "");
            WebsocketHandler.send_group_message(920838753, event.getPlayer(), message);
        }
    }
}
