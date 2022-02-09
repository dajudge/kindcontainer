#! /bin/sh

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
