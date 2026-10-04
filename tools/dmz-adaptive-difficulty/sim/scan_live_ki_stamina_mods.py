#!/usr/bin/env python3
"""Scan live SFTP mods/ for jars that reference DMZ ki/stamina pools. Read-only."""
from __future__ import annotations

import hashlib
import io
import os
import re
import sys
import zipfile

try:
    import paramiko
except ImportError:
    print("pip install paramiko", file=sys.stderr)
    raise

ENTRY_PAT = re.compile(
    r"(?i)(mixin.*\.json$|StatsData|ResourcePool|PoolClamp|RegenGuard|"
    r"getMaxEnergy|getMaxStamina|max_mana|MaxKi|EnergyMana|"
    r"dragonminez/common/stats/character/Resources)"
)

PRIORITY = re.compile(
    r"(?i)(legacy|dragonminez|dmz|fabled|revamp|iron.?spell|extra_gauges|apothic|"
    r"conditional|noea|tleveling|gauge|menace|saga|bridge|mohist.?melee|livingworld)"
)


def load_env() -> None:
    root = os.environ.get("REPO_ROOT", os.path.abspath(os.path.join(os.path.dirname(__file__), "../../..")))
    env = os.path.join(root, "live-sftp.env")
    if os.path.isfile(env):
        for line in open(env, encoding="utf-8"):
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip().strip('"'))


def scan_namelist(data: bytes) -> list[str]:
    zf = zipfile.ZipFile(io.BytesIO(data))
    return [n for n in zf.namelist() if ENTRY_PAT.search(n.replace("\\", "/"))]


def lm_detail(data: bytes) -> dict:
    zf = zipfile.ZipFile(io.BytesIO(data))
    out: dict = {}
    for key in ("legacymechanics.mixins.json", "META-INF/mods.toml"):
        try:
            out[key] = zf.read(key).decode("utf-8", errors="replace")
        except KeyError:
            pass
    pool = "com/dbzlegacy/adaptivedifficulty/progression/DmzResourcePoolClamp.class"
    out["has_pool_clamp"] = pool in zf.namelist()
    return out


def main() -> int:
    load_env()
    host = os.environ.get("LIVE_SFTP_HOST")
    user = os.environ.get("LIVE_SFTP_USER")
    password = os.environ.get("LIVE_SFTP_PASS") or os.environ.get("SSHPASS")
    if not host or not user or not password:
        print("Missing LIVE_SFTP_* env", file=sys.stderr)
        return 1
    port = int(os.environ.get("LIVE_SFTP_PORT", "2022"))
    remote_mods = os.environ.get("LIVE_SFTP_MODS", "mods")
    recycle = os.environ.get("LIVE_SFTP_RECYCLE", "recycle_bin")

    transport = paramiko.Transport((host, port))
    transport.connect(username=user, password=password)
    sftp = paramiko.SFTPClient.from_transport(transport)

    jars = sorted(
        e.filename for e in sftp.listdir_attr(remote_mods) if e.filename.endswith(".jar")
    )
    print(f"Live mods/: {len(jars)} jars\n")

    flagged: dict[str, list[str]] = {}
    for name in jars:
        if "Noea_Build" in name:
            continue
        path = f"{remote_mods}/{name}"
        with sftp.open(path, "rb") as f:
            data = f.read()
        hits = scan_namelist(data)
        if hits or PRIORITY.search(name):
            flagged[name] = hits

    try:
        rec_lm = sorted(x for x in sftp.listdir(recycle) if "LegacyMechanics" in x)
        print("recycle_bin LegacyMechanics:", rec_lm[-10:])
    except OSError:
        pass

    sftp.close()
    transport.close()

    lm_name = "LegacyMechanics-4.5.115.jar"
    if lm_name in flagged:
        transport = paramiko.Transport((host, port))
        transport.connect(username=user, password=password)
        sftp = paramiko.SFTPClient.from_transport(transport)
        with sftp.open(f"{remote_mods}/{lm_name}", "rb") as f:
            lm_data = f.read()
        sftp.close()
        transport.close()
        md5 = hashlib.md5(lm_data).hexdigest()
        print(f"=== {lm_name} ===")
        print(f"  md5: {md5}")
        det = lm_detail(lm_data)
        print(f"  DmzResourcePoolClamp.class: {det.get('has_pool_clamp')}")
        if "legacymechanics.mixins.json" in det:
            print("  mixins.json:\n" + det["legacymechanics.mixins.json"])
        local = os.path.join(
            os.environ.get("REPO_ROOT", "/workspace"), "mods", lm_name
        )
        if os.path.isfile(local):
            local_md5 = hashlib.md5(open(local, "rb").read()).hexdigest()
            print(f"  repo jar md5: {local_md5} ({'MATCH' if local_md5 == md5 else 'DIFF'})")
        print()

    print("=== Jars with ki/stamina-related paths (or DMZ priority name) ===")
    for name in sorted(flagged.keys(), key=str.lower):
        hits = flagged[name]
        tag = "PRIORITY" if PRIORITY.search(name) else "hit"
        print(f"\n[{tag}] {name} ({len(hits)} path hits)")
        for h in hits[:15]:
            print(f"  {h}")
        if len(hits) > 15:
            print(f"  ... +{len(hits) - 15} more")

    non_core = [
        n
        for n in flagged
        if not n.startswith(("dragonminez", "LegacyMechanics"))
        and flagged[n]
    ]
    print(f"\n=== Non-core mods with path hits: {len(non_core)} ===")
    for n in sorted(non_core, key=str.lower):
        print(f"  {n}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
