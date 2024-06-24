#!/usr/bin/env bash

TESTS=$(./gradlew determineTests | grep -e '<< TEST >>' | sed -e 's/<< TEST >>//g' | jq -s 'add' | jq -c .)
# Check if the GITHUB_OUTPUT is set
if [ -z "$GITHUB_OUTPUT" ]; then
  # We do not have github output, then use the set output command
  echo "::set-output name=tests-to-run::$TESTS"
  exit 0
fi
echo "tests-to-run=$TESTS" >> "$GITHUB_OUTPUT"
