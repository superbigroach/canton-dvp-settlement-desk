#!/usr/bin/env python
"""
preflight.py - verify a Canton participant is wired correctly BEFORE the first fixing.

    python preflight.py                    # read .env in this directory
    python preflight.py --env .env.devnet  # or a specific file
    python preflight.py --diag http://localhost:8080/api/diag

Every check here exists because getting it wrong costs hours and the error message
does not say what is actually wrong. Canton deliberately refuses to explain
authorization failures to the client ("the exact reason is logged on the participant,
but not given to the user for security reasons"), so a bad applicationId looks
identical to a bad token, a missing party, and an unuploaded package.

Exit code 0 = ready to submit. 1 = something would fail.
"""
from __future__ import annotations

import argparse
import base64
import json
import re
import socket
import sys
import urllib.error
import urllib.request
from pathlib import Path

OK, WARN, FAIL = "PASS", "WARN", "FAIL"
_results: list[tuple[str, str, str]] = []


def check(name: str, status: str, detail: str = "") -> None:
    _results.append((status, name, detail))


def read_env(path: Path) -> dict[str, str]:
    """Minimal .env reader: KEY=VALUE, ignoring comments and blanks."""
    env: dict[str, str] = {}
    if not path.exists():
        return env
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, _, v = line.partition("=")
        env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def jwt_claims(token: str) -> dict | None:
    """Decode a JWT payload WITHOUT verifying it. We only need to read claims."""
    parts = token.split(".")
    if len(parts) != 3:
        return None
    payload = parts[1] + "=" * (-len(parts[1]) % 4)
    try:
        return json.loads(base64.urlsafe_b64decode(payload))
    except Exception:
        return None


# --------------------------------------------------------------------------
# checks
# --------------------------------------------------------------------------

def check_auth(env: dict[str, str]) -> None:
    """The applicationId / JWT `sub` trap.

    On a real participant the applicationId MUST equal the JWT `sub` claim, or every
    command is rejected. The rejection does not say so.
    """
    token = env.get("LEDGER_JWT", "").strip()
    app_id = env.get("LEDGER_APPLICATION_ID", "").strip()

    if not token:
        if env.get("LEDGER_TLS", "false").lower() == "true":
            check("JWT present", FAIL, "LEDGER_TLS=true but LEDGER_JWT is empty")
        else:
            check("JWT present", WARN, "no JWT - correct for a local sandbox, wrong for a participant")
        return

    claims = jwt_claims(token)
    if claims is None:
        check("JWT decodes", FAIL, "LEDGER_JWT is not a well-formed JWT")
        return
    check("JWT decodes", OK)

    sub = str(claims.get("sub", ""))
    if not app_id:
        check("applicationId set", FAIL,
              f"LEDGER_APPLICATION_ID is empty. It must equal the JWT sub: {sub or '(no sub claim)'}")
    elif sub and app_id != sub:
        check("applicationId == JWT sub", FAIL,
              f"applicationId={app_id!r} but sub={sub!r}. Every command will be rejected")
    elif not sub:
        check("applicationId == JWT sub", WARN, "token has no `sub` claim to compare against")
    else:
        check("applicationId == JWT sub", OK, sub)

    exp = claims.get("exp")
    if isinstance(exp, (int, float)):
        import datetime as _dt
        when = _dt.datetime.fromtimestamp(exp, _dt.timezone.utc)
        now = _dt.datetime.now(_dt.timezone.utc)
        if when <= now:
            check("JWT not expired", FAIL, f"expired {when.isoformat()}")
        else:
            hrs = (when - now).total_seconds() / 3600
            status = OK if hrs > 1 else WARN
            check("JWT not expired", status, f"{hrs:.1f}h remaining ({when.isoformat()})")

    aud = claims.get("aud")
    if aud:
        check("JWT audience", OK, str(aud))


def check_parties(env: dict[str, str]) -> None:
    """Party ids must be full `hint::fingerprint`. A bare hint matches nothing."""
    roster = env.get("LEDGER_PARTIES", "").strip()
    if not roster:
        if env.get("LEDGER_TLS", "false").lower() == "true":
            check("party roster", WARN,
                  "LEDGER_PARTIES empty. The 3.x bindings drop party-management, so a shared "
                  "node needs the roster supplied here")
        else:
            check("party roster", OK, "empty - correct for a local sandbox")
        return

    entries = [e for e in roster.split(",") if e.strip()]
    bad: list[str] = []
    for entry in entries:
        label, _, pid = entry.partition("=")
        if not pid:
            bad.append(f"{entry} (no '=')")
        elif "::" not in pid:
            bad.append(f"{label}: {pid} has no ::fingerprint")
        elif not re.search(r"::[0-9a-f]{4,}", pid):
            bad.append(f"{label}: {pid} fingerprint looks wrong")

    if bad:
        check("party roster", FAIL, "; ".join(bad))
    else:
        check("party roster", OK, f"{len(entries)} parties, all with fingerprints")

    # REQUIRED parties. resolveParty() THROWS "no known party matches" when one is
    # absent, and the failure surfaces deep inside a request rather than at startup.
    # Venue has 26 call sites (every auction and settlement path); Auditor has 10.
    # Test:initialize allocates them on a sandbox, so this only bites on a shared node
    # - which is exactly when it is most expensive to discover.
    labels = {e.split("=", 1)[0].strip().lower() for e in entries if "=" in e}
    missing = [p for p in ("Issuer", "Venue", "Bank", "Auditor") if p.lower() not in labels]
    if missing:
        check("required parties", FAIL,
              "missing " + ", ".join(missing) + " - resolveParty throws when these are absent")
    else:
        check("required parties", OK, "Issuer, Venue, Bank, Auditor present")


