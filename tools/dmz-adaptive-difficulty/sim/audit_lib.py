"""Shared helpers for LegacyMechanics audit scripts."""
from __future__ import annotations

import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
MOD_ROOT = REPO / "tools" / "dmz-adaptive-difficulty"
MOD_JAVA = MOD_ROOT / "src" / "main" / "java"
MOD_SRC = MOD_JAVA / "com/dbzlegacy/adaptivedifficulty"
PROFILE = MOD_SRC / "calc/PlayerCombatProfile.java"
MOD_CLASS = MOD_SRC / "AdaptiveDifficultyMod.java"
CFG = MOD_SRC / "config/DifficultyConfig.java"
SIMULATE_MATRIX = Path(__file__).resolve().parent / "simulate_build_matrix.py"


def mod_version() -> str:
    text = MOD_CLASS.read_text(encoding="utf-8", errors="replace")
    m = re.search(r'VERSION\s*=\s*"([0-9.]+)"', text)
    if not m:
        raise RuntimeError(f"Could not read VERSION from {MOD_CLASS}")
    return m.group(1)


def read_profile() -> str:
    return PROFILE.read_text(encoding="utf-8", errors="replace")


def read_config() -> str:
    return CFG.read_text(encoding="utf-8", errors="replace")


def extract_method(source: str, name: str) -> str:
    """Return the body of a package-private or public method by name."""
    pattern = re.compile(
        rf"(?:public|private|protected)\s+[\w<>,\s\[\]]+\s+{re.escape(name)}\s*\([^)]*\)\s*\{{",
        re.MULTILINE,
    )
    m = pattern.search(source)
    if not m:
        return ""
    start = m.end() - 1
    depth = 0
    for i in range(start, len(source)):
        ch = source[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return source[start : i + 1]
    return ""


def java_case_literal(tier: int, value: float, default_tier: int | None = None) -> str:
    """Format a switch arm the way PlayerCombatProfile uses it."""
    if default_tier is not None and tier == default_tier:
        if float(int(value)) == value:
            return f"default -> {int(value) if value == int(value) else value}"
        return f"default -> {value}"
    if float(int(value)) == value and value >= 1.0:
        return f"case {tier} -> {int(value) if value == int(value) else value}"
    if float(int(value)) == value:
        return f"case {tier} -> {value:.2f}".rstrip("0").rstrip(".")
    return f"case {tier} -> {value}"


def contains_literal(haystack: str, tier: int, value: float, default_tier: int | None = None) -> bool:
    lit = java_case_literal(tier, value, default_tier)
    return lit in haystack
