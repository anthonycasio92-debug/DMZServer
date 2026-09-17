"""Shared rules for GUI tooltip humanization and audits.

Catalog lore in gui-tooltips.json **replaces** Java fallback lore when present
(see GuiTooltips.lore). Humanize passes must not strip costs, unlock gates,
placeholders, or state-specific lines by adding short static catalog text.

Icons/materials live in Java (Material.*) — humanize scripts only edit JSON.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TOOLTIPS = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "resources" / "gui-tooltips.json"
GUI_TOOLTIPS_JAVA = (
    ROOT
    / "tools"
    / "dmz-adaptive-difficulty-gui"
    / "src"
    / "main"
    / "java"
    / "com"
    / "dbzlegacy"
    / "adaptivedifficulty"
    / "bukkit"
    / "GuiTooltips.java"
)
GUI_SRC = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "java"

# Catalog must not define lore[] — Java builds per-player state (cost, lock, toggles).
JAVA_LORE_ONLY_KEYS: frozenset[str] = frozenset({
    "difficulty.tiers.tier",
})

# Catalog lore may exist but must keep these placeholders (filled from Java vars).
REQUIRED_PLACEHOLDERS: dict[str, frozenset[str]] = {
    "progression.economy.staff_free": frozenset({"action"}),
    "difficulty.titles.item": frozenset({"rarity_line", "req", "perk"}),
}

PLACEHOLDER_RE = re.compile(r"\{([a-zA-Z_][a-zA-Z0-9_]*)\}")

TIP_KEY_LORE_RE = re.compile(
    r'tipBtn\(\s*(?:[^,]+,\s*)?'
    r'"(?P<key>(?:spar|rival|hub|prestige|progression|difficulty|skills|common|character)\.[a-z0-9_.]+)"'
    r'[^)]*List\.of\((?P<lore>.*?)\)\s*(?:,\s*(?P<vars>\w+))?',
    re.DOTALL,
)

ACTION_KEY_LORE_RE = re.compile(
    r'actionBtn\(\s*(?:[^,]+,\s*){1,3}'
    r'"(?P<key>(?:spar|rival|hub|prestige|progression|difficulty|skills|common|character)\.[a-z0-9_.]+)"'
    r'[^)]*List\.of\((?P<lore>.*?)\)\s*(?:,\s*(?P<vars>\w+))?',
    re.DOTALL,
)


def read_catalog_revision_from_java() -> int:
    if not GUI_TOOLTIPS_JAVA.is_file():
        return 0
    text = GUI_TOOLTIPS_JAVA.read_text(encoding="utf-8")
    m = re.search(r"CATALOG_REVISION\s*=\s*(\d+)", text)
    return int(m.group(1)) if m else 0


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


def placeholders_in_lines(lines: list[str]) -> set[str]:
    found: set[str] = set()
    for line in lines:
        found.update(PLACEHOLDER_RE.findall(line))
    return found


def java_fallback_placeholders() -> dict[str, set[str]]:
    """Placeholders in Java List.of(...) lore for tooltip keys (static lines only)."""
    out: dict[str, set[str]] = {}
    for java in GUI_SRC.rglob("*.java"):
        if java.name in ("GuiTooltips.java", "ForgeBridge.java"):
            continue
        text = java.read_text(encoding="utf-8", errors="replace")
        for m in TIP_KEY_LORE_RE.finditer(text):
            key = m.group("key")
            blob = m.group("lore")
            lines = re.findall(r'"([&§][^"]*)"', blob)
            out.setdefault(key, set()).update(placeholders_in_lines(lines))
        for m in ACTION_KEY_LORE_RE.finditer(text):
            key = m.group("key")
            blob = m.group("lore")
            lines = re.findall(r'"([&§][^"]*)"', blob)
            out.setdefault(key, set()).update(placeholders_in_lines(lines))
    # Shared helper — same placeholders as tier buttons.
    helper = GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/GuiBoardHelper.java"
    if helper.is_file():
        htext = helper.read_text(encoding="utf-8", errors="replace")
        if "difficultyTierButtonLore" in htext:
            lines = re.findall(r'"([&§][^"]*)"', htext)
            tier_ph = placeholders_in_lines(lines)
            if tier_ph:
                out.setdefault("difficulty.tiers.tier", set()).update(tier_ph)
    return out


def strip_java_only_catalog_lore(root: dict) -> list[str]:
    """Remove catalog lore for JAVA_LORE_ONLY_KEYS. Returns keys changed."""
    changed: list[str] = []
    flat = flatten_keys(root)
    for key in JAVA_LORE_ONLY_KEYS:
        entry = flat.get(key)
        if not entry or "lore" not in entry:
            continue
        parts = key.split(".")
        node = root
        for p in parts[:-1]:
            node = node.get(p, {})
        leaf = parts[-1]
        if isinstance(node.get(leaf), dict) and "lore" in node[leaf]:
            del node[leaf]["lore"]
            changed.append(key)
    return changed


def audit_catalog_preservation(catalog: dict[str, dict]) -> list[str]:
    errors: list[str] = []
    java_ph = java_fallback_placeholders()

    for key in JAVA_LORE_ONLY_KEYS:
        entry = catalog.get(key)
        if entry and entry.get("lore"):
            errors.append(
                f"CATALOG LORE BLOCKED [{key}]: lore must be omitted — Java builds cost/unlock/state"
            )

    for key, required in REQUIRED_PLACEHOLDERS.items():
        if key in JAVA_LORE_ONLY_KEYS:
            continue
        entry = catalog.get(key)
        if not entry or not entry.get("lore"):
            continue
        cat_lines = entry.get("lore")
        if not isinstance(cat_lines, list):
            continue
        cat_ph = placeholders_in_lines([str(x) for x in cat_lines])
        missing = required - cat_ph
        if missing:
            errors.append(
                f"MISSING PLACEHOLDER [{key}]: catalog lore needs {sorted(missing)}"
            )

    for key, java_vars in sorted(java_ph.items()):
        entry = catalog.get(key)
        if not entry or not entry.get("lore"):
            continue
        cat_lines = [str(x) for x in entry.get("lore", [])]
        cat_ph = placeholders_in_lines(cat_lines)
        # Java uses these at runtime; catalog must not drop them.
        critical = java_vars & {"cost", "action", "req_level", "req_prestige", "level", "prestige", "req", "tier"}
        missing = critical - cat_ph
        if missing and key not in JAVA_LORE_ONLY_KEYS:
            errors.append(
                f"JAVA PLACEHOLDER DROP [{key}]: catalog lore missing {sorted(missing)} "
                f"(Java fallbacks use them)"
            )
        # Static catalog replaced a rich Java fallback.
        if len(cat_lines) <= 2 and len(java_vars) >= 3 and key not in JAVA_LORE_ONLY_KEYS:
            errors.append(
                f"CATALOG LORE SHRINK [{key}]: only {len(cat_lines)} catalog line(s) but "
                f"Java uses placeholders {sorted(java_vars)} — prefer Java defaults or match placeholders"
            )

    return errors
