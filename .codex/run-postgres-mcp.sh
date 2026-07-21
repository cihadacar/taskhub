#!/bin/sh
set -eu

env_file="${TASKHUB_ENV_FILE:-.env}"

if [ ! -f "$env_file" ]; then
  echo "PostgreSQL MCP environment file not found: $env_file" >&2
  exit 1
fi

set -a
. "$env_file"
set +a

exec npx -y mcp-postgres-server
