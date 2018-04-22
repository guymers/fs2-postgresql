#!/bin/bash
set -e
set -o pipefail
readonly DIR="$(cd "$( dirname "${BASH_SOURCE[0]}")" && pwd)"

"$DIR/client.sh" "SELECT pg_notify('$1', '$2');"
