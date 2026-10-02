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

CONFIG_SRC="$(dirname "$0")/fieldtest-config.json"
echo "=== push config to device ==="
adb push "$CONFIG_SRC" /data/local/tmp/ironapp-fieldtest.json

echo "=== verify package + service ==="
adb shell pm list packages 2>/dev/null | grep -i ironpanel || echo "(package not listed!)"
adb shell dumpsys package "$PKG" 2>/dev/null | grep -i "IronVpnService" | head -3 || echo "(service not found!)"

echo "=== sanity: launch MainActivity (must succeed) ==="
adb shell am start --user 0 -n "$PKG/.MainActivity" || true
sleep 5

echo "=== start core service (config via on-device variable, always quoted) ==="
if [ "$API" -ge 26 ]; then
  adb shell 'CFG=$(cat /data/local/tmp/ironapp-fieldtest.json); am start-foreground-service --user 0 -n '"$PKG"'/.vpn.IronVpnService -a com.ironpanel.app.vpn.START --es config_json "$CFG" --es label "fieldtest vless"' || true
else
  adb shell 'CFG=$(cat /data/local/tmp/ironapp-fieldtest.json); am startservice --user 0 -n '"$PKG"'/.vpn.IronVpnService -a com.ironpanel.app.vpn.START --es config_json "$CFG" --es label "fieldtest vless"' || true
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
