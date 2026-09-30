# IronAPP embedded core (sing-box / libbox)

IronAPP tunnels **inside the app** with one modern core instead of
downloading separate VPN apps: [sing-box](https://github.com/SagerNet/sing-box)
(`experimental/libbox`, gomobile AAR), which natively speaks VLESS, VMess,
Trojan, Shadowsocks, WireGuard, Hysteria2, SSH, OpenVPN-client and
OpenConnect-client (ocserv/AnyConnect).

## Pinned versions (`core/versions.env`)

- sing-box **v1.14.2**, Go **1.26.8**, `-androidapi 21`, `-javapkg com.ironpanel`
- Mobile tag set copied from sing-box's own `cmd/internal/build_libbox`
  (legacy variant, minus naive outbound)
- gomobile = **SagerNet's fork** (`github.com/sagernet/gomobile`, pinned by
  sing-box's own `go.mod`) — upstream gomobile cannot link this tree
  (`invalid reference to os.checkPidfdOnce`).

## How it is built

CI (`Android CI → core`) clones the pinned tag and runs:

```bash
bash scripts/build-core.sh   # needs Go 1.26+, JDK 17, Android SDK+NDK 28
```

The resulting `libbox.aar` is uploaded as a CI artifact and consumed by
`app/libs/libbox.aar` (`implementation(files("libs/libbox.aar"))`).

Local development: run the script once and copy the AAR to `app/libs/`.
Never commit binary AARs to git.

## License

sing-box is GPL-3.0-or-later, therefore IronAPP as a whole is
GPL-3.0-or-later. See `LICENSE` and `THIRD_PARTY_NOTICES.md`.
Third-party trademark rule respected: nothing here is named sing-box.
