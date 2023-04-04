#!/usr/bin/env sh
set -e
./gradlew clean podspec podPublishReleaseXCFramework
./gradlew kSwiftMultiplatformLibraryPodspec
./gradlew copyFrameworksToXcodeProject
