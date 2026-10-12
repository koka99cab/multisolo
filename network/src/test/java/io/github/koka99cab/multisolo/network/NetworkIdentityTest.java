package io.github.koka99cab.multisolo.network;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NetworkIdentityTest {
    @TempDir Path directory;

    @Test void identitySurvivesReloadAndAuthenticatesExactRequest() throws Exception {
        Path file = directory.resolve("identity.json");
        NetworkIdentity first = NetworkIdentity.loadOrCreate(file);
        NetworkIdentity reloaded = NetworkIdentity.loadOrCreate(file);
        assertEquals(first.id(), reloaded.id());
        byte[] request = NetworkIdentity.signingBytes("PUT", "/v2/messages/example", "1", new byte[]{1});
        assertTrue(NetworkIdentity.verify(first.publicKey(), request, reloaded.sign(request)));
        assertFalse(NetworkIdentity.verify(NetworkIdentity.generate().publicKey(), request, first.sign(request)));
        assertFalse(NetworkIdentity.verify(first.publicKey(), NetworkIdentity.signingBytes("DELETE", "/v2/messages/example", "1", new byte[]{1}), first.sign(request)));
    }

    @Test void malformedIdentityIsPreservedRatherThanRegenerated() throws Exception {
        Path file = directory.resolve("identity.json");
        Files.writeString(file, "broken");
        assertThrows(Exception.class, () -> NetworkIdentity.loadOrCreate(file));
        assertEquals("broken", Files.readString(file));
    }

    @Test void externalEndpointsRequireTlsAndCannotRedirectIdentityThroughUserInfoOrPaths() {
        for (String endpoint : new String[]{"http://example.com", "https://user:pass@example.com", "https://example.com/base", "https://example.com?x=1", "file:///tmp"}) {
            assertThrows(IllegalArgumentException.class, () -> RelayClient.validateEndpoint(URI.create(endpoint)));
        }
        RelayClient.validateEndpoint(URI.create("http://127.0.0.1:8787"));
        RelayClient.validateEndpoint(URI.create("https://relay.example.com"));
    }
}
