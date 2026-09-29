# IronAPP protocol matrix

Source of truth for mapping: panel `docs/IRONAPP.md` + `GET /s/<token>/app.json`.

| Panel id | Config key | v1.0 engine | v1.1 roadmap |
|---|---|---|---|
| xray | `xray.txt` + `xray_links[]` | handoff (v2rayNG/NekoBox/Hiddify) | embedded Xray gomobile AAR |
| wireguard | `wireguard.conf` | embedded `com.wireguard.android:tunnel` | — (done) |
| openvpn | `*.ovpn` | handoff (ics-openvpn) | embedded ics-openvpn core |
| hysteria2 | `hysteria2.txt` | handoff | embedded hysteria core |
| ocserv | `ocserv.txt` | handoff (AnyConnect) | — (no OSS embeddable core) |
| l2tp/pptp | `l2tp.txt`/`pptp.txt` | handoff (removed from Android 12+ ROMs) | — |
| ssh | `ssh.txt` | handoff | — |
| telegram_proxy | `telegram_proxy.txt` | handoff (Telegram) | — |

Rules for every engine: check `QuotaGate` before connect, poll `/status`
every 45s while connected, disconnect + show `access_reason` on deny.
