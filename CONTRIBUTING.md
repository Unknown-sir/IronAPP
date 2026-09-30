# Contributing to IronAPP

IronAPP is GPL-3.0-or-later (embedded sing-box core — see `LICENSE` and
`THIRD_PARTY_NOTICES.md`).

1. Open Android Studio (Hedgehog or newer), JDK 17, SDK 34.
2. Build the core once: `bash scripts/build-core.sh`, copy the AAR to
   `app/libs/` (CI does this automatically; AARs are never committed).
3. `gradle :app:assembleDebug` must stay green on every PR.
4. New protocol support must respect `QuotaGate`: never connect while `access_ok=false`.
5. Persian + English strings are both required (`values` and `values-fa`).
6. Keep `minSdk 21` — no API above 21 without a runtime guard.
7. Never add a third-party VPN client dependency: all tunnels run inside
   the app through `vpn/box` (sing-box).

