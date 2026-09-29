# Contributing to IronAPP

1. Open Android Studio (Hedgehog or newer), JDK 17, SDK 34.
2. `gradle :app:assembleDebug` must stay green on every PR.
3. New protocol support must respect `QuotaGate`: never connect while `access_ok=false`.
4. Persian + English strings are both required (`values` and `values-fa`).
5. Keep `minSdk 21` — no API above 21 without a runtime guard.
