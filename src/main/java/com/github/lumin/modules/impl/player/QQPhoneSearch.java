package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ModeSetting;
import com.github.lumin.settings.impl.StringSetting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQPhoneSearch extends Module {

    public static final QQPhoneSearch INSTANCE = new QQPhoneSearch();

    private final ModeSetting apiType = modeSetting("接口类型", "Type1", new String[]{"Type1", "Type2"});
    private final StringSetting qqInput = stringSetting("QQ号", "");
    private final BoolSetting searchButton = boolSetting("开始查询", false);
    private final BoolSetting autoSearch = boolSetting("自动查询", false);
    private final BoolSetting publicChat = boolSetting("公布信息", false);

    private final Pattern qqPattern = Pattern.compile("\\b[1-9]\\d{4,10}\\b");
    private final Map<String, Long> queryCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 30000;

    // QQ号保护列表
    private static final Set<String> protectedQQs = new HashSet<>(Arrays.asList(
            "2390748112", "1096947623", "2227372014", "3488388541", "2259600835",
            "2353761389", "1729251588", "2201728924", "2143288749", "1829734700",
            "1096970384", "3373502163", "3297757599", "3690812160", "1227168895",
            "3162294451", "1465340352", "2802453611", "2116878673"
    ));

    public QQPhoneSearch() {
        super("Q绑查询", "QQSearch", Category.PLAYER);
    }

    @SubscribeEvent
    public void onUpdate(ClientTickEvent.Post event) {
        if (searchButton.getValue()) {
            searchButton.setValue(false);
            performSearch(qqInput.getValue().trim(), false);
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!autoSearch.getValue() || nullCheck()) {
            return;
        }

        Packet<?> packet = event.getPacket();
        String message = null;

        if (packet instanceof ClientboundSystemChatPacket p) {
            message = p.content().getString();
        } else if (packet instanceof ClientboundDisguisedChatPacket p) {
            message = p.message().getString();
        } else if (packet instanceof ClientboundPlayerChatPacket p) {
            message = p.body().content();
        }

        if (message != null) {
            // 忽略自身知道吗不然循环了
            if (message.contains("[Q绑查询]")) {
                return;
            }

            Matcher matcher = qqPattern.matcher(message);
            while (matcher.find()) {
                String potentialQQ = matcher.group();

                if (queryCache.containsKey(potentialQQ)) {
                    if (System.currentTimeMillis() - queryCache.get(potentialQQ) < CACHE_DURATION) {
                        continue;
                    }
                }

                performSearch(potentialQQ, true);
            }
        }
    }

    private void performSearch(String qq, boolean isAuto) {
        if (qq.isEmpty()) {
            if (!isAuto) sendMessage("§c[Q绑查询] 请输入QQ号！");
            return;
        }

        if (protectedQQs.contains(qq)) {
            sendMessage("§c[Q绑查询] 该QQ号码受到开发组保护!");
            if (isAuto) {
                queryCache.put(qq, System.currentTimeMillis());
            }
            return;
        }

        if (isAuto) {
            queryCache.put(qq, System.currentTimeMillis());
        }

        String apiUrl;
        if (apiType.is("Type1")) {
            apiUrl = "https://api.kona.uno/API/qb.php?value=" + qq;
        } else {
            // 老子还给你整了第二个接口
            apiUrl = "http://apii.317ak.com/API/yljk/chaq/chaq.php?qq=" + qq;
        }

        final String finalUrl = apiUrl;

        new Thread(() -> {
            try {
                if (!isAuto) sendMessage("§7[Q绑查询] 正在查询: " + qq + " (接口: " + apiType.getValue() + ") ...");
                URL url = new URL(finalUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    handleResponse(response.toString(), isAuto);
                } else {
                    if (!isAuto) sendMessage("§c[Q绑查询] 请求失败，状态码: " + responseCode);
                }
            } catch (Exception e) {
                if (!isAuto) {
                    sendMessage("§c[Q绑查询] 发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleResponse(String jsonStr, boolean isAuto) {
        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

            boolean hasUsefulInfo = false;
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();

                if (key.equalsIgnoreCase("code") ||
                        key.equalsIgnoreCase("msg") ||
                        key.equalsIgnoreCase("message") ||
                        key.equalsIgnoreCase("result") ||
                        key.equalsIgnoreCase("status") ||
                        key.equalsIgnoreCase("qq")) {
                    continue;
                }

                JsonElement value = entry.getValue();
                if (!value.isJsonNull()) {
                    if (value.isJsonPrimitive()) {
                        String s = value.getAsString();
                        if (!s.isEmpty() && !s.equalsIgnoreCase("null")) {
                            hasUsefulInfo = true;
                            break;
                        }
                    } else if (value.isJsonObject() || value.isJsonArray()) {
                        hasUsefulInfo = true;
                        break;
                    }
                }
            }

            if (!hasUsefulInfo) {
                if (!isAuto) {
                    sendMessage("§c[Q绑查询] 未查询到额外信息。");
                }
                return;
            }

            if (publicChat.getValue()) {
                StringBuilder publicMsg = new StringBuilder("[Q绑查询] ");
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    String key = entry.getKey();
                    if (key.equalsIgnoreCase("code") || key.equalsIgnoreCase("msg") || key.equalsIgnoreCase("status") || key.equalsIgnoreCase("message")) continue;

                    if (entry.getValue().isJsonPrimitive()) {
                        publicMsg.append(key).append(":").append(entry.getValue().getAsString()).append(" ");
                    }
                }
                
                ClientPacketListener connection = mc.getConnection();
                if (connection != null) {
                    connection.sendChat(publicMsg.toString());
                }
            }

            if (!publicChat.getValue()) {
                sendMessage("§a[Q绑查询] 查询结果:");
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    String key = entry.getKey();
                    JsonElement value = entry.getValue();

                    if (value.isJsonPrimitive()) {
                        sendMessage("§b" + key + ": §f" + value.getAsString());
                    } else if (value.isJsonObject()) {
                        sendMessage("§b" + key + ":");
                        JsonObject subObj = value.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> subEntry : subObj.entrySet()) {
                            sendMessage("  §7- " + subEntry.getKey() + ": §f" + subEntry.getValue().toString());
                        }
                    } else if (value.isJsonArray()) {
                        sendMessage("§b" + key + ": §f" + value.toString());
                    }
                }
            }

        } catch (Exception e) {
            if (!isAuto) sendMessage("§a[Q绑查询] 查询结果(原始): " + jsonStr);
        }
    }

    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal(message), false);
                }
            });
        }
    }
}
