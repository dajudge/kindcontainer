#! /bin/sh

set -eu

echo "Waiting for startup signal: $STARTUP_SIGNAL"
while [ ! -f "$STARTUP_SIGNAL" ]; do
  read -t 0.1 < /dev/stdout || true
done
exec "$@"