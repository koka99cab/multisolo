package io.github.koka99cab.multisolo.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.core.message.PhantomSign;
import io.github.koka99cab.multisolo.core.message.MessageLifetime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class Protocol {
    public static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final int VERSION = 2;
    public static final int MAX_BODY_BYTES = 8192;
    public static final int MAX_RESPONSE_BYTES = 131072;
    public static final int RADIUS = 128;
    private Protocol() {}

    public record Publish(CompatibilityKey scope, MessagePosition position, PhantomSign sign) {
        public Publish {
            java.util.Objects.requireNonNull(scope);
            java.util.Objects.requireNonNull(position);
            java.util.Objects.requireNonNull(sign);
        }
    }
    public record MessageView(LocatedMessage message, int upvotes, int downvotes, int ownVote) {
        public MessageView {
            java.util.Objects.requireNonNull(message);
            if (upvotes < 0 || downvotes < 0 || upvotes + (long) downvotes > 1000 || ownVote < -1 || ownVote > 1)
                throw new IllegalArgumentException("Invalid votes");
        }
        public long expiresAt() { return MessageLifetime.expiresAt(message.createdAt(), upvotes, downvotes); }
        public boolean activeAt(long now) { return MessageLifetime.activeAt(message.createdAt(), upvotes, downvotes, now); }
    }
    public record Vote(int value) {
        public Vote { if (value < -1 || value > 1) throw new IllegalArgumentException("Vote must be -1, 0 or 1"); }
    }
    public record Messages(List<MessageView> messages) {
        public Messages { messages = List.copyOf(messages); }
    }

    public static String query(CompatibilityKey scope, MessagePosition position) {
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(
                JSON.toJson(scope).getBytes(StandardCharsets.UTF_8));
        return "/v2/messages?scope=" + key + "&x=" + position.x()
                + "&y=" + position.y() + "&z=" + position.z();
    }

    public static String path(UUID id) { return "/v2/messages/" + id; }
}
