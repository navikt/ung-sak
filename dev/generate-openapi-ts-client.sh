#!/usr/bin/env bash

set -eu -o pipefail

# This script is a shortcut to run the maven command that generates a openapi specification json file from the project sources.

# Resolve the correct working directory for the mvn command
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
WORK_DIR="$(dirname ${SCRIPT_DIR})"

cd "${WORK_DIR}"

TS_CLIENT_SRC_DIR="$(pwd)/web/src/main/resources/openapi-ts-client"
TS_CLIENT_TARGET_DIR="$(pwd)/web/target/ts-client"

# Get all dependencies in order
mvn install -DskipTests=true

# Generate k9-sak.openapi.json in the resources/openapi-ts-client directory
mvn --projects web exec:java

# Use docker to make the typescript client from the generated k9-sak.openapi.json
mkdir -p "${TS_CLIENT_TARGET_DIR}"
CONTAINER_NAME="openapi-ts-clientmaker-cli"
IMAGE_URL="europe-north1-docker.pkg.dev/nais-management-233d/k9saksbehandling/navikt/${CONTAINER_NAME}:latest"
# If docker pull fails because of missing authorization, run `gcloud auth login` and try again.
docker pull --quiet $IMAGE_URL
docker run \
  --name="$CONTAINER_NAME" \
  --rm \
  --mount type=bind,source="${TS_CLIENT_SRC_DIR}",target=/in \
  --mount type=bind,source="${TS_CLIENT_TARGET_DIR}",target=/out \
  $IMAGE_URL -- \
  --openapi-spec-file in/k9-sak.openapi.json \
  --package-json-file in/package.json \
  --client-name K9SakClient

# Restore original working directory
cd -
