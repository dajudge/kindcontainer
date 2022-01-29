#! /bin/sh
# Copyright 2020-2022 Alex Stockinger
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

echo "Container IP: $CONTAINER_IP"
echo "Starting etcd..."
etcd \
  --advertise-client-urls=https://${CONTAINER_IP}:2379 \
  --cert-file=${SERVER_CERT_PATH} \
  --key-file=${SERVER_KEY_PATH} \
  --trusted-ca-file=${SERVER_CACERTS_PATH} \
  --peer-cert-file=${SERVER_CERT_PATH} \
  --peer-key-file=${SERVER_KEY_PATH} \
  --peer-trusted-ca-file=${SERVER_CACERTS_PATH} \
  --peer-client-cert-auth=true \
  --client-cert-auth=true \
  --data-dir=/var/lib/etcd \
  --initial-advertise-peer-urls=https://${CONTAINER_IP}:2380 \
  --initial-cluster=control-plane=https://${CONTAINER_IP}:2380 \
  --listen-client-urls=https://127.0.0.1:2379,https://${CONTAINER_IP}:2379 \
  --listen-metrics-urls=http://${CONTAINER_IP}:2381 \
  --listen-peer-urls=https://${CONTAINER_IP}:2380 \
  --name=control-plane \
  --snapshot-count=10000
