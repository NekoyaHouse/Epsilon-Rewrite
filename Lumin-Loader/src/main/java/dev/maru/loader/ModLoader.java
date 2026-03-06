package dev.maru.loader;

import dev.maru.verify.LoaderWindow;
import dev.maru.verify.VerificationClient;
import dev.maru.verify.client.IRCHandler;
import dev.maru.verify.client.IRCTransport;
import dev.maru.verify.packet.implemention.c2s.RequestModC2S;
import dev.maru.verify.util.HwidUtil;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import niurendeobf.ZKMIndy;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ZKMIndy
public class ModLoader implements IModFileCandidateLocator {

    private static byte[] modData = null;
    private static final CountDownLatch downloadLatch = new CountDownLatch(1);

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        LoaderWindow.verifyOrExitBlocking();

        try {
            VerificationClient.connect(new IRCHandler() {
                @Override
                public void onMessage(String sender, String message) {

                }

                @Override
                public void onDisconnected(String message) {
                    downloadLatch.countDown();
                }

                @Override
                public void onConnected() {

                }

                @Override
                public String getInGameUsername() {
                    return "";
                }

                @Override
                public void onModDownload(String content, String hash) {
                    try {
                        modData = Base64.getDecoder().decode(content);
                    } catch (Exception ignored) {
                    } finally {
                        downloadLatch.countDown();
                    }
                }
            });

            IRCTransport transport = VerificationClient.getTransport();
            if (transport != null && !transport.isClosed()) {
                String name = LoaderWindow.ModSelection.name;
                String version = LoaderWindow.ModSelection.version;
                transport.sendPacket(new RequestModC2S(HwidUtil.getHWID(), name, version));
            }

            downloadLatch.await(60, TimeUnit.SECONDS);

            if (modData != null) {
                try {
                    Path memoryRoot = MemoryJarUtil.loadJarToMemoryFileSystem(modData);
                    pipeline.addPath(memoryRoot, ModFileDiscoveryAttributes.DEFAULT.withLocator(this), IncompatibleFileReporting.WARN_ALWAYS);
                } finally {
                    Arrays.fill(modData, (byte) 0);
                    modData = null;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
