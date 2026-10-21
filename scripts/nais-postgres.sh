#!/bin/bash
# Connects to the Cloud SQL database of a Nais app using `nais cloudsql`.
# See https://cli.nais.io/nais_cloudsql.html
# Exit if any command fails.
set -e

usage() {
  cat <<USAGE
Usage: $0 --app <app> --environment <dev-gcp|prod-gcp> --team <team> --reason <reason> [--all-privileges] [--port <port>]

  --app             Name of the Nais app that owns the database (e.g. ung-sak)
  --environment     dev-gcp or prod-gcp
  --team            Nais team that owns the app (e.g. k9saksbehandling)
  --reason          Justification for accessing the database (audit logged)
  --all-privileges  Grant all privileges on the schema in 'prepare' (default: not set)
  --port            Local port for the proxy (default: 5432)
USAGE
}

require_value() {
  if [ -z "$2" ] || [[ "$2" == --* ]]; then
    echo "Missing value for $1"
    usage
    exit 1
  fi
}

# --- Parse Input Arguments ---
APP=""
ENVIRONMENT=""
TEAM=""
REASON=""
ALL_PRIVILEGES=false
PORT=5432

while [[ "$#" -gt 0 ]]; do
  case $1 in
    --app)
      require_value "$1" "$2"
      APP="$2"
      shift 2
      ;;
    --environment)
      require_value "$1" "$2"
      ENVIRONMENT="$2"
      shift 2
      ;;
    --team)
      require_value "$1" "$2"
      TEAM="$2"
      shift 2
      ;;
    --reason)
      require_value "$1" "$2"
      REASON="$2"
      shift 2
      ;;
    --all-privileges)
      ALL_PRIVILEGES=true
      shift
      ;;
    --port)
      require_value "$1" "$2"
      PORT="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown parameter passed: $1"
      usage
      exit 1
      ;;
  esac
done

if [ -z "$APP" ] || [ -z "$ENVIRONMENT" ] || [ -z "$TEAM" ] || [ -z "$REASON" ]; then
  echo "Missing required parameter."
  usage
  exit 1
fi

if [[ "$ENVIRONMENT" != "dev-gcp" && "$ENVIRONMENT" != "prod-gcp" ]]; then
  echo "Invalid environment: $ENVIRONMENT (must be dev-gcp or prod-gcp)"
  exit 1
fi

# Validate before any nais command runs, so invalid input does not grant access before the proxy fails.
# 10# forces base 10, so leading zeros are not parsed as octal.
if ! [[ "$PORT" =~ ^[0-9]{1,5}$ ]] || (( 10#$PORT < 1 || 10#$PORT > 65535 )); then
  echo "Invalid port: $PORT (must be between 1 and 65535)"
  exit 1
fi
# Normalize, since nais parses leading zeros as octal.
PORT=$((10#$PORT))

COMMON_ARGS=(--environment "$ENVIRONMENT" --team "$TEAM" --reason "$REASON")

PREPARE_ARGS=("${COMMON_ARGS[@]}")
if [ "$ALL_PRIVILEGES" = true ]; then
  PREPARE_ARGS+=(--all-privileges)
fi

# --- Step 1: nais login (skipped if already logged in) ---
# `nais login` runs `gcloud auth login --update-adc`. Check both the gcloud user credentials
# (used by nais/kubectl) and Application Default Credentials (used by the Cloud SQL proxy).
# Tokens are discarded; only the exit codes are used.
if nais auth print-access-token >/dev/null 2>&1 && gcloud auth application-default print-access-token >/dev/null 2>&1; then
  echo "Already logged in, skipping nais login."
else
  echo "Starting nais login. This command will open a browser for authentication..."
  nais login
  # The command waits until login is complete.
fi

# --- Step 2: nais cloudsql prepare ---
echo -e "\nRunning 'nais cloudsql prepare' for:"
echo "  Environment:    $ENVIRONMENT"
echo "  Team:           $TEAM"
echo "  App:            $APP"
echo "  All privileges: $ALL_PRIVILEGES"
echo -e "\nPlease follow the instructions provided by the command below."
# Run prepare; it will prompt for confirmation (y/N) only once.
nais cloudsql prepare "${PREPARE_ARGS[@]}" "$APP"

# --- Step 3: nais cloudsql grant ---
echo -e "\nGranting database access with 'nais cloudsql grant'..."
nais cloudsql grant "${COMMON_ARGS[@]}" "$APP"

# --- Step 4: nais cloudsql proxy ---
echo -e "\nStarting proxy on port $PORT with 'nais cloudsql proxy'..."
nais cloudsql proxy "${COMMON_ARGS[@]}" --port "$PORT" "$APP"

echo -e "\nAll commands executed successfully."
