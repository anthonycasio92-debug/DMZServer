#!/usr/bin/env python3
"""Audit gui-tooltips.json for player-facing copy quality.

Checks:
  - Keys referenced from GUI Java (tipBtn/pageBtn) exist in JSON
  - Banned developer jargon in lore
  - Very short or empty lore on named buttons
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TOOLTIPS = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "resources" / "gui-tooltips.json"
GUI_SRC = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "java"
FORGE_GUI_SRC = (
    ROOT / "tools" / "dmz-adaptive-difficulty" / "src" / "main" / "java"
    / "com" / "dbzlegacy" / "adaptivedifficulty" / "gui"
)

BANNED = [
    r"Legacy deep-link",
    r"deep-link",
    r"mentor resetcd",
    r"sparring\.json",
    r"progression-v\d",
    r"androidforms\.",
    r"androidforms",
    r"superforms",
    r"legendaryforms",
    r"Bridge returned",
    r"Skill check ADMIN",
    r"inter-dojo",
    r"\bSYSLOG\b",
    r"telemetry",
    r"Legacy Mechanics hub",
    r"CNPC script",
    r"Flush log writers",
    r"Logging ON",
    r"Logging OFF",
    r"teamBonusPercent",
    r"contributionPercent",
    r"opted-in",
    r"opted in",
    r"\bOpt-in\b",
    r"\bWIP\b",
]

KEY_RE = re.compile(
    r'"(?:spar|rival|hub|prestige|progression|difficulty|skills|common)\.[a-z0-9_.]+"',
)

def scan_java_banned(java_path: Path, text: str) -> list[str]:
    """Flag banned jargon only in player-facing string literals (& / § lore)."""
    hits: list[str] = []
    rel = java_path.relative_to(ROOT)
    for m in re.finditer(r'"([&§][^"]*)"', text):
        literal = m.group(1)
        if not re.match(r"[&§][0-9a-fklmnor]", literal, re.I):
            continue
        # Staff slash-command hints are allowed to mirror real command names.
        if literal.startswith("&8/") or literal.startswith("§8/"):
            continue
        for pat in BANNED:
            if re.search(pat, literal, re.I):
                hits.append(f"BANNED JAVA [{rel}]: /{pat}/ → {literal[:72]}")
                break
    return hits


def flatten_keys(obj: dict, prefix: str = "") -> dict[str, dict]:
    out: dict[str, dict] = {}
    for k, v in obj.items():
        if k.startswith("_"):
            continue
        path = f"{prefix}.{k}" if prefix else k
        if isinstance(v, dict) and "name" not in v and "lore" not in v:
            out.update(flatten_keys(v, path))
        else:
            out[path] = v if isinstance(v, dict) else {"lore": v}
    return out


def lore_lines(entry: dict) -> list[str]:
    raw = entry.get("lore")
    if raw is None:
        return []
    if isinstance(raw, list):
        return [str(x) for x in raw]
    return [str(raw)]


def main() -> int:
    if not TOOLTIPS.is_file():
        print(f"MISSING {TOOLTIPS}", file=sys.stderr)
        return 2
    data = json.loads(TOOLTIPS.read_text(encoding="utf-8"))
    catalog = flatten_keys(data)

    used: set[str] = set()
    for java in GUI_SRC.rglob("*.java"):
        if java.name == "ForgeBridge.java":
            continue
        text = java.read_text(encoding="utf-8", errors="replace")
        for m in KEY_RE.finditer(text):
            key = m.group(0)[1:-1]
            if key.endswith("."):
                continue
            used.add(key)

    errors: list[str] = []
    warns: list[str] = []

    for key in sorted(used):
        if key not in catalog:
            errors.append(f"MISSING KEY: {key}")

    for key, entry in sorted(catalog.items()):
        if not isinstance(entry, dict):
            continue
        lines = lore_lines(entry)
        plain = " ".join(lines)
        for pat in BANNED:
            if re.search(pat, plain, re.I):
                # Staff slash-command hints in lore are allowed.
                if all(
                        ln.strip().startswith("&8/") or ln.strip().startswith("§8/")
                        for ln in lines
                        if re.search(pat, ln, re.I)
                ):
                    continue
                errors.append(f"BANNED [{key}]: matches /{pat}/ → {plain[:80]}")
        if key in used and entry.get("name") and len(lines) == 0:
            warns.append(f"WARN [{key}]: name only, no lore")
        if key in used:
            content = [ln.strip() for ln in lines if ln.strip() and ln.strip() != ""]
            if entry.get("name") and not content:
                warns.append(f"WARN [{key}]: empty lore")

    for java in GUI_SRC.rglob("*.java"):
        if java.name in (
                "ForgeBridge.java",
                "GuiTooltips.java",
                "GuiChat.java",
                "AdaptiveDifficultyGuiPlugin.java",
        ):
            continue
        text = java.read_text(encoding="utf-8", errors="replace")
        errors.extend(scan_java_banned(java, text))

    if FORGE_GUI_SRC.is_dir():
        for java in FORGE_GUI_SRC.rglob("*.java"):
            text = java.read_text(encoding="utf-8", errors="replace")
            errors.extend(scan_java_banned(java, text))

    print(f"# GUI tooltip audit — {len(catalog)} keys, {len(used)} referenced from Java\n")
    for w in warns:
        print(w)
    for e in errors:
        print(e)

    if errors:
        print(f"\nFAIL — {len(errors)} issue(s), {len(warns)} warning(s)")
        return 1
    print(f"\nPASS — {len(warns)} warning(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
