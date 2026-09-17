#!/usr/bin/env python3
"""Audit gui-tooltips.json for player-facing copy quality.

Checks:
  - Keys referenced from GUI Java (tipBtn/pageBtn) exist in JSON
  - Banned developer jargon in lore
  - Catalog does not override Java-only dynamic lore (costs, unlocks, {vars})
  - Required placeholders preserved where catalog defines lore
  - Very short or empty lore on named buttons
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from gui_tooltip_policy import audit_catalog_preservation

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
    r'"(?:spar|rival|hub|prestige|progression|difficulty|skills|common|character)\.[a-z0-9_.]+"',
)

# Missing newline between lore lines (humanize bug): "...10s)8Prestige" or "messages8Hides"
LORE_CORRUPT_RE = re.compile(r'(?:\d\)|[a-z])8(?=[A-Z&])')

ROBOTIC_CLICK_RE = re.compile(r"&[0-7][^\"]*\bClick to\b", re.I)

BTN_HOVER_RE = re.compile(
    r'btn\(\s*"[^"]*"\s*,\s*"[^"]*"\s*,\s*"([^"]+)"\s*\)',
)

TIP_BTN_LORE_RE = re.compile(
    r'tipBtn\([^,]+,\s*"(?P<key>(?:spar|rival|hub|prestige|progression|difficulty|skills|common)\.[a-z0-9_.]+)"[^)]*List\.of\((?P<lore>[^)]+)\)',
    re.DOTALL,
)

ACTION_BTN_LORE_RE = re.compile(
    r'actionBtn\([^,]+,\s*\d+,\s*"(?P<key>(?:spar|rival|hub|prestige|progression|difficulty|skills|common)\.[a-z0-9_.]+)"[^)]*List\.of\((?P<lore>[^)]+)\)',
    re.DOTALL,
)

def scan_plain_hover(java_path: Path, text: str) -> list[str]:
    """Flag banned jargon in Forge chat-menu btn() hover strings."""
    hits: list[str] = []
    rel = java_path.relative_to(ROOT)
    for m in BTN_HOVER_RE.finditer(text):
        hover = m.group(1)
        for pat in BANNED:
            if re.search(pat, hover, re.I):
                hits.append(f"BANNED HOVER [{rel}]: /{pat}/ → {hover[:72]}")
                break
    return hits


def normalize_lore_blob(blob: str) -> list[str]:
    """Extract static lore lines from a List.of(...) fragment."""
    lines: list[str] = []
    for m in re.finditer(r'"([&§][^"]*)"', blob):
        line = m.group(1)
        if re.search(r"\{[^}]+\}", line):
            continue
        if re.search(r'\+ ph\.getOrDefault\(', blob[m.start():m.end() + 40]):
            continue
        lines.append(line.replace('§', '&'))
    return lines


def scan_chest_cmi_drift() -> list[str]:
    """Chest and CMI fallbacks for the same tooltip key should match."""
    chest = GUI_SRC / "com" / "dbzlegacy" / "adaptivedifficulty" / "bukkit" / "DifficultyChestGui.java"
    cmi = GUI_SRC / "com" / "dbzlegacy" / "adaptivedifficulty" / "bukkit" / "CmiDifficultyGui.java"
    if not chest.is_file() or not cmi.is_file():
        return []
    chest_text = chest.read_text(encoding="utf-8", errors="replace")
    cmi_text = cmi.read_text(encoding="utf-8", errors="replace")
    chest_map: dict[str, list[str]] = {}
    cmi_map: dict[str, list[str]] = {}
    for m in TIP_BTN_LORE_RE.finditer(chest_text):
        chest_map[m.group("key")] = normalize_lore_blob(m.group("lore"))
    for m in ACTION_BTN_LORE_RE.finditer(cmi_text):
        cmi_map[m.group("key")] = normalize_lore_blob(m.group("lore"))
    errors: list[str] = []
    for key in sorted(set(chest_map) & set(cmi_map)):
        c_lines = [ln for ln in chest_map[key] if not ln.endswith("Current mode") and not ln.endswith("Click to select")]
        m_lines = [ln for ln in cmi_map[key] if not ln.endswith("Current mode") and not ln.endswith("Click to select")]
        if c_lines != m_lines:
            errors.append(
                f"CHEST/CMI DRIFT [{key}]: chest={c_lines!r} cmi={m_lines!r}"
            )
    return errors

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


def scan_corruption(java_path: Path, text: str) -> list[str]:
    rel = java_path.relative_to(ROOT)
    hits: list[str] = []
    for m in re.finditer(r'"([&§][^"]*)"', text):
        literal = m.group(1)
        if LORE_CORRUPT_RE.search(literal):
            hits.append(f"CORRUPT LORE [{rel}]: {literal[:90]}")
    return hits


def scan_robotic_click(java_path: Path, text: str) -> list[str]:
    rel = java_path.relative_to(ROOT)
    hits: list[str] = []
    for m in re.finditer(r'"([&§][^"]*)"', text):
        literal = m.group(1)
        if literal.startswith("&8") or literal.startswith("§8"):
            continue
        if ROBOTIC_CLICK_RE.search(literal):
            hits.append(f"ROBOTIC [{rel}]: {literal[:90]}")
    return hits


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

    errors.extend(audit_catalog_preservation(catalog))

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
        errors.extend(scan_corruption(java, text))

    if FORGE_GUI_SRC.is_dir():
        for java in FORGE_GUI_SRC.rglob("*.java"):
            text = java.read_text(encoding="utf-8", errors="replace")
            errors.extend(scan_java_banned(java, text))
            if java.name.endswith("ChatMenu.java"):
                errors.extend(scan_plain_hover(java, text))

    errors.extend(scan_chest_cmi_drift())

    robotic: list[str] = []
    for java in GUI_SRC.rglob("*.java"):
        if java.name in ("ForgeBridge.java", "GuiTooltips.java"):
            continue
        text = java.read_text(encoding="utf-8", errors="replace")
        robotic.extend(scan_robotic_click(java, text))

    out_md = ROOT / "tools" / "dmz-adaptive-difficulty" / "sim" / "out" / "gui-humanization-audit.md"
    out_md.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "# GUI humanization audit",
        "",
        f"- Catalog keys: **{len(catalog)}** · Referenced from Java: **{len(used)}**",
        f"- GuiTooltips catalog revision: see `GuiTooltips.CATALOG_REVISION` / `gui-tooltips.json` `_catalogRevision`",
        "",
        "## Summary",
        "",
    ]
    if errors:
        lines.append(f"- **{len(errors)} blocking issue(s)** (missing keys, banned jargon, corrupt lore)")
    else:
        lines.append("- No blocking issues")
    lines.append(f"- **{len(robotic)}** Java fallback lines still use robotic \"Click to…\" (use `&8Tap…` or catalog keys)")
    lines.append(f"- **{len(warns)}** catalog warnings")
    lines.append("")
    lines.append("## Icons (materials)")
    lines.append("")
    lines.append("| Pattern | Use |")
    lines.append("|---------|-----|")
    lines.append("| `LIME_DYE` / `GRAY_DYE` | Personal toggles ON/OFF, staff flags, coin bypass |")
    lines.append("| `GOLD_INGOT` | Ancient Coins / economy |")
    lines.append("| `BELL` | Chat message toggles ON |")
    lines.append("| `DRAGON_EGG` / `GRAY_DYE` | End dragon summon ready / locked |")
    lines.append("| `REPEATER` | Staff admin / flag boards |")
    lines.append("| Tier mats `COPPER`→`NETHER_STAR` | Difficulty tiers T1–T7 |")
    lines.append("")
    if errors:
        lines.append("## Blocking issues")
        lines.append("")
        for e in errors:
            lines.append(f"- {e}")
        lines.append("")
    if robotic:
        lines.append("## Robotic fallback lore (sample)")
        lines.append("")
        for r in robotic[:40]:
            lines.append(f"- {r}")
        if len(robotic) > 40:
            lines.append(f"- … and {len(robotic) - 40} more")
        lines.append("")
    lines.append("## Humanize safety")
    lines.append("")
    lines.append("- Catalog **lore** replaces Java fallback lore when present — do not add short static lore on dynamic buttons (tier cost, DMZ/Prestige gates, `{action}` toggles).")
    lines.append("- `JAVA_LORE_ONLY_KEYS` in `gui_tooltip_policy.py` — catalog must omit `lore` for those keys.")
    lines.append("- **Icons** (`Material.*`) are only changed in Java GUIs, not by humanize scripts.")
    lines.append("")
    lines.append("## Maintenance")
    lines.append("")
    lines.append("- Edit `gui-tooltips.json` then `/lm admin reload`")
    lines.append("- Run `humanize_gui_tooltips.py` for phrase polish only; then `audit_gui_tooltips.py` (must PASS)")
    lines.append("")
    out_md.write_text("\n".join(lines), encoding="utf-8")

    print(f"# GUI tooltip audit — {len(catalog)} keys, {len(used)} referenced from Java\n")
    print(f"Wrote {out_md.relative_to(ROOT)}\n")
    for w in warns:
        print(w)
    for e in errors:
        print(e)
    if robotic:
        print(f"\nROBOTIC CLICK ({len(robotic)} lines) — see audit markdown")

    if errors:
        print(f"\nFAIL — {len(errors)} issue(s), {len(warns)} warning(s)")
        return 1
    print(f"\nPASS — {len(warns)} warning(s), {len(robotic)} robotic fallback(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
