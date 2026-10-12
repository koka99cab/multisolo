package io.github.koka99cab.multisolo.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/** A self-authenticated installation identity, not proof of a Microsoft account. */
public final class NetworkIdentity {
    private final KeyPair keys;
    private record StoredKeys(int schemaVersion, String publicKey, String privateKey) {}
    private NetworkIdentity(KeyPair keys) { this.keys = keys; }

    public static NetworkIdentity generate() throws GeneralSecurityException {
        return new NetworkIdentity(KeyPairGenerator.getInstance("Ed25519").generateKeyPair());
    }

    public static synchronized NetworkIdentity loadOrCreate(Path path) throws IOException, GeneralSecurityException {
        if (!Files.exists(path)) {
            NetworkIdentity identity = generate();
            AtomicFiles.write(path, Protocol.JSON.toJson(new StoredKeys(1, identity.publicKey(),
                    Base64.getEncoder().encodeToString(identity.keys.getPrivate().getEncoded()))));
            // Restrict a newly created identity file where POSIX permissions are available.
            try { Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) { /* Windows inherits the user's directory ACL. */ }
            return identity;
        }
        try {
            StoredKeys stored = Protocol.JSON.fromJson(Files.readString(path), StoredKeys.class);
            if (stored.schemaVersion != 1) throw new IOException("Unsupported identity format");
            KeyFactory factory = KeyFactory.getInstance("Ed25519");
            NetworkIdentity identity = new NetworkIdentity(new KeyPair(
                    factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(stored.publicKey))),
                    factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(stored.privateKey)))));
            byte[] probe = "multisolo-identity-check".getBytes(StandardCharsets.UTF_8);
            if (!verify(identity.publicKey(), probe, identity.sign(probe))) throw new IOException("Mismatched identity keys");
            return identity;
        } catch (RuntimeException exception) { throw new IOException("Invalid identity file; it was not replaced", exception); }
    }

    public String publicKey() { return Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()); }
    public String id() { return id(publicKey()); }

    public static String id(String publicKey) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Base64.getDecoder().decode(publicKey)));
        } catch (GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }

    public String sign(byte[] bytes) throws GeneralSecurityException {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keys.getPrivate());
        signer.update(bytes);
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    public static boolean verify(String publicKey, byte[] bytes, String signature) throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(publicKey);
        if (encoded.length != 44) return false;
        var key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(key);
        verifier.update(bytes);
        return verifier.verify(Base64.getDecoder().decode(signature));
    }

    public static byte[] signingBytes(String method, String target, String timestamp, byte[] body) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
            return ("multisolo-v1\n" + method + "\n" + target + "\n" + timestamp + "\n" + digest)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }
}
