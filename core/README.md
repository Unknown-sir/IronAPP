# IronAPP embedded core (sing-box / libbox)

IronAPP tunnels **inside the app** with one modern core instead of
downloading separate VPN apps: [sing-box](https://github.com/SagerNet/sing-box)
(`experimental/libbox`, gomobile AAR), which natively speaks VLESS, VMess,
Trojan, Shadowsocks, WireGuard, Hysteria2, SSH, OpenVPN-client and
OpenConnect-client (ocserv/AnyConnect).

## Pinned versions (`core/versions.env`)

- sing-box **v1.14.2**, Go **1.25.5**, `-androidapi 21`, `-javapkg com.ironpanel.libbox`
- Feature tags = sing-box official `DEFAULT_BUILD_TAGS_OTHERS`
  (QUIC, Reality/uTLS, WireGuard, OpenVPN, OpenConnect, …)

## How it is built

CI (`Android CI → core`) clones the pinned tag, installs gomobile and runs:

```bash
bash scripts/build-core.sh   # needs Go 1.25+, JDK 17, Android SDK+NDK
```

The resulting `libbox.aar` is uploaded as a CI artifact and consumed by
`app/libs/libbox.aar` (`implementation(files("libs/libbox.aar"))`).

Local development: run the script once and copy the AAR to `app/libs/`.
Never commit binary AARs to git.

## License

sing-box is GPL-3.0-or-later, therefore IronAPP as a whole is
GPL-3.0-or-later. See `LICENSE` and `THIRD_PARTY_NOTICES.md`.
Third-party trademark rule respected: nothing here is named sing-box.
