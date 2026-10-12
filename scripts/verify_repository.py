"""Check the repository's product invariants and local documentation links, offline."""

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def main():
    properties = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            properties[key] = value

    config = json.loads((ROOT / "compatibility.json").read_text(encoding="utf-8"))
    for schema in (ROOT / "schemas").glob("*.json"):
        json.loads(schema.read_text(encoding="utf-8"))
    require(config["schema_version"] == 1, "Unknown compatibility schema")
    require(config["required_player_mods"] == ["multisolo"], "Only Multisolo may be required")
    require(config["update_channel"] in {"stable", "preview"}, "Unknown channel")
    require(re.match(re.escape(config["target_series"]) + r"(?:$|[.-])",
                     properties["minecraft_version"]), "Build target differs from target series")

    experience_schema = json.loads((ROOT / "schemas/experience.schema.json").read_text(encoding="utf-8"))
    experiences = list((ROOT / "fabric/src/main/resources/data/multisolo/experiences").glob("*.json"))
    require(len(experiences) == 1, "Connected Solitude starts with exactly one experience")
    for path in experiences:
        data = json.loads(path.read_text(encoding="utf-8"))
        require(set(data) == set(experience_schema["required"]), f"Unexpected experience fields: {path}")
        require(data["schema_version"] == 1, "Unsupported experience schema")
        require(re.fullmatch(r"[a-z0-9_-]+:[a-z0-9_/-]+", data["id"]), "Invalid experience id")
        require(type(data["revision"]) is int and data["revision"] > 0, "Invalid revision")
        require(isinstance(data["seed"], str) and re.fullmatch(r"-?[0-9]{1,19}", data["seed"]), "Invalid seed")
        require(-(2**63) <= int(data["seed"]) < 2**63, "Seed exceeds a signed long")
        require(data["difficulty"] in {"PEACEFUL", "EASY", "NORMAL", "HARD"}, "Invalid difficulty")
        require(data["spawn"] == "vanilla", "Vanilla spawn must be preserved")
        require(data["worldgen_mods"] == {}, "Origin requires no worldgen mods")
        require(data["seed"] == "0" and data["revision"] == 2, "Connected Solitude requires seed 0, revision 2")

    manifest = json.loads((ROOT / "fabric/src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    require(set(manifest["depends"]) == {"fabricloader", "minecraft", "java"}, "Unexpected external mod requirement")
    require(manifest["depends"]["minecraft"] == "=${minecraft_dependency_version}", "Minecraft must be an exact target")
    require(properties["minecraft_dependency_version"], "Loader's normalized version is required")
    languages = ROOT / "fabric/src/client/resources/assets/multisolo/lang"
    french = json.loads((languages / "fr_fr.json").read_text(encoding="utf-8"))
    english = json.loads((languages / "en_us.json").read_text(encoding="utf-8"))
    require(french.keys() == english.keys(), "FR/EN translations differ")
    for path in (ROOT / "fabric/src/client/java").rglob("*.java"):
        for key in re.findall(r'"(multisolo\.[a-z.]+)"', path.read_text(encoding="utf-8")):
            require(key in french, f"Missing UI translation: {key}")

    for path in (ROOT / "core/src").rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        require(not re.search(r"import\s+(net\.minecraft|net\.fabricmc|java\.net)\.", source),
                f"Core depends on platform or network: {path}")

    docs = list(ROOT.glob("*.md")) + list((ROOT / "docs").rglob("*.md"))
    for path in docs:
        for link in re.findall(r"\]\(([^)]+)\)", path.read_text(encoding="utf-8")):
            if "://" in link or link.startswith("#"):
                continue
            target = path.parent / link.split("#", 1)[0]
            require(target.exists(), f"Broken local link in {path.relative_to(ROOT)}: {link}")
    ET.parse(ROOT / "docs/assets/banner.svg")
    print("Repository invariants, JSON files, core boundaries and local links: OK")


if __name__ == "__main__":
    main()
