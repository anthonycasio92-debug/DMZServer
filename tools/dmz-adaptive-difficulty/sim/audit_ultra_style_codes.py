#!/usr/bin/env python3
"""Fail if gui/ultra screen builders embed raw section-sign color codes.

Scans Java sources for bare section-sign characters, and scans jar/class
constant-pool UTF-8 strings (not raw bytecode — JVM goto is 0xA7 and false-positives
as UTF-8 § when preceded by 0xC2).
"""
from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra"
MARK_CHAR = "\u00a7"
MARK = MARK_CHAR.encode("utf-8")  # C2 A7


def utf8_constant_pool_strings(data: bytes) -> list[bytes]:
    if len(data) < 10 or data[:4] != b"\xca\xfe\xba\xbe":
        return []
    cp_count = struct.unpack(">H", data[8:10])[0]
    i = 10
    n = 1
    out: list[bytes] = []
    while n < cp_count and i < len(data):
        tag = data[i]
        i += 1
        if tag == 1:  # CONSTANT_Utf8
            ln = struct.unpack(">H", data[i : i + 2])[0]
            i += 2
            out.append(data[i : i + ln])
            i += ln
        elif tag in (3, 4):
            i += 4
        elif tag in (5, 6):
            i += 8
            n += 1
        elif tag in (7, 8, 16, 19, 20):
            i += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            i += 4
        elif tag == 15:
            i += 3
        else:
            break
        n += 1
    return out


def audit_sources() -> list[str]:
    errors: list[str] = []
    if not SRC.is_dir():
        return [f"Missing {SRC}"]
    for path in sorted(SRC.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for i, line in enumerate(text.splitlines(), 1):
            if MARK_CHAR in line:
                errors.append(
                    f"{path.name}:{i}: bare section-sign — use UltraStyle / char 0xA7 helper"
                )
    return errors


def audit_class_bytes(name: str, data: bytes) -> list[str]:
    short = name.rsplit("/", 1)[-1]
    if short.startswith("UltraStyle"):
        return []
    hits = [s for s in utf8_constant_pool_strings(data) if MARK in s]
    if not hits:
        return []
    sample = hits[0][:60]
    try:
        shown = sample.decode("utf-8")
    except UnicodeDecodeError:
        shown = repr(sample)
    return [f"{short}: {len(hits)} UTF-8 section-sign string(s) e.g. {shown!r}"]


def audit_jar(jar: Path) -> list[str]:
    errors: list[str] = []
    if not jar.is_file():
        return [f"Missing jar {jar}"]
    with zipfile.ZipFile(jar) as zf:
        for name in sorted(zf.namelist()):
            norm = name.replace("\\", "/")
            if not norm.endswith(".class") or "/gui/ultra/" not in norm:
                continue
            errors.extend(audit_class_bytes(norm, zf.read(name)))
    return errors


def main() -> int:
    errors = audit_sources()
    jar_arg = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    if jar_arg is None:
        mods = ROOT.parents[1] / "mods"
        jars = sorted(mods.glob("LegacyMechanics-*.jar"), key=lambda p: p.name)
        jar_arg = jars[-1] if jars else None
    if jar_arg is not None:
        errors.extend(audit_jar(jar_arg))
    print("=== Ultra style codes (no bare § outside UltraStyle) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  ultra screens use UltraStyle only; jar has no folded § in callers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
