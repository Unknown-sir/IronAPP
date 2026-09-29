<div align="center">

# IronAPP

**Open-source Android client for [IronPanel](https://github.com/Unknown-sir/ironpanel)**

![License](https://img.shields.io/badge/license-MIT-green)
![Min SDK](https://img.shields.io/badge/minSdk-21%20(Android%205.0+)-blue)

[🇮🇷 فارسی](README.md)

</div>

---

## How it works

1. The user enters one **subscription link** (`https://panel:port/s/TOKEN`) or the bare token (paste, QR scan, or opening the link from a browser).
2. The app reads from the panel's new endpoint (`GET /s/<token>/app.json` — panel **2.0.11+**): **traffic quota**, **expiry date**, **protocols enabled for that user**, that user's configs, Xray links and feed URLs.
3. The user connects to **any enabled protocol**; when **traffic runs out or the date passes**, the app refuses to connect and kills live sessions via `/status` polling. The server enforces the same gate independently (403). Contract: [docs/IRONAPP.md](https://github.com/Unknown-sir/ironpanel/blob/main/docs/IRONAPP.md).

## Install on every Android

- `minSdk 21` = Android **5.0+** (virtually every active device).
- CI builds a **universal APK** plus **per-ABI APKs** for every core: `armeabi-v7a` · `arm64-v8a` · `x86` · `x86_64`.

## Per-protocol connection (v1.0)

| Panel protocol | v1.0 path |
|---|---|
| WireGuard | ✅ in-app (official `com.wireguard.android:tunnel`), fallback to the official app |
| Xray (VLESS/VMess/Trojan/SS) | handoff to v2rayNG / NekoBox / Hiddify with the user's own links (embedded core roadmap: v1.1) |
| OpenVPN | handoff to ics-openvpn with the user's `.ovpn` |
| Hysteria2 | handoff with the user's URI |
| Ocserv/L2TP/PPTP/SSH/MTProto | prefilled credentials + system-client deep link |

## Design

Material3 dynamic color, light/dark/system, Persian (RTL) + English, responsive on phones/tablets/portrait/landscape, usage ring, expiry countdown, protocol tabs, copy/open config screen.

## Build

```bash
gradle :app:assembleRelease
# outputs: app/build/outputs/apk/release/
```
