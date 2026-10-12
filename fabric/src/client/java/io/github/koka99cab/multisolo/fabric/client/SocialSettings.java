package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.network.AtomicFiles;
import io.github.koka99cab.multisolo.network.Protocol;
import io.github.koka99cab.multisolo.network.RelayClient;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

record SocialSettings(int schemaVersion, boolean messagesEnabled, String relay) {
    SocialSettings {
        if (schemaVersion != 1) throw new IllegalArgumentException("Unknown social preferences schema");
        RelayClient.validateEndpoint(URI.create(relay));
    }
    static SocialSettings offline() { return new SocialSettings(1, false, "http://127.0.0.1:8787"); }
    static SocialSettings load(Path path) throws IOException {
        if (!Files.exists(path)) return offline();
        try { return Protocol.JSON.fromJson(Files.readString(path), SocialSettings.class); }
        catch (RuntimeException exception) { throw new IOException("Invalid social preferences", exception); }
    }
    void save(Path path) throws IOException { AtomicFiles.write(path, Protocol.JSON.toJson(this)); }
}
