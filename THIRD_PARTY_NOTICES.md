# Third-party notices — IronAPP

IronAPP is distributed under GPL-3.0-or-later (see `LICENSE`).
It embeds the following open-source components (built from source in CI):

## sing-box (embedded VPN core)

- Source: https://github.com/SagerNet/sing-box (pinned tag, see `core/versions.env`)
- Copyright (C) 2022 by nekohasekai
- License: GPL-3.0-or-later
- Trademark rule respected: nothing in IronAPP is named sing-box and no
  association is implied.

## WireGuard protocol

- WireGuard is a registered trademark of Jason A. Donenfeld.
- IronAPP speaks the WireGuard protocol via the sing-box core; no
  WireGuard source code is included.

## AndroidX / Jetpack / Material / Kotlin / Retrofit / OkHttp / Gson / ZXing

- Apache License 2.0, via Maven Central (see `app/build.gradle.kts`).

Corresponding source for GPL components: the pinned sing-box tag plus
`scripts/build-core.sh` reproduce the exact core binary shipped in APKs.
