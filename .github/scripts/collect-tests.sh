#!/usr/bin/env bash

./gradlew determineTests | grep -e '<< TEST >>' | sed -e 's/<< TEST >>//g' > ./tasks
TESTS=$(cat tasks | jq --raw-input . | jq --compact-output --slurp .)
echo "tests-to-run=$TESTS" >> "$GITHUB_OUTPUT"
