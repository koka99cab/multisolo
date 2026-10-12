import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check_minecraft_updates import detect_candidates


class UpdateDetectionTest(unittest.TestCase):
    def setUp(self):
        self.current = "26.4-snapshot-3"
        self.games = [
            {"version": self.current, "stable": False},
            {"version": "26.4-snapshot-2", "stable": False},
            {"version": "26.3", "stable": True},
        ]

    def detect(self, games=None, channel="preview"):
        return detect_candidates(self.games if games is None else games, self.current, "26.4", channel)

    def test_current_target_produces_no_alert(self):
        self.assertEqual([], self.detect())

    def test_preview_detects_new_snapshot(self):
        games = [{"version": "26.4-snapshot-4", "stable": False}] + self.games
        self.assertEqual([{"version": "26.4-snapshot-4", "reason": "target_update"}], self.detect(games))

    def test_stable_channel_ignores_preview_and_does_not_downgrade(self):
        games = [{"version": "26.4-snapshot-4", "stable": False}] + self.games
        self.assertEqual([], self.detect(games, channel="stable"))

    def test_final_release_creates_only_one_candidate(self):
        games = [{"version": "26.4", "stable": True}] + self.games
        self.assertEqual([{"version": "26.4", "reason": "target_update"}], self.detect(games))

    def test_new_stable_series_is_detected(self):
        games = [{"version": "26.5", "stable": True}] + self.games
        self.assertEqual([{"version": "26.5", "reason": "new_stable_release"}], self.detect(games))

    def test_other_series_snapshot_is_not_selected(self):
        games = [{"version": "26.5-snapshot-1", "stable": False}] + self.games
        self.assertEqual([], self.detect(games))

    def test_same_prefix_different_series_is_not_a_target_update(self):
        games = [{"version": "26.40-snapshot-1", "stable": False}] + self.games
        self.assertEqual([], self.detect(games))

    def test_missing_current_and_invalid_channel_fail(self):
        with self.assertRaises(ValueError):
            self.detect([])
        with self.assertRaises(ValueError):
            self.detect(channel="anything")


if __name__ == "__main__":
    unittest.main()
