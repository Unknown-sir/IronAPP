#!/usr/bin/env bash
# Build libbox.aar from pinned sing-box source (same steps as CI `core` job).
set -Eeuo pipefail
# shellcheck disable=SC1091
source "$(dirname "$0")/../core/versions.env"

WORK="${1:-$PWD/.core-build}"
echo "[core] sing-box v${SINGBOX_VERSION} / Go ${GO_VERSION} -> $WORK"
mkdir -p "$WORK"
if [[ ! -d "$WORK/sing-box-src" ]]; then
  git clone --depth 1 --branch "v${SINGBOX_VERSION}" \
    https://github.com/SagerNet/sing-box.git "$WORK/sing-box-src"
fi
export PATH="$PATH:$(go env GOPATH)/bin"
go install -v github.com/sagernet/gomobile/cmd/gomobile@v0.1.13
go install -v github.com/sagernet/gomobile/cmd/gobind@v0.1.13
gomobile init
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/28.0.13004108}"
# Bind from INSIDE the pristine sing-box module (go.mod untouched).
cd "$WORK/sing-box-src"
gomobile bind -v -o "$WORK/libbox.aar" \
  -target=android -androidapi "$ANDROID_API" \
  -javapkg "$JAVAPKG" -libname="$LIBNAME" \
  -trimpath -buildvcs=false \
  -ldflags "-X github.com/sagernet/sing-box/constant.Version=v${SINGBOX_VERSION} -X runtime.godebugDefault=multipathtcp=0,tlssha1=1 -checklinkname=0 -s -w -buildid=" \
  -tags "$SINGBOX_TAGS" \
  ./experimental/libbox
echo "[core] AAR ready: $WORK/libbox.aar (copy to app/libs/)"
