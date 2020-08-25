#!/bin/bash
set -e

# Stages a default integration test.
# If you don't want that, pass in a path to the root directory of your generated
# docs (ending in reference/) and the base package name.

start_dir="$PWD"
path="${1:-"testData/simple/docs/reference/"}"
package_base="${2:-"dokkatest"}"

client="$(p4 g4d -f tmp-dokka-devsite)"
cd "$client"

/google/data/ro/projects/devsite/devsite2 provision
cp -r "$start_dir"/"$path" third_party/devsite/android/en/
p4 reopen
/google/data/ro/projects/devsite/devsite2 stage --db="$USER" \
  "third_party/devsite/android/en/assets" \
  "third_party/devsite/android/en/reference/$package_base" \
  "third_party/devsite/android/en/reference/kotlin/$package_base"

cd "$start_dir"
