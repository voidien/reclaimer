#!/usr/bin/env python3
"""Folds docs/progress/*.md into PROGRESS.md and deletes them: python3 tools/progress_fold.py

Each PR writes its entry to its own docs/progress/<slug>.md, so two PRs never conflict over the same spot in PROGRESS.md.
A file is one or more "## " sections. The sections "## Ideas for the owner" and "## Decisions pending (for the owner)" add
their bullets to those sections of PROGRESS.md; every other section goes in before "## Phases". Files fold in name order.
"""
import pathlib
import sys

PHASES = "## Phases"
LISTS = ("## Ideas for the owner", "## Decisions pending (for the owner)")


def sections(text):
    """Splits a fragment into (heading, body lines) pairs; text before the first heading is an error."""
    found = []
    for line in text.strip().splitlines():
        if line.startswith("## "):
            found.append((line.strip(), []))
        elif found:
            found[-1][1].append(line)
        elif line.strip():
            raise ValueError(f"text before the first '## ' heading: {line[:60]}")
    return found


def add_to_list(lines, heading, bullets):
    """Puts bullets at the end of the named section, before the blank line that closes it."""
    start = lines.index(heading)
    end = start + 1
    while end < len(lines) and not lines[end].startswith("## "):
        end += 1
    while lines[end - 1].strip() == "":
        end -= 1
    lines[end:end] = bullets


def fold(progress, fragments):
    """Returns PROGRESS.md's text with every fragment's text folded in."""
    lines = progress.splitlines()
    for fragment in fragments:
        for heading, body in sections(fragment):
            while body and not body[-1].strip():
                body.pop()
            while body and not body[0].strip():
                body.pop(0)
            if heading in LISTS:
                add_to_list(lines, heading, body)
            else:
                at = lines.index(PHASES)
                lines[at:at] = [heading, *body, ""]
    return "\n".join(lines) + "\n"


def main():
    root = pathlib.Path(__file__).resolve().parent.parent
    files = sorted(f for f in (root / "docs" / "progress").glob("*.md") if f.name != "README.md")
    if not files:
        print("nothing to fold")
        return 0
    target = root / "PROGRESS.md"
    target.write_text(fold(target.read_text(), [f.read_text() for f in files]))
    for f in files:
        f.unlink()
    print(f"folded {len(files)} files into PROGRESS.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
