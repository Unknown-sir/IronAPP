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
go install golang.org/x/mobile/cmd/gomobile@latest
gomobile init
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$ANDROID_HOME/ndk/28.0.13004108}"
# Bind from INSIDE the sing-box module (same as CI).
cd "$WORK/sing-box-src"
go get golang.org/x/mobile/cmd/gomobile@latest
go get -tool golang.org/x/mobile/cmd/gobind
go mod tidy
gomobile bind -v -o "$WORK/libbox.aar" \
  -target=android -androidapi "$ANDROID_API" \
  -javapkg "$JAVAPKG" \
  -tags "$SINGBOX_TAGS" \
  ./experimental/libbox
echo "[core] AAR ready: $WORK/libbox.aar (copy to app/libs/)"
