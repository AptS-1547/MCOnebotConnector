package net.esaps.MCOnebotConnector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.velocitypowered.api.proxy.Player;

import java.net.URI;
import java.util.*;

public class WebsocketHandler extends WebSocketClient {

    private static MCOnebotConnector plugin;
    private static WebsocketHandler wsClient;
    private static Map<String, UUID> callbackMapMCToQQ = new HashMap<>();

    public WebsocketHandler(URI uri, Map<String, String> httpHeaders) {
        super(uri, httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        plugin.getLogger().info("Plugin Successfully Connect to Onebot Websocket Server");
    }

    @Override
    public void onMessage(String onebotMessage) {

        JSONObject messageObject = new JSONObject(onebotMessage);

        if (callbackMCtoOnebot(messageObject)) return;
        if (callOnebotToMC(messageObject)) return;

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        plugin.getLogger().info("Disconnected from server with exit code {} additional info: {}", code, reason);
    }

    @Override
    public void onError(Exception ex) {
        plugin.getLogger().error("An error occurred:{}", String.valueOf(ex));
    }

    public static boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }

    public static void initConnection(MCOnebotConnector plugin) {
        WebsocketHandler.plugin = plugin;

        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Authorization", "Bearer " + ConfigHandler.getConfig().accessToken);
        wsClient = new WebsocketHandler(URI.create(ConfigHandler.getConfig().connectAddress), httpHeaders);
        wsClient.connect();
    }

    public static void send_group_message(long groupId, Player player, String message) {
        JSONObject params = new JSONObject();

        message = ConfigHandler.getConfig().sendFormatFromMC
                .replace("{playerName}", player.getUsername())
                .replace("{message}", message);

        params.put("group_id", groupId);
        params.put("message", message);

        String requestId = UUID.randomUUID().toString();

        callbackMapMCToQQ.put(requestId, player.getUniqueId());
        wsClient.send(constructRequest("send_group_msg", params, requestId));
    }

    public static boolean callbackMCtoOnebot(JSONObject messageObject) {
        try {
            if (messageObject.has("echo")) {
                String requestId = messageObject.getString("echo");

                if (!callbackMapMCToQQ.containsKey(requestId)) return false;
                Player player = plugin.getServer().getPlayer(callbackMapMCToQQ.get(requestId)).orElse(null);
                if (player == null) {callbackMapMCToQQ.remove(requestId); return true;}

                if (Objects.equals(messageObject.getString("status"), "ok")) {
                    // Mc to Onebot 发送消息成功后的回调
                    callbackMapMCToQQ.remove(requestId);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<rainbow>[MCOnebotConnector]</rainbow> <green>消息发送成功</green>"));
                    return true;
                } else if (Objects.equals(messageObject.getString("status"), "failed")) {
                    // Mc to Onebot 发送消息失败后的回调
                    callbackMapMCToQQ.remove(requestId);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<rainbow>[MCOnebotConnector]</rainbow> <red>消息发送失败，错误代码：" + messageObject.getInt("retcode") + "</red>"));
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().error("Failed to parse message: {}", e.getMessage());
        }
        return false;
    }

    public static boolean callOnebotToMC(JSONObject messageObject) {
        if (!messageObject.has("post_type") || !messageObject.has("message_type")) return false;

        if (!Objects.equals(messageObject.getString("post_type"), "message")) return false;
        if (!Objects.equals(messageObject.getString("message_type"), "group")) return false;

        String raw_message = messageObject.getString("raw_message");

        if (!raw_message.startsWith(ConfigHandler.getConfig().prefixOnebot)) {
            return false;
        }

        String onebotUserName = messageObject.getJSONObject("sender").getString("nickname");
        String onebotUserId = String.valueOf(messageObject.getInt("user_id"));

        Component message = MiniMessage.miniMessage().deserialize(
                ConfigHandler.getConfig().sendFormatFromOnebot
                .replace("{onebotUserName}", onebotUserName)
                .replace("{onebotUserID}", onebotUserId)
                .replace("{message}", raw_message)
                .replace(ConfigHandler.getConfig().prefixOnebot, ""));

        List<Player> players = (List<Player>) plugin.getServer().getAllPlayers();
        if (Integer.toString(messageObject.getInt("group_id")).equals("920838753")) {
            for (Player player : players) {
                player.sendMessage(message);
            }
        }

        return true;
    }

    private static String constructRequest(String action, JSONObject params, String requestId) {
        JSONObject request = new JSONObject();
        request.put("action", action);
        request.put("params", params);
        request.put("echo", requestId);
        return request.toString();
    }

}
