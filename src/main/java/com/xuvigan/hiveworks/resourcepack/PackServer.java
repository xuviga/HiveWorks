package com.xuvigan.hiveworks.resourcepack;

import com.sun.net.httpserver.HttpServer;
import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.config.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serves the HiveWorks pack from an embedded JDK HTTP server or a configured URL.
 */
public final class PackServer {

    private final HiveWorksPlugin plugin;
    private final ConfigManager config;
    private final File packFile;

    private HttpServer server;
    private ExecutorService executor;
    private byte[] packBytes;
    private byte[] sha1;
    private String url;
    private boolean active;
    private boolean embedded;

    public PackServer(HiveWorksPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.packFile = new File(plugin.getDataFolder(), "pack/hiveworks-pack.zip");
    }

    public void start() {
        String mode = config.resourcePackMode();
        if ("OFF".equals(mode)) {
            plugin.getLogger().info("Resource pack: disabled.");
            return;
        }
        switch (mode) {
            case "URL" -> {
                if (config.resourcePackUrl().isBlank()) {
                    plugin.getLogger().warning("Resource pack mode is URL but resource-pack.url is empty.");
                    return;
                }
                this.url = config.resourcePackUrl();
                this.active = true;
                plugin.getLogger().info("Resource pack: " + url);
            }
            case "EMBEDDED" -> startEmbedded();
            default -> plugin.getLogger().warning("Unknown resource-pack.mode: " + mode);
        }
    }

    public boolean send(Player player) {
        if (!active || url == null) {
            return false;
        }
        Component prompt = Component.text(config.resourcePackPrompt());
        player.setResourcePack(url, embedded ? sha1 : null, prompt, config.resourcePackRequired());
        return true;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        active = false;
    }

    private void loadPack() {
        plugin.saveResource("pack/hiveworks-pack.zip", false);
        if (!packFile.isFile()) {
            plugin.getLogger().warning("Resource pack archive is missing: " + packFile.getAbsolutePath());
            return;
        }
        try {
            this.packBytes = Files.readAllBytes(packFile.toPath());
            this.sha1 = MessageDigest.getInstance("SHA-1").digest(packBytes);
        } catch (IOException | NoSuchAlgorithmException exception) {
            plugin.getLogger().warning("Failed to load resource pack: " + exception.getMessage());
            this.packBytes = null;
            this.sha1 = null;
        }
    }

    private void startEmbedded() {
        loadPack();
        if (packBytes == null) {
            return;
        }
        try {
            this.embedded = true;
            this.server = HttpServer.create(new InetSocketAddress(config.resourcePackPort()), 0);
            this.server.createContext("/hiveworks-pack.zip", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, packBytes.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(packBytes);
                }
            });
            this.executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "HiveWorks-PackServer");
                thread.setDaemon(true);
                return thread;
            });
            this.server.setExecutor(executor);
            this.server.start();

            String base = config.resourcePackPublicUrl().trim();
            if (base.isEmpty()) {
                plugin.getLogger().warning("Resource pack EMBEDDED is listening on port "
                        + config.resourcePackPort()
                        + " but resource-pack.public-url is empty; players will not receive it.");
                return;
            }
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            this.url = base + "/hiveworks-pack.zip";
            this.active = true;
            plugin.getLogger().info("Resource pack: " + url + " (sha1 " + hex(sha1) + ")");
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to start embedded pack server: " + exception.getMessage());
        }
    }

    public boolean active() {
        return active;
    }

    public String url() {
        return url;
    }

    private static String hex(byte[] bytes) {
        if (bytes == null) {
            return "-";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}
