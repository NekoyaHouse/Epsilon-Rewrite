package com.github.epsilon.assets.i18n;

import com.github.epsilon.assets.holders.TextureCacheHolder;
import com.github.epsilon.assets.holders.TranslateHolder;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LanguageReloadListener implements PreparableReloadListener {

    @Override
    public @NonNull CompletableFuture<Void> reload(
            @NonNull SharedState sharedState,
            @NonNull Executor exectutor,
            PreparationBarrier barrier,
            @NonNull Executor applyExectutor
    ) {
        return CompletableFuture.completedFuture(null)
                .thenCompose(barrier::wait)
                .thenRunAsync(() -> {

                    TranslateHolder.INSTANCE.refresh();
                    TextureCacheHolder.INSTANCE.clearCache();

                }, applyExectutor);
    }

}