def check_registry(env: dict[str, str]) -> None:
    """CIP-56 registry wiring for foreign assets (cBTC, cETH)."""
    remotes = env.get("REGISTRY_REMOTE_URLS", "").strip()
    single = env.get("REGISTRY_REMOTE_URL", "").strip()
    if not remotes and not single:
        check("CIP-56 registry", WARN,
              "no registry configured. Required to claim a foreign asset (cBTC/cETH); "
              "not required for self-issued demo assets")
        return
    entries = [e for e in remotes.split(",") if e.strip()]
    bad = [e for e in entries if "=" not in e or not e.split("=", 1)[1].startswith("http")]
    if bad:
        check("CIP-56 registry", FAIL, f"malformed: {'; '.join(bad)}")
    else:
        names = [e.split("=", 1)[0] for e in entries] or ["(default)"]
        check("CIP-56 registry", OK, ", ".join(names))


def check_reachable(env: dict[str, str]) -> None:
    """Can we open a TCP connection to the ledger host at all?"""
    host = env.get("LEDGER_HOST", "localhost")
    try:
        port = int(env.get("LEDGER_PORT", "6865"))
    except ValueError:
        check("ledger port", FAIL, f"LEDGER_PORT={env.get('LEDGER_PORT')!r} is not a number")
        return
    try:
        with socket.create_connection((host, port), timeout=6):
            check("ledger reachable", OK, f"{host}:{port}")
    except OSError as e:
        check("ledger reachable", FAIL, f"{host}:{port} - {e.__class__.__name__}: {e}")


def check_diag(url: str) -> None:
    """If the backend is up, its own diagnostic is the authoritative answer."""
    try:
        with urllib.request.urlopen(url, timeout=8) as r:
            data = json.loads(r.read().decode())
    except (urllib.error.URLError, OSError, json.JSONDecodeError) as e:
        check("backend /api/diag", WARN, f"not reachable ({e.__class__.__name__}) - start the backend to use it")
        return

    status = data.get("status", "?")
    ledger = data.get("ledger", {}) or {}
    if status == "OK" or ledger.get("reachable") is True:
        check("backend /api/diag", OK, f"status={status} ledgerEnd={ledger.get('ledgerEnd')}")
    else:
        check("backend /api/diag", FAIL,
              f"status={status} reachable={ledger.get('reachable')} error={ledger.get('error')}")


def check_dar() -> None:
    """The DAR that must be uploaded to the participant."""
    dar = Path(".daml/dist/crossdesk-2.1.0.dar")
    if dar.exists():
        check("DAR built", OK, f"{dar} ({dar.stat().st_size // 1024} KB)")
    else:
        check("DAR built", WARN, f"{dar} not found - run `daml build` before uploading")


# --------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--env", default=".env", help="env file to read (default: .env)")
    ap.add_argument("--diag", default="http://localhost:8080/api/diag",
                    help="backend diagnostic endpoint")
    ap.add_argument("--skip-net", action="store_true", help="config checks only, no network")
    args = ap.parse_args()

    env_path = Path(args.env)
    env = read_env(env_path)
    if not env:
        print(f"No readable env at {env_path}. Copy .env.example to .env and fill it in.\n")

    print(f"CrossDesk preflight  -  env: {env_path}  host: {env.get('LEDGER_HOST','?')}\n")

    check_auth(env)
    check_parties(env)
    check_registry(env)
    check_dar()
    if not args.skip_net:
        check_reachable(env)
        check_diag(args.diag)

    width = max(len(n) for _, n, _ in _results) + 2
    for status, name, detail in _results:
        mark = {OK: "  ok  ", WARN: " warn ", FAIL: " FAIL "}[status]
        print(f"[{mark}] {name.ljust(width)} {detail}")

    fails = sum(1 for s, _, _ in _results if s == FAIL)
    warns = sum(1 for s, _, _ in _results if s == WARN)
    print()
    if fails:
        print(f"{fails} check(s) would prevent a submission. Fix these before the first fixing.")
        return 1
    if warns:
        print(f"Ready, with {warns} warning(s) - check they are expected for this profile.")
        return 0
    print("Ready to submit.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
