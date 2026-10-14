#!/usr/bin/env python3
"""Rewrite KaiPath UltraWidgetReflect.fitLabel ellipsis constants to ASCII '...'.

Stock KaiPath stores a mojibaked UTF-8 ellipsis (â€¦) in the class constant pool.
On clients that mishandle that string it renders as stray A/E glyphs. Protocol /
bridge docs require ASCII '...' instead.

Usage:
  python3 tools/ultra-gui-kit/patch_fitlabel_ascii.py path/to/UltraWidgetReflect.class
  # rewrites the class in place; exit 0 if already patched or newly patched
"""
from __future__ import annotations

import struct
import sys
from pathlib import Path

# Mojibake of U+2026 as UTF-8 bytes of the three chars â € ¦
MOJIBAKE = bytes([0xC3, 0xA2, 0xE2, 0x82, 0xAC, 0xC2, 0xA6])
ASCII_ELLIPSIS = b"..."


def patch_class(data: bytes) -> tuple[bytes, int]:
    """Return (new_bytes, replacements). Raises if structure looks wrong."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a class file")
    cp_count = struct.unpack_from(">H", data, 8)[0]
    i = 10
    out = bytearray(data[:10])
    replacements = 0
    idx = 1
    while idx < cp_count:
        tag = data[i]
        if tag == 1:  # CONSTANT_Utf8
            ln = struct.unpack_from(">H", data, i + 1)[0]
            payload = data[i + 3 : i + 3 + ln]
            i += 3 + ln
            if payload == MOJIBAKE:
                out.append(1)
                out += struct.pack(">H", len(ASCII_ELLIPSIS))
                out += ASCII_ELLIPSIS
                replacements += 1
            elif payload == b"\x01" + MOJIBAKE:  # StringConcat recipe "\u0001â€¦"
                new_payload = b"\x01" + ASCII_ELLIPSIS
                out.append(1)
                out += struct.pack(">H", len(new_payload))
                out += new_payload
                replacements += 1
            elif payload == ASCII_ELLIPSIS or payload == b"\x01" + ASCII_ELLIPSIS:
                out.append(1)
                out += struct.pack(">H", ln)
                out += payload
            else:
                out.append(1)
                out += struct.pack(">H", ln)
                out += payload
        elif tag in (7, 8, 16, 19, 20):
            out += data[i : i + 3]
            i += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            out += data[i : i + 5]
            i += 5
        elif tag in (5, 6):
            out += data[i : i + 9]
            i += 9
            out  # keep idx bump below
            idx += 1
            cp_count  # silence
        elif tag == 15:
            out += data[i : i + 4]
            i += 4
        else:
            raise ValueError(f"unknown constant pool tag {tag} at {i}")
        idx += 1
    out += data[i:]
    return bytes(out), replacements


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: patch_fitlabel_ascii.py UltraWidgetReflect.class", file=sys.stderr)
        return 2
    path = Path(sys.argv[1])
    raw = path.read_bytes()
    # Already ASCII-only?
    if MOJIBAKE not in raw:
        if ASCII_ELLIPSIS in raw:
            print(f"OK already ASCII ellipsis: {path}")
            return 0
        print(f"WARN no mojibake and no ASCII ellipsis found in {path}", file=sys.stderr)
        return 1
    patched, n = patch_class(raw)
    if n < 1:
        print(f"ERROR mojibake present but not as Utf8 constants in {path}", file=sys.stderr)
        return 1
    if MOJIBAKE in patched:
        print(f"ERROR mojibake still present after patch in {path}", file=sys.stderr)
        return 1
    path.write_bytes(patched)
    print(f"Patched {n} Utf8 constant(s) → ASCII '...' in {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
