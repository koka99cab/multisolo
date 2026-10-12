package io.github.koka99cab.multisolo.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class Protocol {
    public static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final int MAX_BODY_BYTES = 8192;
    public static final int MAX_RESPONSE_BYTES = 131072;
    public static final int RADIUS = 128;
    private Protocol() {}

    public record Publish(CompatibilityKey scope, MessagePosition position, String text) {
        public Publish {
            java.util.Objects.requireNonNull(scope);
            java.util.Objects.requireNonNull(position);
            text = LocatedMessage.validateText(text);
        }
    }
    public record Messages(List<LocatedMessage> messages) {
        public Messages { messages = List.copyOf(messages); }
    }

    public static String query(CompatibilityKey scope, MessagePosition position) {
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(
                JSON.toJson(scope).getBytes(StandardCharsets.UTF_8));
        return "/v1/messages?scope=" + key + "&x=" + position.x()
                + "&y=" + position.y() + "&z=" + position.z();
    }

    public static String path(UUID id) { return "/v1/messages/" + id; }
}
