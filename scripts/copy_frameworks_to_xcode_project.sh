#!/usr/bin/env sh
set -x #echo on
if [ -z ${IOS_PROJECT_DIR}  ] || [ ${IOS_PROJECT_DIR} == null ]
then
  echo "IOS_PROJECT_DIR is not set. See project README for instructions."
  exit 1
fi

rm -rf $IOS_PROJECT_DIR/Pods/$LIB_BASENAME/$LIB_BASENAME.xcframework
rm -rf $IOS_PROJECT_DIR/Pods/$LIB_BASENAME_SWIFT/build/cocoapods/framework/$LIB_BASENAME_SWIFT

cp -r $KMM_UMBRELLA_MODULE_DIR/build/cocoapods/publish/release/$LIB_BASENAME.xcframework $IOS_PROJECT_DIR/Pods/$LIB_BASENAME/
cp -r $KMM_UMBRELLA_MODULE_DIR/build/cocoapods/framework/$LIB_BASENAME_SWIFT $IOS_PROJECT_DIR/Pods/$LIB_BASENAME_SWIFT/build/cocoapods/framework/
