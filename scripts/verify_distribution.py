"""Verify that the built mod contains its rules and declares no extra player mod."""

import argparse
import json
from pathlib import Path
import zipfile
from check_minecraft_updates import read_properties
from verify_repository import require

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bootstrap-log", type=Path)
    args = parser.parse_args()
    properties = read_properties(ROOT / "gradle.properties")
    jar_path = ROOT / f"fabric/build/libs/multisolo-{properties['mod_version']}.jar"
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        manifest = json.loads(jar.read("fabric.mod.json"))
        require(manifest["depends"] == {
            "fabricloader": ">=" + properties["loader_version"],
            "minecraft": "=" + properties["minecraft_dependency_version"],
            "java": ">=" + properties["java_version"],
        }, "Built manifest has unexpected dependencies")
        require("io/github/koka99cab/multisolo/core/assistance/GuestPolicy.class" in names,
                "Core is missing from the single player JAR")
        require("data/multisolo/experiences/origin.json" in names, "Experience is missing")
        experience = json.loads(jar.read("data/multisolo/experiences/origin.json"))
        require(experience["seed"] == "0" and experience["revision"] == 2, "Incorrect experience in JAR")
        require("io/github/koka99cab/multisolo/network/NetworkIdentity.class" in names, "Network is missing")
        require("multisolo.client.mixins.json" in names, "Client sign mixins are missing")
        require("io/github/koka99cab/multisolo/fabric/client/SignEditorSession.class" in names, "Vanilla sign editor integration is missing")
        nested_ids = set()
        import io
        for entry in manifest.get("jars", []):
            with zipfile.ZipFile(io.BytesIO(jar.read(entry["file"]))) as nested:
                nested_manifest = json.loads(nested.read("fabric.mod.json"))
                nested_ids.add(nested_manifest["id"])
                require(any("LICENSE" in name.upper() for name in nested.namelist()), "Nested license missing")
        require(nested_ids == {"fabric-api-base", "fabric-lifecycle-events-v1", "fabric-screen-api-v1", "fabric-resource-loader-v1"},
                f"Unexpected bundled modules: {nested_ids}")
        require("assets/multisolo/lang/fr_fr.json" in names, "Client resources are missing")
        require(not any("gametest" in name.lower() for name in names), "Test code leaked into the player JAR")
        require("LICENSE_multisolo" in names, "Mod license is missing")
        require(not any(name.startswith("net/minecraft/") for name in names),
                "Do not redistribute Minecraft classes")
    if args.bootstrap_log:
        log = args.bootstrap_log.read_text(encoding="utf-8")
        require("Multisolo Connected Solitude loaded: multisolo:origin revision 2" in log,
                "Bootstrap did not reach Multisolo's initializer")
        require("Mod resolution failed" not in log, "Fabric dependency resolution failed")
    print(f"Single-JAR distribution verified: {jar_path.name}")


if __name__ == "__main__":
    main()
