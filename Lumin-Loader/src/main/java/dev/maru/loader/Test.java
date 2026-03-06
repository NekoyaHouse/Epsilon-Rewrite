package dev.maru.loader;

import dev.maru.loader.security.EncryptedMemoryStorage;
import dev.maru.loader.security.SecurityManager;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import niurendeobf.ZKMIndy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ZKMIndy
public class Test implements IModFileCandidateLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger("LuminLoader");

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        if (!SecurityManager.getInstance().isInitialized()) {
            SecurityManager.quickInit();
        }
        
        if (!SecurityManager.quickCheck()) {
            LOGGER.error("Security check failed, falling back to folder loader");
            loadFromFolder(context, pipeline);
            return;
        }
        
        MemoryJarContents jarContents = SecurityManager.getInstance().createSecureJarContents();
        
        if (jarContents != null) {
            try {
                pipeline.addJarContent(jarContents, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
                
                CloudJarManager.getInstance().markLoaded();
                LOGGER.info("Successfully loaded mod from secure memory");
            } catch (Exception e) {
                LOGGER.error("Failed to load mod from memory, falling back to folder loader", e);
                loadFromFolder(context, pipeline);
            }
        } else {
            loadFromFolder(context, pipeline);
        }
    }

    private void loadFromFolder(ILaunchContext context, IDiscoveryPipeline pipeline) {
        IModFileCandidateLocator delegate = IModFileCandidateLocator.forFolder(
            context.gameDirectory().resolve("test").toFile(), 
            "lumin-fallback"
        );
        delegate.findCandidates(context, pipeline);
    }
}
