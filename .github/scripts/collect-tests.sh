#!/usr/bin/env bash

./gradlew determineTests | grep -e '<< TEST >>' | sed -e 's/<< TEST >>//g' > ./tasks
TESTS=$(cat tasks | jq --raw-input . | jq --compact-output --slurp .)
# Check if the GITHUB_OUTPUT is set
if [ -z "$GITHUB_OUTPUT" ]; then
  # We do not have github output, then use the set output command
  echo "::set-output name=tests-to-run::$TESTS"
  exit 0
fi
echo "tests-to-run=$TESTS" >> "$GITHUB_OUTPUT"
