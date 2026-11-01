#!/usr/bin/env python3
"""Tests for progress_fold.py: python3 tools/test_progress_fold.py"""
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from progress_fold import fold, sections

PROGRESS = """# Progress

## Ideas for the owner
Intro.
- Old idea.

## Decisions pending (for the owner)
- Old decision.

## Old work: done
- Old.

## Phases
1. One.
"""


class Fold(unittest.TestCase):
    def test_a_work_section_goes_before_phases(self):
        out = fold(PROGRESS, ["## New work: done\n- New.\n"])
        self.assertLess(out.index("## Old work"), out.index("## New work"))
        self.assertLess(out.index("## New work"), out.index("## Phases"))

    def test_lists_grow_at_their_end(self):
        out = fold(PROGRESS, ["## Ideas for the owner\n- New idea.\n\n## Decisions pending (for the owner)\n- New decision.\n"])
        self.assertIn("- Old idea.\n- New idea.\n\n## Decisions", out)
        self.assertIn("- Old decision.\n- New decision.\n\n## Old work", out)

    def test_fragments_keep_their_order(self):
        out = fold(PROGRESS, ["## A\n- a\n", "## B\n- b\n"])
        self.assertLess(out.index("## A"), out.index("## B"))

    def test_only_additions(self):
        out = fold(PROGRESS, ["## A\n- a\n"])
        for line in PROGRESS.splitlines():
            self.assertIn(line, out)

    def test_text_before_a_heading_is_refused(self):
        with self.assertRaises(ValueError):
            sections("no heading\n## A\n")


if __name__ == "__main__":
    unittest.main()
