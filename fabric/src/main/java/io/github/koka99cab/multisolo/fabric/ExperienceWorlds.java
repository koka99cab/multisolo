package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.experience.Experience;
import io.github.koka99cab.multisolo.network.AtomicFiles;
import io.github.koka99cab.multisolo.network.Protocol;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

public final class ExperienceWorlds {
    private static final String MARKER = "multisolo-experience.json";
    private static final Map<MinecraftServer, Experience> ACTIVE = new ConcurrentHashMap<>();
    public record Definition(int schemaVersion, String minecraftVersion, Experience experience) {}
    private ExperienceWorlds() {}

    public static String gameVersion() { return SharedConstants.getCurrentVersion().id(); }

    public static boolean cleanProfile() {
        boolean testRun = FabricLoader.getInstance().isDevelopmentEnvironment()
                && System.getProperty("fabric.client.gametest") != null;
        return FabricLoader.getInstance().getAllMods().stream().allMatch(mod -> {
            String id = mod.getMetadata().getId();
            return id.equals("minecraft") || id.equals("java") || id.equals("fabricloader")
                    || id.equals("multisolo") || id.equals("mixinextras")
                    || id.equals("fabric-api-base") || id.equals("fabric-lifecycle-events-v1")
                    || id.equals("fabric-screen-api-v1")
                    || id.equals("fabric-resource-loader-v1")
                    || (testRun && (id.startsWith("fabric-") || id.equals("multisolo-test")));
        });
    }

    public static void markNewWorld(Path directory) throws IOException {
        if (Files.exists(directory)) throw new IOException("A new world directory is required");
        Files.createDirectory(directory);
        AtomicFiles.write(directory.resolve(MARKER), Protocol.JSON.toJson(
                new Definition(1, gameVersion(), ExperienceCatalog.origin())));
    }

    public static void starting(MinecraftServer server) {
        Path marker = server.getWorldPath(LevelResource.ROOT).resolve(MARKER);
        if (!Files.exists(marker)) return;
        try {
            Definition definition = Protocol.JSON.fromJson(Files.readString(marker), Definition.class);
            if (definition.schemaVersion() != 1 || !definition.minecraftVersion().equals(gameVersion())
                    || !definition.experience().equals(ExperienceCatalog.origin())) {
                throw new IOException("Experience/version differs; explicit migration is required");
            }
            if (server.getWorldGenSettings().options().seed() != definition.experience().seed()
                    || server.getWorldData().isHardcore() || server.getWorldData().getGameType() != GameType.SURVIVAL) {
                throw new IOException("Saved world does not match its experience");
            }
            ACTIVE.put(server, definition.experience());
            // SERVER_STARTING precedes player-list initialization; send no packets here.
            server.getWorldData().setDifficulty(Difficulty.NORMAL);
            server.getWorldData().setDifficultyLocked(true);
            server.getWorldData().setAllowCommands(false);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Multisolo: incompatible experience; save was not converted", exception);
        }
    }

    public static boolean connectedSolitude(MinecraftServer server) { return server != null && ACTIVE.containsKey(server); }
    public static void stopped(MinecraftServer server) { ACTIVE.remove(server); }

    public static boolean canShare(MinecraftServer server) {
        return connectedSolitude(server) && !server.isPublished() && cleanProfile()
                && server.getWorldData().getDataConfiguration().dataPacks().getEnabled().stream()
                    .allMatch(pack -> pack.equals("vanilla") || pack.equals("fabric") || pack.equals("multisolo"));
    }
}
