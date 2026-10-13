#!/usr/bin/env python3
"""Static audit of Ultra*Screens open* builders for broken-looking UI patterns.

Writes sim/out/ultra-ui-audit.md and exits 1 on high-severity issues that are not
mutually exclusive early-return false positives.
"""
from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ULTRA = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra"
OUT_MD = ROOT / "sim/out/ultra-ui-audit.md"
OUT_JSON = ROOT / "sim/out/ultra-ui-audit.json"


def methods_of(text: str):
    for m in re.finditer(
        r"(?:private|public|static)\s+[^{;]+?\b((?:open|paint|build)\w*)\s*\([^)]*\)\s*\{",
        text,
    ):
        name = m.group(1)
        i = text.find("{", m.end() - 1)
        depth = 0
        end = i
        for j in range(i, len(text)):
            if text[j] == "{":
                depth += 1
            elif text[j] == "}":
                depth -= 1
                if depth == 0:
                    end = j + 1
                    break
        yield name, text[m.start() : end]


def mutually_exclusive(body: str, a: str, b: str) -> bool:
    """True when a and b sit on opposite sides of a return; in the same method."""
    ia, ib = body.find(a), body.find(b)
    if ia < 0 or ib < 0:
        return False
    lo, hi = (ia, ib) if ia < ib else (ib, ia)
    return "return;" in body[lo:hi] or "return " in body[lo:hi]


