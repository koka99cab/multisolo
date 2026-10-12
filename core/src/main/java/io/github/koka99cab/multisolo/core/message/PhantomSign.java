package io.github.koka99cab.multisolo.core.message;

import java.util.Objects;

/** Shared content of one face of an existing vanilla sign. */
public record PhantomSign(MessagePhrase phrase, SignColor color, Face face) {
    public PhantomSign { Objects.requireNonNull(phrase); Objects.requireNonNull(color); Objects.requireNonNull(face); }
    public PhantomSign(MessagePhrase phrase, Face face) { this(phrase, SignColor.DEFAULT, face); }
    public enum Face { FRONT, BACK }
}
