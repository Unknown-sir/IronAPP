#!/usr/bin/env bash
# On-emulator field test: start the real VpnService with a realistic config,
# then collect stages + Go log + logcat. Fails the job if the process dies.
set -u
APK="$1"
API="${2:-29}"
PKG="com.ironpanel.app.debug"

echo "=== install $APK ==="
adb install -r "$APK"

echo "=== root for system-level start ==="
adb root
adb wait-for-device

echo "=== grant VPN consent (best effort) ==="
adb shell appops set "$PKG" ACTIVATE_VPN allow || true

CONFIG="$(tr -d '\n' < "$(dirname "$0")/fieldtest-config.json")"
echo "=== start core service ==="
if [ "$API" -ge 26 ]; then
  adb shell am start-foreground-service -n "$PKG/.vpn.IronVpnService" \
    -a com.ironpanel.app.vpn.START \
    --es config_json "$CONFIG" \
    --es label "fieldtest vless" || true
else
  adb shell am startservice -n "$PKG/.vpn.IronVpnService" \
    -a com.ironpanel.app.vpn.START \
    --es config_json "$CONFIG" \
    --es label "fieldtest vless" || true
fi

echo "=== wait 25s ==="
sleep 25

echo "=== process alive? ==="
if adb shell pidof "$PKG" > /dev/null 2>&1; then
  echo "ALIVE: $(adb shell pidof "$PKG")"
  ALIVE=1
else
  echo "DEAD"
  ALIVE=0
fi

echo "=== core stages ==="
adb shell "run-as $PKG cat files/boxboot/stages.txt" 2>&1 || echo "(no stages file)"

echo "=== saved input config (first 500 chars) ==="
adb shell "run-as $PKG cat files/boxboot/last-config.json" 2>&1 | head -c 500 || echo "(no config file)"
echo

echo "=== go stderr tail ==="
adb shell "run-as $PKG tail -c 6000 files/box/CrashReport-ironapp.log" 2>&1 || echo "(no go log)"

echo "=== logcat (filtered) ==="
adb logcat -d -b all 2>/dev/null | grep -aiE "ironapp|libbox|boxcore|sing-box|panic|fatal|AndroidRuntime|am_kill|lowmemory|ForceClose" | tail -n 120 || true

echo "=== full logcat to file ==="
adb logcat -d -b all > "logcat-api$API.txt" 2>/dev/null || true

if [ "$ALIVE" = "0" ]; then
  echo "FIELDTEST RESULT: process died (see stages/log above)"
  exit 1
fi
echo "FIELDTEST RESULT: process survived core start"
