#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$#" -eq 0 ]; then
  echo "Usage: ./validate.sh <file-or-directory> [...]"
  echo "Example: ./validate.sh levels/"
  exit 1
fi

node "$SCRIPT_DIR/validate-levels.js" "$@"
