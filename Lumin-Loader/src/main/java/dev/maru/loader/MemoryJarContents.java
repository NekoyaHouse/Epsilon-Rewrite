package dev.maru.loader;

import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.jarcontents.JarResourceAttributes;
import net.neoforged.fml.jarcontents.JarResourceVisitor;
import net.neoforged.fml.jarcontents.PathNormalization;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MemoryJarContents implements JarContents {
    
    private final Map<String, byte[]> entries = new HashMap<>();
    private final Map<String, ZipEntryInfo> entryInfo = new HashMap<>();
    private Manifest manifest;
    private String checksum;
    private final String identifier;

    public MemoryJarContents(byte[] jarBytes, String identifier) {
        this.identifier = identifier;
        loadFromBytes(jarBytes);
        computeChecksum(jarBytes);
    }

    private void loadFromBytes(byte[] jarBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = PathNormalization.normalize(entry.getName());
                    byte[] bytes = zis.readAllBytes();
                    entries.put(name, bytes);
                    entryInfo.put(name, new ZipEntryInfo(entry.getLastModifiedTime(), entry.getSize()));
                    
                    if (name.equals("META-INF/MANIFEST.MF")) {
                        try (InputStream is = new ByteArrayInputStream(bytes)) {
                            manifest = new Manifest(is);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JAR from memory", e);
        }
        
        if (manifest == null) {
            manifest = new Manifest();
        }
    }

    private void computeChecksum(byte[] jarBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jarBytes);
            checksum = HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            checksum = null;
        }
    }

    @Override
    public Optional<String> getChecksum() {
        return Optional.ofNullable(checksum);
    }

    @Override
    public Path getPrimaryPath() {
        return Path.of("memory:" + identifier);
    }

    @Override
    public Collection<Path> getContentRoots() {
        return List.of(getPrimaryPath());
    }

    @Override
    @Nullable
    public JarResource get(String relativePath) {
        String normalized = PathNormalization.normalize(relativePath);
        byte[] bytes = entries.get(normalized);
        if (bytes == null) {
            return null;
        }
        return new MemoryJarResource(bytes, entryInfo.get(normalized));
    }

    @Override
    public Optional<URI> findFile(String relativePath) {
        String normalized = PathNormalization.normalize(relativePath);
        if (entries.containsKey(normalized)) {
            return Optional.of(URI.create("memory:" + identifier + "!/" + normalized));
        }
        return Optional.empty();
    }

    @Override
    @Nullable
    public InputStream openFile(String relativePath) throws IOException {
        String normalized = PathNormalization.normalize(relativePath);
        byte[] bytes = entries.get(normalized);
        if (bytes == null) {
            return null;
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public byte[] readFile(String relativePath) throws IOException {
        String normalized = PathNormalization.normalize(relativePath);
        return entries.get(normalized);
    }

    @Override
    public boolean containsFile(String relativePath) {
        return entries.containsKey(PathNormalization.normalize(relativePath));
    }

    @Override
    public Manifest getManifest() {
        return manifest;
    }

    @Override
    public void visitContent(String startingFolder, JarResourceVisitor visitor) {
        String folder = PathNormalization.normalizeFolderPrefix(startingFolder);
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String path = entry.getKey();
            if (folder.isEmpty() || path.startsWith(folder)) {
                visitor.visit(path, new MemoryJarResource(entry.getValue(), entryInfo.get(path)));
            }
        }
    }

    @Override
    public void close() {
        entries.clear();
        entryInfo.clear();
    }

    @Override
    public String toString() {
        return "memory(" + identifier + ")";
    }

    private record ZipEntryInfo(java.nio.file.attribute.FileTime lastModified, long size) {}

    private static class MemoryJarResource implements JarResource {
        private final byte[] bytes;
        private final ZipEntryInfo info;

        MemoryJarResource(byte[] bytes, ZipEntryInfo info) {
            this.bytes = bytes;
            this.info = info;
        }

        @Override
        public InputStream open() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public JarResourceAttributes attributes() throws IOException {
            return new JarResourceAttributes(info != null ? info.lastModified() : null, info != null ? info.size() : bytes.length);
        }

        @Override
        public JarResource retain() {
            return this;
        }
    }
}
