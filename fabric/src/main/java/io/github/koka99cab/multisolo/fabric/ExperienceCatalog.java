package io.github.koka99cab.multisolo.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.koka99cab.multisolo.core.experience.Difficulty;
import io.github.koka99cab.multisolo.core.experience.Experience;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/** Reads the bundled, versioned catalogue. Never consumes a remote experience definition. */
public final class ExperienceCatalog {
    private ExperienceCatalog() {}

    public static Experience origin() {
        String path = "/data/multisolo/experiences/origin.json";
        try (var stream = ExperienceCatalog.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled experience: " + path);
            }
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("schema_version").getAsInt() != 1
                    || !"vanilla".equals(json.get("spawn").getAsString())) {
                throw new IllegalStateException("Unsupported experience schema or spawn policy");
            }
            var mods = new HashMap<String, String>();
            json.getAsJsonObject("worldgen_mods").entrySet()
                    .forEach(entry -> mods.put(entry.getKey(), entry.getValue().getAsString()));
            return new Experience(
                    json.get("id").getAsString(), json.get("revision").getAsInt(),
                    Long.parseLong(json.get("seed").getAsString()),
                    Difficulty.valueOf(json.get("difficulty").getAsString()), mods);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid bundled experience: " + path, exception);
        }
    }
}
