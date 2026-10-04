#!/usr/bin/env python3
"""Upload LegacyMechanics jars to live SFTP; remove previous active jars first."""
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
        key = key.strip()
        val = val.strip().strip('"').strip("'")
        os.environ.setdefault(key, val)


def require_paramiko():
    try:
        import paramiko  # noqa: F401
    except ImportError:
        print("lm-live-sftp-upload.py requires paramiko (pip install paramiko)", file=sys.stderr)
        sys.exit(1)


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
    """Move jar to recycle_bin, or delete if rename fails (duplicate in recycle)."""
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
            ts = int(time.time())
            sftp.rename(dest, f"{dest}.{ts}.bak")
        except OSError:
            pass
    try:
        sftp.rename(remote_path, dest)
        return f"archived -> {dest}"
    except OSError:
        sftp.remove(remote_path)
        return "removed (recycle rename failed)"


def list_lm_forge_jars(sftp, remote_mods: str) -> list[str]:
    try:
        names = sftp.listdir(remote_mods)
    except OSError:
        return []
    return sorted(
        n
        for n in names
        if n.startswith("LegacyMechanics-") and n.endswith(".jar") and "GUI" not in n
    )


def list_lm_gui_jars(sftp, remote_plugins: str) -> list[str]:
    try:
        names = sftp.listdir(remote_plugins)
    except OSError:
        return []
    return sorted(n for n in names if n.startswith("LegacyMechanicsGUI-") and n.endswith(".jar"))


def main() -> int:
    load_live_env()
    require_paramiko()

    parser = argparse.ArgumentParser(description="Upload LM jars to live SFTP")
    parser.add_argument("--forge", required=True, type=Path, help="Local LegacyMechanics Forge jar")
    parser.add_argument("--gui", type=Path, default=None, help="Local LegacyMechanicsGUI jar")
    parser.add_argument(
        "--stage-pending",
        action="store_true",
        help="Upload as *.jar.pending without removing active jars",
    )
    parser.add_argument(
        "--recycle-gui",
        action="store_true",
        help="Archive all LegacyMechanicsGUI jars in plugins/ before Forge upload",
    )
    args = parser.parse_args()

    if not args.forge.is_file():
        print(f"Missing forge jar: {args.forge}", file=sys.stderr)
        return 1
    if args.gui is not None and not args.gui.is_file():
        print(f"Missing GUI jar: {args.gui}", file=sys.stderr)
        return 1

    remote_mods = os.environ.get("LIVE_SFTP_MODS", "mods")
    remote_plugins = os.environ.get("LIVE_SFTP_PLUGINS", "plugins")
    recycle = os.environ.get("LIVE_SFTP_RECYCLE", "recycle_bin")

    forge_name = args.forge.name
    remote_forge = f"{remote_mods}/{forge_name}"
    if args.stage_pending:
        remote_forge = f"{remote_forge}.pending"

    sftp, transport = connect_sftp()
    try:
        ensure_dir(sftp, recycle)
        ensure_dir(sftp, remote_mods)
        if args.gui is not None:
            ensure_dir(sftp, remote_plugins)

        if not args.stage_pending:
            for name in list_lm_forge_jars(sftp, remote_mods):
                if name == forge_name:
                    continue
                action = clear_remote_jar(sftp, f"{remote_mods}/{name}", recycle)
                print(f"  cleared previous Forge jar {name}: {action}")
            # Re-uploading same version: drop old copy so put replaces cleanly.
            if forge_name in list_lm_forge_jars(sftp, remote_mods):
                action = clear_remote_jar(sftp, f"{remote_mods}/{forge_name}", recycle)
                print(f"  cleared same-version Forge jar {forge_name}: {action}")

        if args.recycle_gui:
            for name in list_lm_gui_jars(sftp, remote_plugins):
                action = clear_remote_jar(sftp, f"{remote_plugins}/{name}", recycle)
                print(f"  cleared GUI jar {name}: {action}")

        if not args.stage_pending and args.gui is not None:
            for name in list_lm_gui_jars(sftp, remote_plugins):
                if name == args.gui.name:
                    continue
                action = clear_remote_jar(sftp, f"{remote_plugins}/{name}", recycle)
                print(f"  cleared previous GUI jar {name}: {action}")

        print(f"  uploading {args.forge} -> {remote_forge}")
        sftp.put(str(args.forge), remote_forge)

        if args.gui is not None:
            gui_remote = f"{remote_plugins}/{args.gui.name}"
            if args.stage_pending:
                gui_remote = f"{gui_remote}.pending"
            print(f"  uploading {args.gui} -> {gui_remote}")
            sftp.put(str(args.gui), gui_remote)
    finally:
        sftp.close()
        transport.close()

    print("Upload complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
