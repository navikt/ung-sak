#!/usr/bin/env bash

# This script is a shortcut to run the maven command that generates a openapi specification json file from the project sources.

# Resolve the correct working directory for the mvn command
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
WORK_DIR="$(dirname ${SCRIPT_DIR})"

cd "${WORK_DIR}"

# One can provide an argument to override the default output path of the generated json.
if [ -z "$1" ]
then
  # no argument provided
  mvn --projects web compile exec:java
else
  mvn --projects web compile exec:java -Dexec.args="$1"
fi

# Restore original working directory
cd -
