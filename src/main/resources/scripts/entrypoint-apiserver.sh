#! /bin/sh
# Copyright 2020-2021 Alex Stockinger
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

echo "Waiting for container IP address..."
while [ ! -f "$IP_ADDRESS_PATH" ]; do
  sleep .1
done
echo "Waiting for etcd IP hostname..."
while [ ! -f "$ETCD_HOSTNAME_PATH" ]; do
  sleep .1
done
CONTAINER_IP="$(cat $IP_ADDRESS_PATH)" ETCD_HOSTNAME="$(cat $ETCD_HOSTNAME_PATH)" exec "$@"