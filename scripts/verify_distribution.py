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
        require("LICENSE_multisolo" in names, "Mod license is missing")
        require(not any(name.startswith("net/minecraft/") for name in names),
                "Do not redistribute Minecraft classes")
    if args.bootstrap_log:
        log = args.bootstrap_log.read_text(encoding="utf-8")
        require("Multisolo foundation loaded: multisolo:origin revision 1" in log,
                "Bootstrap did not reach Multisolo's initializer")
        require("Mod resolution failed" not in log, "Fabric dependency resolution failed")
    print(f"Single-JAR distribution verified: {jar_path.name}")


if __name__ == "__main__":
    main()
