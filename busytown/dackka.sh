#!/bin/bash
set -e

cd "$(dirname $0)/.."

./gradlew assemble --no-daemon
