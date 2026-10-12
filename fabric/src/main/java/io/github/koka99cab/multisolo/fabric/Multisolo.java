package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.social.SocialConsent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** World rules are scoped to explicitly created experience worlds. Network is opt-in. */
public final class Multisolo implements ModInitializer {
    public static final String MOD_ID = "multisolo";
    public static final int PROTOCOL_VERSION = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        var experience = ExperienceCatalog.origin();
        ServerLifecycleEvents.SERVER_STARTING.register(ExperienceWorlds::starting);
        ServerLifecycleEvents.SERVER_STOPPED.register(ExperienceWorlds::stopped);
        LOGGER.info("Multisolo Connected Solitude loaded: {} revision {}, social defaults: {}",
                experience.id(), experience.revision(), SocialConsent.offline());
    }
}
