#!/bin/sh
# لقطات Kotlin بنفس بيانات fixture.json: shoot-kt.sh <outDir>
cd "$(dirname "$0")/../../android" && rm -rf "$1" && mkdir -p "$1" && GRCS_SHOTS="$1" ./gradlew -q testDebugUnitTest --tests '*ShotTest*' 2>&1 | grep -v JAVA_TOOL | tail -8