def audit() -> list[dict]:
    findings: list[dict] = []

    def add(sev, file, method, code, detail):
        findings.append(
            {
                "severity": sev,
                "file": file,
                "method": method,
                "code": code,
                "detail": detail,
            }
        )

    for path in sorted(ULTRA.glob("Ultra*Screens.java")):
        text = path.read_text(encoding="utf-8")
        for name, body in methods_of(text):
            has_footer = "footerBackHome()" in body or "footerBackToMain()" in body
            back_labels = re.findall(
                r'(?:dim|confirm|danger)\(\s*"([^"]*[Bb]ack[^"]*)"\s*\)', body
            )
            if has_footer and back_labels:
                if not any(
                    mutually_exclusive(body, lab, "footerBackHome()")
                    or mutually_exclusive(body, lab, "footerBackToMain()")
                    for lab in back_labels
                ):
                    # Also OK if BACK const is only in early-return branch
                    add(
                        "high",
                        path.name,
                        name,
                        "stacked_back",
                        f"footerBack* + Back label(s): {back_labels}",
                    )
            if (
                has_footer
                and "UltraStyle.BACK" in body
                and not mutually_exclusive(body, "UltraStyle.BACK", "footerBackHome()")
            ):
                add(
                    "high",
                    path.name,
                    name,
                    "stacked_back_const",
                    "UltraStyle.BACK button + footerBackHome()",
                )

            # multi_footer only if both calls can run without return between
            footers = [
                m.start()
                for m in re.finditer(r"footerBack(?:Home|ToMain)\(\)", body)
            ]
            if len(footers) > 1:
                between = body[footers[0] : footers[-1]]
                if "return;" not in between and "return " not in between:
                    add(
                        "high",
                        path.name,
                        name,
                        "multi_footer",
                        f"{len(footers)} footer Back calls on one path",
                    )

            if has_footer and re.search(r'dim\(\s*"Cancel"\s*\)', body):
                if not mutually_exclusive(body, 'dim("Cancel")', "footerBackHome()"):
                    add(
                        "high",
                        path.name,
                        name,
                        "cancel_plus_footer",
                        "Cancel + footerBackHome (two exit controls)",
                    )

            for m in re.finditer(
                r'(?:dim|confirm|danger)\(\s*"([^"]*[<>«»][^"]*)"\s*\)', body
            ):
                add(
                    "medium",
                    path.name,
                    name,
                    "angle_bracket_label",
                    f"label has <>/guillemets: {m.group(1)!r}",
                )

            for m in re.finditer(r'"((?:\\.|[^"\\])*)"', body):
                s = m.group(1)
                bad = [c for c in s if ord(c) > 127 and c != "\u00a7"]
                if bad:
                    add(
                        "high",
                        path.name,
                        name,
                        "non_ascii_string",
                        f"non-ASCII {[hex(ord(c)) for c in bad[:4]]}: {s[:60]!r}",
                    )

            if re.search(r"\.textField(Row)?\s*\(", body) and "buttonBound" not in body:
                add(
                    "high",
                    path.name,
                    name,
                    "unbound_text_field",
                    "textField without buttonBound",
                )

            for m in re.finditer(r'"<\s*[A-Za-z]', body):
                add(
                    "medium",
                    path.name,
                    name,
                    "leading_lt",
                    f"leading < in string near {m.group(0)!r}",
                )

    style = (ULTRA / "UltraStyle.java").read_text(encoding="utf-8")
    if 'BACK = LABEL + "<' in style:
        add("high", "UltraStyle.java", "static", "back_angle", "BACK still has leading <")
    if "wireText" not in style:
        add("high", "UltraStyle.java", "static", "no_wiretext", "missing wireText")
    if "fitAsciiLabel" not in style:
        add(
            "high",
            "UltraStyle.java",
            "static",
            "no_fit_ascii",
            "missing fitAsciiLabel (KaiPath fitLabel mojibakes ellipsis)",
        )
    spec = (ULTRA / "UltraScreenSpec.java").read_text(encoding="utf-8")
    line_fn = re.search(
        r"public UltraScreenSpec line\(String text\)\s*\{(.*?)\}", spec, re.S
    )
    if line_fn and "wireText" not in line_fn.group(1):
        add(
            "high",
            "UltraScreenSpec.java",
            "line",
            "line_no_wire",
            "line() does not call wireText",
        )
    if "fitAsciiLabel" not in spec:
        add(
            "high",
            "UltraScreenSpec.java",
            "tabs",
            "tabs_no_fit",
            "tab/button labels must use fitAsciiLabel before wire",
        )

    # Tab label budget: PANEL_W=350, 3 tabs => ~86px text (~14 chars at 6px).
    char_w = 6
    pad = 8
    panel_w = 350
    col = 16
    gap = 6
    for path in sorted(ULTRA.glob("Ultra*Screens.java")):
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r'\.tabs\(([^)]*)\)', text):
            args = m.group(1)
            labels = re.findall(r'"[^"|]*\|([^"]+)"', args)
            if not labels:
                continue
            n = len(labels)
            inner = panel_w - col * 2
            tab_w = max(80, (inner - gap * max(0, n - 1)) // max(1, n))
            text_max = max(char_w, tab_w - pad * 2)
            max_chars = text_max // char_w
            for lab in labels:
                if len(lab) > max_chars:
                    add(
                        "high",
                        path.name,
                        "tabs",
                        "tab_label_overflow",
                        f"{lab!r} len={len(lab)} > budget {max_chars} "
                        f"({n}-up tabs @ {panel_w}px) — KaiPath shows â€¦ mojibake",
                    )

    # de-dupe
    uniq = []
    seen = set()
    for f in findings:
        key = (f["severity"], f["file"], f["method"], f["code"], f["detail"])
        if key in seen:
            continue
        seen.add(key)
        uniq.append(f)
    return uniq


def main() -> int:
    findings = audit()
    by = defaultdict(int)
    for f in findings:
        by[f["severity"]] += 1
    lines = [
        "# Ultra UI menu audit",
        "",
        f"Total findings: **{len(findings)}**",
        "",
    ]
    for sev in ("high", "medium", "low", "info"):
        group = [f for f in findings if f["severity"] == sev]
        if not group:
            continue
        lines.append(f"## {sev.upper()} ({len(group)})")
        lines.append("")
        for f in group:
            lines.append(
                f"- `{f['file']}.{f['method']}` — **{f['code']}**: {f['detail']}"
            )
        lines.append("")
    OUT_MD.parent.mkdir(parents=True, exist_ok=True)
    OUT_MD.write_text("\n".join(lines), encoding="utf-8")
    import json

    OUT_JSON.write_text(
        json.dumps({"summary": dict(by), "count": len(findings), "findings": findings}, indent=2),
        encoding="utf-8",
    )
    print(OUT_MD.read_text(encoding="utf-8"))
    highs = [f for f in findings if f["severity"] == "high"]
    if highs:
        print(f"\n{len(highs)} high-severity issue(s)")
        return 1
    print("\nOK — no high-severity Ultra UI issues")
    return 0


if __name__ == "__main__":
    sys.exit(main())
