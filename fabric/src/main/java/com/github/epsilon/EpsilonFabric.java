package com.github.epsilon;

import com.github.epsilon.graphics.LuminRenderPipelines;
import net.fabricmc.api.ClientModInitializer;

public class EpsilonFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EpsilonCommon.init();
        CommonListeners.register();

        // Register render pipelines
        LuminRenderPipelines.registerAll(pipeline -> {
            // Fabric doesn't have RegisterRenderPipelinesEvent
            // Pipelines are auto-registered when referenced in vanilla 26.1.2+
        });

        EpsilonCommon.LOGGER.info("Epsilon Fabric loaded successfully!");
    }

}

