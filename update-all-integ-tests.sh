#!/bin/bash -e

./gradlew :test --tests="com.google.devsite.integration.*" && exit 0 || printf "\n-----------\nUpdating..."

rm -rf testData/fragment/docs && cp -r build/docs/testData/fragment/docs testData/fragment/docs
rm -rf testData/innerClasses/docs && cp -r build/docs/testData/innerClasses/docs testData/innerClasses/docs
rm -rf testData/simple/docs && cp -r build/docs/testData/simple/docs testData/simple/docs
rm -rf testData/topLevelFunctions/docs && cp -r build/docs/testData/topLevelFunctions/docs testData/topLevelFunctions/docs
