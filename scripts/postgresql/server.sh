#!/bin/bash
set -e
set -o pipefail
readonly DIR="$(cd "$( dirname "${BASH_SOURCE[0]}")" && pwd)"

source "$DIR/env.sh"

docker run --rm -p "5432:$POSTGRES_PORT" --env "POSTGRES_PASSWORD=$POSTGRES_PASSWORD" "postgres:$POSTGRES_VERSION"
