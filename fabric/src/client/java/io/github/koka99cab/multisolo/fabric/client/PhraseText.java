package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.MessagePhrase;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class PhraseText {
    private PhraseText() {}
    static MutableComponent line(MessagePhrase.Line line) {
        return Component.translatable("multisolo.phrase.line." + line.name().toLowerCase(Locale.ROOT));
    }
    static Component component(MessagePhrase phrase) {
        return Component.literal(String.join(" / ", phrase.lines().stream().map(value -> line(value).getString()).filter(value -> !value.isEmpty()).toList()));
    }
}
