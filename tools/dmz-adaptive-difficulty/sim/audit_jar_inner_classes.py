#!/usr/bin/env python3
"""Fail the build when a shipped class names an LM inner class that is not in the jar.

Constant-pool UTF8 entries for com/dbzlegacy/adaptivedifficulty/.../Outer$Inner must
have a matching .class file. This is the check that would have stopped the 4.6.17
GuiClickConfirm$Pending crash before deploy.
"""
from __future__ import annotations

import re
import sys
import zipfile
from pathlib import Path

PREFIX = b"com/dbzlegacy/adaptivedifficulty/"
INNER = re.compile(
    rb"com/dbzlegacy/adaptivedifficulty/(?:[A-Za-z0-9_]+/)*[A-Za-z0-9_]+(?:\$[A-Za-z0-9_]+)+"
)


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: audit_jar_inner_classes.py <jar>", file=sys.stderr)
        return 2
    jar = Path(sys.argv[1])
    if not jar.is_file():
        print(f"ERROR: missing jar {jar}", file=sys.stderr)
        return 1
    missing: dict[str, set[str]] = {}
    with zipfile.ZipFile(jar) as zf:
        names = set(zf.namelist())
        for info in zf.infolist():
            if not info.filename.endswith(".class"):
                continue
            data = zf.read(info.filename)
            if PREFIX not in data:
                continue
            for match in INNER.finditer(data):
                ref = match.group(0).decode("ascii")
                path = ref + ".class"
                if path not in names:
                    missing.setdefault(path, set()).add(info.filename)
    if missing:
        print("ERROR: jar references inner classes that were not packaged:", file=sys.stderr)
        for path in sorted(missing):
            users = ", ".join(sorted(missing[path])[:8])
            print(f"  missing {path} (referenced by {users})", file=sys.stderr)
        return 1
    print(f"PASS — inner classes referenced in {jar.name} are packaged")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
