#!/bin/bash
set -e
set -o pipefail
readonly DIR="$(cd "$( dirname "${BASH_SOURCE[0]}")" && pwd)"

source "$DIR/env.sh"

PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -U "$POSTGRES_USER" -c "$1"
