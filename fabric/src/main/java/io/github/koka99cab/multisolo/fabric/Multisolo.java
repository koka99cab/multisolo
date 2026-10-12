package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.social.SocialConsent;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bootstrap only: no save is changed and no network connection is opened. */
public final class Multisolo implements ModInitializer {
    public static final String MOD_ID = "multisolo";
    public static final int PROTOCOL_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        var experience = ExperienceCatalog.origin();
        LOGGER.info("Multisolo foundation loaded: {} revision {}, social defaults: {}",
                experience.id(), experience.revision(), SocialConsent.offline());
    }
}
