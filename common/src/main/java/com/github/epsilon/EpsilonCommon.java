package com.github.epsilon;

import com.github.epsilon.assets.i18n.I18NFileGenerator;
import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.SyncManager;
import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.managers.network.ClientboundPacketManager;
import com.github.epsilon.managers.network.ServerboundPacketManager;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common initialization logic shared by all loaders.
 */
public class EpsilonCommon {

    public static final String MODID = "open_epsilon";
    public static String VERSION = "Loading ...";

    public static final Logger LOGGER = LogManager.getLogger("Open Epsilon");

    public static int skipTicks;
    public static Minecraft mc;

    /**
     * Called during client setup on all loaders.
     */
    public static void init() {
        LOGGER.info("Welcome to Epsilon, Meow~");

        mc = Minecraft.getInstance();

        // 初始化 Managers
        ModuleManager.INSTANCE.initModules();

        // 初始化网络和同步管理器（触发单例构造，注册到事件总线）
        SyncManager.INSTANCE.getClass();
        ClientboundPacketManager.INSTANCE.getClass();
        ServerboundPacketManager.INSTANCE.getClass();

        TargetManager.INSTANCE.clearSharedTarget();
        ConfigManager.INSTANCE.initConfig();

        // 生成空的 i18n 文件
        I18NFileGenerator.generate("epsilon-config/empty-i18n.json");

        // 添加一个退出游戏时候的钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.INSTANCE.saveNow();
            LOGGER.info("お兄ちゃん、私はあなたを一番愛しています~");
        }));

        LOGGER.info("Epsilon has loaded successfully, Meow~");
    }

}

