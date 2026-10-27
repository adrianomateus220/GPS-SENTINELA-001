#!/usr/bin/env bash
set -euo pipefail
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb push /tmp/sentinela-data/sudeste.map /data/local/tmp/sentinela-sudeste.map
adb push /tmp/sentinela-data/SP.json /data/local/tmp/sentinela-SP.json
adb shell pm grant br.com.sentinela.gps android.permission.ACCESS_FINE_LOCATION
adb shell pm grant br.com.sentinela.gps android.permission.ACCESS_COARSE_LOCATION
adb shell svc wifi disable
adb shell svc data disable
adb shell cmd connectivity airplane-mode enable
adb shell settings put secure location_mode 0
mkdir -p app/build/reports/device
adb shell am instrument -w br.com.sentinela.gps.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/reports/device/instrumentation.txt
if ! grep -Fq 'OK (1 test)' app/build/reports/device/instrumentation.txt; then
  adb logcat -d > app/build/reports/device/logcat.txt
  exit 1
fi
adb logcat -d > app/build/reports/device/logcat.txt

adb pull /sdcard/Android/data/br.com.sentinela.gps/files/offline-test.png app/build/reports/device/offline-test.png
