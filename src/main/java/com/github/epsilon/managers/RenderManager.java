package com.github.epsilon.managers;

import net.minecraft.client.DeltaTracker;

import java.util.ArrayList;
import java.util.function.Consumer;

public class RenderManager {

    public static final RenderManager INSTANCE = new RenderManager();

    private final ArrayList<Consumer<DeltaTracker>> renderQueue = new ArrayList<>();

    private RenderManager() {
    }

    public void applyRenderAfterFrame(Consumer<DeltaTracker> func) {
        renderQueue.add(func);
    }

    public void applyRenderAfterFrame(Runnable func) {
        renderQueue.add(_ -> func.run());
    }

    public void callAndClear(DeltaTracker tracker) {
        if (renderQueue.isEmpty()) return;
        ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderQueue);
        renderQueue.clear();
        pending.forEach(func -> func.accept(tracker));
    }

}
