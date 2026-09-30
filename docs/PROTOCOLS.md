# IronAPP protocol matrix (v1.2.0+)

Everything below tunnels **inside IronAPP** through the embedded sing-box
core (`core/`, built from pinned source in CI). No third-party VPN client
is ever downloaded. Source of truth for mapping: panel `docs/IRONAPP.md`
plus `GET /s/<token>/app.json`.

The generated core JSON follows the sing-box 1.12–1.14 migrations:
type-based DNS servers, sniff/hijack-dns rule actions (no legacy dns
outbound), WireGuard as endpoint. Panel config text itself is never
modified — only translated 1:1.

| Panel id | Panel payload | sing-box node | Converter |
|---|---|---|---|
| xray | `xray_links[]` (vless/vmess/trojan/ss) | outbound vless/vmess/trojan/shadowsocks | `UriConfig.xrayLinkToNode` |
| wireguard | `wireguard.conf` | outbound wireguard | `UriConfig.wireGuardConfToNode` |
| hysteria2 | `hysteria2.txt` (`hy2://…`) | outbound hysteria2 | `UriConfig.hysteriaToNode` |
| ssh | `ssh.txt` (Server/Port/User/Pass) | outbound ssh | `UriConfig.sshTxtToNode` |
| openvpn | `*.ovpn` (inline ca/cert/key, opt. tls-crypt) | endpoint `openvpn-client` | `UriConfig.ovpnToNode` |
| ocserv | `ocserv.txt` (Server/User/Pass) | endpoint `openconnect` (anyconnect) | `UriConfig.ocservTxtToNode` |
| l2tp/pptp | `l2tp.txt` / `pptp.txt` | — (view-only) | credential card in UI |
| telegram_proxy | `telegram_proxy.txt` (`tg://…`) | — (Telegram-only) | `MtprotoConnector` deep link |

Rules for every tunnel: check `QuotaGate` before connect, poll `/status`
every 45s while connected, disconnect + show `access_reason` on deny.
One session at a time; switching protocols rebuilds the config.

## Single configs (no subscription)

Configs tab → My configs: a pasted **VLESS URI**, **wireguard.conf** or
**.ovpn** is validated with the same converters, stored privately on device
and connected with the same engine. No quota applies (nothing to poll).

Legacy notes:

- L2TP/PPTP client APIs were removed from Android 12+; IronAPP shows the
  credentials for manual entry instead of a dead button.
- MTProto proxies only work inside Telegram by design; the app opens the
  exact `tg://proxy` link the panel generated for that user.
- `same-as-panel` passwords are never known to the app: those protocols
  fall back to the credential view instead of failing silently.
