<div align="center">

# IronAPP — آیرون‌اپ

**کلاینت اوپن‌سورس اندروید برای [IronPanel](https://github.com/Unknown-sir/ironpanel)**

![License](https://img.shields.io/badge/license-MIT-green)
![Min SDK](https://img.shields.io/badge/minSdk-21%20(Android%205.0+)-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.0-purple)
![Compose](https://img.shields.io/badge/UI-Compose%20Material3-orange)

[🇬🇧 English](README_EN.md)

</div>

---

## روش کار

1. کاربر **لینک ساب** (`https://panel:port/s/TOKEN`) یا خود توکن را وارد می‌کند (چسباندن، اسکن QR، یا باز کردن لینک از مرورگر).
2. اپ از اندپوینت جدید پنل (`GET /s/<token>/app.json` — پنل **2.0.11+**) می‌خواند:
   - **حجم** (کل/مصرف/باقی‌مانده)، **تاریخ انقضا**، و **پروتکل‌های فعال همان کاربر**
   - کانفیگ‌های همان کاربر + لینک‌های Xray + آدرس فیدهای Clash/Sing-box/Hiddify
3. کاربر به **هر پروتکلی که برایش فعال است** وصل می‌شود؛ اگر **حجم تمام شود یا تاریخ بگذرد**، اپ اجازه اتصال نمی‌دهد و وسط اتصال هم با poll کردن `/status` قطع می‌کند. (سرور هم مستقل همین گیت را با 403 اجرا می‌کند — سند قرارداد: [docs/IRONAPP.md](https://github.com/Unknown-sir/ironpanel/blob/main/docs/IRONAPP.md))

## نصب روی همه اندرویدها

- `minSdk 21` یعنی اندروید **5.0 به بالا** (عملاً همه گوشی‌های فعال).
- خروجی CI هم **APK سراسری (universal)** می‌سازد هم **تک‌تک per-ABI** برای هسته‌های متفاوت:
  `armeabi-v7a` · `arm64-v8a` · `x86` · `x86_64`
- اگر گوشی قدیمی universal را نصب می‌کند، در غیر این صورت نسخه مخصوص معماری خودش را.

## اتصال هر پروتکل

| پروتکل پنل | اتصال در v1.0 |
|---|---|
| WireGuard | ✅ داخل خود اپ (کتابخانه رسمی `com.wireguard.android:tunnel`)، با fallback به اپ رسمی WireGuard |
| Xray/X (VLESS/VMess/Trojan/SS) | handoff به v2rayNG / NekoBox / Hiddify با همان لینک کاربر (هسته داخل‌اپی در نقشه v1.1) |
| OpenVPN | handoff به ics-openvpn با فایل `.ovpn` همان کاربر |
| Hysteria2 | handoff به Hiddify/NekoBox با URI همان کاربر |
| Ocserv/L2TP/PPTP/SSH/MTProto | کپی credential آماده + دیپ‌لینک به کلاینت سیستمی (در اندروید 12+ راه دیگری نیست) |

## طراحی

- **Material3** با رنگ داینامیک، حالت روشن/تیره/سیستمی، فارسی (راست‌به‌چپ) + انگلیسی
- ریسپانسیو برای گوشی و تبلت، عمودی و افقی (Compose adaptive)
- حلقه مصرف حجمی، شمارش معکوس انقضا، تب پروتکل‌ها، صفحه کانفیگ‌ها با کپی/بازکردن

## بیلد

```bash
# پیش‌نیاز: Android Studio (Hedgehog+) یا Gradle 8.7 + JDK 17 + SDK 34
gradle :app:assembleRelease
# خروجی: app/build/outputs/apk/release/
```

CI گیت‌هاب روی هر push به `main` همه APKها را می‌سازد و روی تگ `v*` ریلیز می‌کند.
امضای استور: متغیرهای `IRONAPP_KEYSTORE_*` را در Secrets بگذارید (در غیر این صورت CI با کلید debug امضا می‌کند).

## توسعه

- [CONTRIBUTING.md](CONTRIBUTING.md) · [SECURITY.md](SECURITY.md) · [docs/PROTOCOLS.md](docs/PROTOCOLS.md)
- قرارداد سرور↔اپ: پنل `docs/IRONAPP.md`
