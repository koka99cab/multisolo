"""Detect porting candidates from official Fabric metadata; never modify source files."""

import argparse
import json
from pathlib import Path
import re
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
GAME_URL = "https://meta.fabricmc.net/v2/versions/game"
VERSION_PATTERN = re.compile(r"^\d+\.\d+(?:\.\d+)?(?:-(?:snapshot|pre|rc)-\d+)?$")


def read_properties(path):
    return dict(
        line.split("=", 1)
        for raw in path.read_text(encoding="utf-8").splitlines()
        if (line := raw.strip()) and not line.startswith("#") and "=" in line
    )


def numeric_version(version):
    parts = tuple(int(part) for part in version.split("."))
    return parts + (0,) * (3 - len(parts))


def detect_candidates(games, current, series, channel):
    """Fabric returns newest first. Never propose an older target than current."""
    if channel not in {"stable", "preview"}:
        raise ValueError("Unknown update channel")
    if not re.fullmatch(r"\d+\.\d+", series):
        raise ValueError("Invalid target series")
    versions = [game for game in games if VERSION_PATTERN.fullmatch(game["version"])]
    by_id = {game["version"]: i for i, game in enumerate(versions)}
    if current not in by_id:
        raise ValueError(f"Current target {current} is absent from official metadata")
    target_pattern = re.compile(re.escape(series) + r"(?:$|[.-])")
    targets = [
        game for game in versions
        if target_pattern.match(game["version"]) and (channel == "preview" or game["stable"])
    ]
    candidates = []
    if targets and by_id[targets[0]["version"]] < by_id[current]:
        candidates.append({"version": targets[0]["version"], "reason": "target_update"})
    latest_stable = next((game for game in versions if game["stable"]), None)
    if latest_stable and numeric_version(latest_stable["version"]) > numeric_version(series):
        version = latest_stable["version"]
        if version != current and all(item["version"] != version for item in candidates):
            candidates.append({"version": version, "reason": "new_stable_release"})
    return candidates


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "build/update-report.json")
    args = parser.parse_args()
    config = json.loads((ROOT / "compatibility.json").read_text(encoding="utf-8"))
    current = read_properties(ROOT / "gradle.properties")["minecraft_version"]
    request = urllib.request.Request(GAME_URL, headers={"User-Agent": "Multisolo-version-watch/1"})
    # A network or metadata error must fail the job, not pretend everything is current.
    with urllib.request.urlopen(request, timeout=30) as response:
        games = json.load(response)
    report = {
        "current": current,
        "target_series": config["target_series"],
        "channel": config["update_channel"],
        "candidates": detect_candidates(games, current, config["target_series"], config["update_channel"]),
        "source": GAME_URL,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
