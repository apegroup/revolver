#!/usr/bin/env sh
set -e # Abort script on error
./gradlew clean testDebugUnitTest
./gradlew iosSimulatorArm64Test
