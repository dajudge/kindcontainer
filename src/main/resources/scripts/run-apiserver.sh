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

echo "Container IP: $CONTAINER_IP"
echo "Starting API Server..."
kube-apiserver \
  --advertise-address=${CONTAINER_IP} \
  --allow-privileged=true \
  --authorization-mode=Node,RBAC \
  --enable-admission-plugins=NodeRestriction \
  --enable-bootstrap-token-auth=true \
  --client-ca-file=${API_SERVER_CA} \
  --tls-cert-file=${API_SERVER_CERT} \
  --tls-private-key-file=${API_SERVER_KEY} \
  --kubelet-client-certificate=${API_SERVER_CERT} \
  --kubelet-client-key=${API_SERVER_KEY} \
  --proxy-client-key-file=${API_SERVER_KEY} \
  --proxy-client-cert-file=${API_SERVER_CERT} \
  --etcd-cafile=${ETCD_CLIENT_CA} \
  --etcd-certfile=${ETCD_CLIENT_CERT} \
  --etcd-keyfile=${ETCD_CLIENT_KEY} \
  --etcd-servers=https://${ETCD_HOSTNAME}:2379 \
  --service-account-key-file=${API_SERVER_PUBKEY} \
  --service-account-signing-key-file=${API_SERVER_KEY} \
  --service-account-issuer=https://kubernetes.default.svc.cluster.local \
  --kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname \
  --requestheader-allowed-names=front-proxy-client \
  --requestheader-client-ca-file=${API_SERVER_CA} \
  --requestheader-extra-headers-prefix=X-Remote-Extra- \
  --requestheader-group-headers=X-Remote-Group \
  --requestheader-username-headers=X-Remote-User \
  --runtime-config= \
  --secure-port=6443 \
  --service-cluster-ip-range=10.96.0.0/16
