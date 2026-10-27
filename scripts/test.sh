#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -r "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/dev/vynex/thermal/ProcSnapshot.java app/src/main/java/dev/vynex/thermal/SnapshotFiles.java app/src/main/java/dev/vynex/thermal/DesktopSetting.java app/src/main/java/dev/vynex/thermal/DesktopDiagnostics.java app/src/main/java/dev/vynex/thermal/UpdatePolicy.java tests/ProcSnapshotTest.java tests/SnapshotFilesTest.java tests/ControlPolicyTest.java tests/DesktopDiagnosticsTest.java
java -cp "$test_dir" ProcSnapshotTest
java -cp "$test_dir" SnapshotFilesTest
java -cp "$test_dir" ControlPolicyTest
java -cp "$test_dir" DesktopDiagnosticsTest
