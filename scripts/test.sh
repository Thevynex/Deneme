#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -r "$test_dir"' EXIT
javac -d "$test_dir" app/src/main/java/dev/vynex/thermal/ProcSnapshot.java tests/ProcSnapshotTest.java
java -cp "$test_dir" ProcSnapshotTest
