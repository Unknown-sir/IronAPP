<div align="center">

# IronAPP

**Open-source Android client for [IronPanel](https://github.com/Unknown-sir/ironpanel)**

![License](https://img.shields.io/badge/license-GPL--3.0--or--later-blue)
![Min SDK](https://img.shields.io/badge/minSdk-21%20(Android%205.0+)-blue)
![Core](https://img.shields.io/badge/core-sing--box%20embedded-green)

[🇮🇷 فارسی](README.md)

</div>

---

## How it works

1. The user enters one **subscription link** (`https://panel:port/s/TOKEN`) or the bare token (paste, QR scan, or opening the link from a browser).
2. The app reads from the panel's new endpoint (`GET /s/<token>/app.json` — panel **2.0.11+**): **traffic quota**, **expiry date**, **protocols enabled for that user**, that user's configs, Xray links and feed URLs.
3. The user connects to **any enabled protocol**; when **traffic runs out or the date passes**, the app refuses to connect and kills live sessions via `/status` polling. The server enforces the same gate independently (403). Contract: [docs/IRONAPP.md](https://github.com/Unknown-sir/ironpanel/blob/main/docs/IRONAPP.md).

## Single configs without a sub

The Configs tab → “My configs” accepts a pasted **VLESS link, wireguard.conf or .ovpn**; no subscription needed, same in-app tunnel, no quota.

## Install on every Android

- `minSdk 21` = Android **5.0+** (virtually every active device).
- CI builds a **universal APK** plus **per-ABI APKs** for every core: `armeabi-v7a` · `arm64-v8a` · `x86` · `x86_64`.

## Per-protocol connection — fully in-app (v1.1.0)

No side apps are downloaded. Everything tunnels through the **embedded
sing-box core** (built from pinned source in CI) in the app's own VpnService:

| Panel protocol | In-app path |
|---|---|
| Xray (VLESS/VMess/Trojan/Shadowsocks) | ✅ embedded core |
| WireGuard | ✅ embedded core |
| Hysteria2 | ✅ embedded core |
| SSH | ✅ embedded core |
| OpenVPN (panel cert + tls-crypt) | ✅ embedded core (`openvpn-client` endpoint) |
| Ocserv/AnyConnect | ✅ embedded core (`openconnect` endpoint) |
| L2TP/PPTP | 📋 credential view (removed from Android 12+) |
| MTProto | 📲 direct `tg://` link into Telegram |

## Design

2026-style dark-first aurora UI: big power button with pulse ring, live
up/down speeds, protocol carousel, glass usage card, expiry countdown.
Persian (RTL) + English, light/dark/system, responsive on phones/tablets.

## Build

```bash
gradle :app:assembleRelease
# outputs: app/build/outputs/apk/release/
```
