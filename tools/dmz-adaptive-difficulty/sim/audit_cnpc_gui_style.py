#!/usr/bin/env python3
"""Fail-closed consistency checks for CustomNPCs Legacy Mechanics menus.

Rules (CNPC + shared with chest/CMI via tooltips audit):
  - Headers use paintHeader, not legacy title()
  - Preview column only on system main (footer parentPage null / hub main)
  - Pick lists: no paintInfoBlock scroll band before scrollPickList (use paintInfoBeforePickList)
  - Lists use scrollPickList (not direct scrollSearchable outside CnpcGuiSupport)
  - Left column width: listWidth/textBandWidth, not W - 2*M
  - Human hints: prefer CnpcGuiStyle.HINT_* over raw "Click to" subtitles
  - Disabled systems: CnpcGuiStyle.MSG_* copy
  - Staff Admin capitalization
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CNPC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc"

LEGACY_HEADER = re.compile(r"CnpcGuiSupport\.title\s*\(\s*gui")
DOUBLE_SEP = re.compile(r"  §8·  ")
LOWERCASE_STAFF_ADMIN = re.compile(r"Staff admin")
SYSTEM_DISABLED = re.compile(r"system is disabled", re.I)
RAW_SCROLL_SEARCH = re.compile(r"CnpcGuiSupport\.scrollSearchable\s*\(")
BAD_LIST_WIDTH = re.compile(r"W\s*-\s*2\s*\*\s*M")
ROBOTIC_HINT = re.compile(
    r'paintHeader\([^)]+,\s*"[^"]*Click (?:to|a|for)[^"]*"\s*\)'
)
# paintInfoBlock uses ID_INFO_LABEL_BASE (10) + line index — section tags must not reuse that band.
BAD_SECTION_TAG_ID = re.compile(
    r"paintSectionTag\s*\(\s*gui\s*,\s*(1[0-9]|2[0-9]|3[0-2])\s*,"
)
PREVIEW_CALL = re.compile(r"paintSystemMainPreview\s*\(")
FOOTER_PREVIEW = re.compile(
    r"if\s*\(\s*parentPage\s*==\s*null(?:\s*\|\|\s*parentPage\.isBlank\(\))?\s*\)\s*\{[^}]*paintSystemMainPreview",
    re.DOTALL,
)
def method_blocks(text: str) -> list[tuple[str, str]]:
    parts = re.split(r"\n    private static void ", text)
    out: list[tuple[str, str]] = []
    for part in parts[1:]:
        name = part.split("(", 1)[0].strip()
        out.append((name, part))
    return out


def scroll_info_conflict(method: str) -> bool:
    if "scrollPickList" not in method:
        return False
    if "paintInfoBeforePickList" in method:
        return False
    if "paintInfoBlock(gui, infoY" not in method:
        return False
    # Empty-list branch only: paintInfoBlock then early return before scrollPickList
    if re.search(
        r"paintInfoBlock\(gui,\s*infoY[\s\S]*?return\s*;[\s\S]*?scrollPickList",
        method,
    ):
        return False
    return True


def main() -> int:
    errors: list[str] = []
    warnings: list[str] = []

    if not CNPC.is_dir():
        print(f"Missing {CNPC}", file=sys.stderr)
        return 1

    for path in sorted(CNPC.glob("CnpcLm*.java")):
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT)

        if LEGACY_HEADER.search(text):
            errors.append(f"{rel}: legacy title() header (use paintHeader)")
        if DOUBLE_SEP.search(text):
            errors.append(f"{rel}: double-spaced §8· (use CnpcGuiStyle.SEP)")
        if LOWERCASE_STAFF_ADMIN.search(text):
            errors.append(f"{rel}: use 'Staff Admin' capitalization")
        if SYSTEM_DISABLED.search(text):
            errors.append(f"{rel}: use CnpcGuiStyle.MSG_* for disabled systems")
        if RAW_SCROLL_SEARCH.search(text):
            errors.append(f"{rel}: call scrollPickList, not scrollSearchable")
        if BAD_LIST_WIDTH.search(text):
            errors.append(f"{rel}: use listWidth/textBandWidth, not W - 2*M")
        if BAD_SECTION_TAG_ID.search(text):
            errors.append(
                f"{rel}: paintSectionTag id overlaps info labels (use CnpcGuiSupport.ID_SECTION_TAG_*)"
            )

        for m in ROBOTIC_HINT.finditer(text):
            snippet = m.group(0)[:80].replace("\n", " ")
            errors.append(f"{rel}: robotic paintHeader hint ({snippet}…) — use CnpcGuiStyle.HINT_*")

        if PREVIEW_CALL.search(text) and "CnpcLmHubGui" not in path.name:
            if "private static void footer" in text or "navFooter" in text:
                if not FOOTER_PREVIEW.search(text):
                    warnings.append(
                        f"{rel}: paintSystemMainPreview present — verify footer gates on parentPage null"
                    )
            elif "paintMain" not in text and "paintMainHub" not in text:
                warnings.append(f"{rel}: preview outside paintMain — verify main-only rule")

        for name, body in method_blocks(text):
            if scroll_info_conflict(body):
                errors.append(
                    f"{rel}::{name}(): paintInfoBlock + scrollPickList (use paintInfoBeforePickList)"
                )

    style = CNPC / "CnpcGuiStyle.java"
    if not style.is_file():
        errors.append("Missing CnpcGuiStyle.java")

    print("=== CNPC GUI menu audit ===")
    for w in warnings:
        print(f"  WARN  {w}")
    for e in errors:
        print(f"  FAIL  {e}")

    if errors:
        print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
        return 1

    print("  OK  headers, scroll/preview rules, hints, layout")
    if warnings:
        print(f"  ({len(warnings)} warning(s))")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
