#!/bin/bash
set -e

# Stages a default integration test.
# If you don't want that, pass in a path to the root directory of your generated
# docs (ending in reference/) and the base package name.

start_dir="$PWD"
path="${1:-"testData/simple/docs/reference"}"
package_base="${2:-"dokkatest"}"

client="$(p4 g4d -f tmp-dokka-devsite)"
cd "$client"

/google/data/ro/projects/devsite/devsite2 provision
cp -r "$start_dir/$path" third_party/devsite/android/en/
cp "$start_dir/testData/book.yaml" third_party/devsite/android/en/reference/dokkatest/_book.yaml
cp "$start_dir/testData/kotlin-book.yaml" third_party/devsite/android/en/reference/kotlin/dokkatest/_book.yaml
p4 reopen

/google/data/ro/projects/devsite/devsite2 stage --db="$USER" \
  "third_party/devsite/android/en/*.*" \
  "third_party/devsite/android/en/assets" \
  $(find "$start_dir/$path" -type d \
  | sed "s!$start_dir/$path!!" \
  | sed 's/$/\/*.*/' \
  | sed 's/^/third_party\/devsite\/android\/en\/reference/')

cd "$start_dir"
