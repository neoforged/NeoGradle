#!/usr/bin/env bash

./gradlew determineTests | grep -e '<< TEST >>' | sed -e 's/<< TEST >> //g' > ./tasks
TESTS=$(cat tasks | jq --raw-input . | jq --compact-output --slurp .)
echo "::set-output name=tests-to-run::$TESTS"
