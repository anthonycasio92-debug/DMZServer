#!/usr/bin/env python3
"""Upload kaipath-*.jar to live SFTP; archive previous kaipath jars first."""
from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load_live_env() -> None:
    env_file = ROOT / "live-sftp.env"
    if not env_file.is_file():
        return
    for line in env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, val = line.split("=", 1)
        os.environ.setdefault(key.strip(), val.strip().strip('"').strip("'"))


def connect_sftp():
    import paramiko

    host = os.environ.get("LIVE_SFTP_HOST", "")
    user = os.environ.get("LIVE_SFTP_USER", "")
    password = os.environ.get("LIVE_SFTP_PASS") or os.environ.get("SSHPASS", "")
    port = int(os.environ.get("LIVE_SFTP_PORT", "2022"))
    if not host or not user or not password:
        print("Missing LIVE_SFTP_HOST, LIVE_SFTP_USER, or LIVE_SFTP_PASS", file=sys.stderr)
        sys.exit(1)
    transport = paramiko.Transport((host, port))
    transport.connect(username=user, password=password)
    return paramiko.SFTPClient.from_transport(transport), transport


def ensure_dir(sftp, path: str) -> None:
    parts = [p for p in path.split("/") if p]
    cur = ""
    for part in parts:
        cur = f"{cur}/{part}" if cur else part
        try:
            sftp.mkdir(cur)
        except OSError:
            pass


def clear_remote_jar(sftp, remote_path: str, recycle_dir: str) -> str:
    name = remote_path.rsplit("/", 1)[-1]
    dest = f"{recycle_dir}/{name}"
    try:
        sftp.stat(remote_path)
    except OSError:
        return "absent"
    try:
        sftp.remove(dest)
    except OSError:
        try:
            sftp.rename(dest, f"{dest}.{int(time.time())}.bak")
        except OSError:
            pass
    try:
        sftp.rename(remote_path, dest)
        return f"archived -> {dest}"
    except OSError:
        sftp.remove(remote_path)
        return "removed (recycle rename failed)"


def list_kaipath_jars(sftp, remote_mods: str) -> list[str]:
    try:
        names = sftp.listdir(remote_mods)
    except OSError:
        return []
    return sorted(
        n
        for n in names
        if n.lower().startswith("kaipath") and n.endswith(".jar")
    )


def main() -> int:
    load_live_env()
    try:
        import paramiko  # noqa: F401
    except ImportError:
        print("paramiko required", file=sys.stderr)
        return 1

    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True, type=Path)
    args = parser.parse_args()
    if not args.jar.is_file():
        print(f"Missing jar: {args.jar}", file=sys.stderr)
        return 1

    remote_mods = os.environ.get("LIVE_SFTP_MODS", "mods")
    recycle = os.environ.get("LIVE_SFTP_RECYCLE", "recycle_bin")
    name = args.jar.name
    remote = f"{remote_mods}/{name}"

    sftp, transport = connect_sftp()
    try:
        ensure_dir(sftp, recycle)
        ensure_dir(sftp, remote_mods)
        before = list_kaipath_jars(sftp, remote_mods)
        print(f"  remote kaipath jars before: {before or '(none)'}")
        for old in before:
            action = clear_remote_jar(sftp, f"{remote_mods}/{old}", recycle)
            print(f"  cleared {old}: {action}")
        print(f"  uploading {args.jar} -> {remote}")
        sftp.put(str(args.jar), remote)
        after = list_kaipath_jars(sftp, remote_mods)
        print(f"  remote kaipath jars after: {after}")
    finally:
        sftp.close()
        transport.close()
    print("KaiPath upload complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
