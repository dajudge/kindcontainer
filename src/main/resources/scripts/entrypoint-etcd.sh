#! /bin/sh

set -eu

echo "Waiting for container IP address: $IP_ADDRESS_PATH"
while [ ! -f "$IP_ADDRESS_PATH" ]; do
  read -t 0.1 < /dev/stdout || true
done
while read line; do
  IP_ADDRESS="$line"
done <"$IP_ADDRESS_PATH"
CONTAINER_IP="$IP_ADDRESS" exec "$@"