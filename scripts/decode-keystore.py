#!/usr/bin/env python3
"""Decode the signing secret without logging credential contents."""
import base64
import binascii
import os
from pathlib import Path
import sys


def decode_keystore(raw: str) -> bytes:
    cleaned = "".join(raw.split())
    try:
        data = base64.b64decode(
            cleaned + "=" * (-len(cleaned) % 4), altchars=b"-_", validate=True
        )
    except (binascii.Error, ValueError):
        raise ValueError(
            "KEYSTORE_BASE64 must contain the encoded keystore file, not a command, "
            "file path, or instructions. Recreate it from the existing release keystore."
        ) from None
    # JKS starts with FEEDFEED; PKCS12 is an ASN.1 sequence.
    if len(data) < 32 or not (data.startswith(b"\xfe\xed\xfe\xed") or data.startswith(b"\x30")):
        raise ValueError("KEYSTORE_BASE64 decoded successfully but is not a JKS or PKCS12 keystore.")
    return data


def main() -> int:
    try:
        data = decode_keystore(os.environ.get("KEYSTORE_BASE64", ""))
    except ValueError as error:
        print(f"::error::{error}", file=sys.stderr)
        return 1
    output = Path(sys.argv[1])
    output.write_bytes(data)
    output.chmod(0o600)
    print("Release keystore decoded successfully.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
