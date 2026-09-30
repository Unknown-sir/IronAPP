<div align="center">

# IronAPP — آیرون‌اپ

**کلاینت اوپن‌سورس اندروید برای [IronPanel](https://github.com/Unknown-sir/ironpanel)**

![License](https://img.shields.io/badge/license-GPL--3.0--or--later-blue)
![Min SDK](https://img.shields.io/badge/minSdk-21%20(Android%205.0+)-blue)
![Core](https://img.shields.io/badge/core-sing--box%20embedded-green)
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

## اتصال هر پروتکل — کاملاً داخل خود اپ

هیچ اپ جانبی دانلود نمی‌شود. همه‌چیز با **هسته داخلی sing-box** (بیلدشده از سورس پین‌شده در CI) از طریق VpnService خود اپ تونل می‌شود:

| پروتکل پنل | اتصال درون‌برنامه‌ای |
|---|---|
| Xray/X (VLESS/VMess/Trojan/Shadowsocks) | ✅ هسته داخلی |
| WireGuard | ✅ هسته داخلی |
| Hysteria2 | ✅ هسته داخلی |
| SSH | ✅ هسته داخلی |
| OpenVPN (گواهی‌محور + tls-crypt پنل) | ✅ هسته داخلی (endpoint openvpn-client) |
| Ocserv/AnyConnect | ✅ هسته داخلی (endpoint openconnect) |
| L2TP/PPTP | 📋 نمایش مشخصات (اندروید 12+ این APIها را حذف کرده) |
| MTProto | 📲 باز شدن مستقیم لینک `tg://` در تلگرام |

## طراحی ۲۰۲۶

- تم تیره اول با گرادیان aurora، دکمه پاور بزرگ با حلقه پالس، سرعت لحظه‌ای دانلود/آپلود، کاروسل پروتکل‌ها، کارت شیشه‌ای مصرف و شمارش معکوس انقضا
- فارسی (راست‌به‌چپ) + انگلیسی، روشن/تیره/سیستمی، ریسپانسیو برای گوشی و تبلت

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
