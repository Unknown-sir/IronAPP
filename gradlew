#!/bin/sh
# IronAPP gradle wrapper bootstrap (CI installs Gradle directly; see .github/workflows).
exec gradle "$@"
