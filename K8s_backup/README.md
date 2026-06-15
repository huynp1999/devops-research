# Công nghệ Backup và Restore cho Kubernetes

## 1. Tổng quan về K8s Backup

Kubernetes backup sẽ cần cover các tiêu chí sau:
- **Cluster state**: etcd data, resource definitions (Deployments, Services, ConfigMaps, Secrets...)
- **Persistent data**: PersistentVolumes (PV/PVC)
- **Application-consistent backup**: Đảm bảo data consistency khi backup (pre/post hooks)

## 2. Top 3 Open Source Tool

Ba công cụ backup K8s open-source tốt nhất hiện tại:

### Tool 1: Velero (VMware/Heptio)
### Tool 2: Kasten K10 (Veeam) - Free Edition
### Tool 3: Stash/KubeStash (AppsCode)

## 3. Tổng quan

| Tiêu chí | Velero | Kasten K10 (Free) | Stash (KubeStash) |
|-----------|--------|-------------------|-------------------|
| License | Apache 2.0 | Freemium (free ≤5 nodes) | Community + Enterprise |
| Backup scope | Namespace, cluster resource, PVs | Full application, namespace | Workload-level, database-aware |
| Storage backend | Các storage backend phổ biến hiện nay, bao gồm cả public cloud, enterprise và open-source | Các storage backend phổ biến hiện nay, bao gồm cả public cloud, enterprise và open-source | Các storage backend phổ biến hiện nay, bao gồm cả public cloud, enterprise và open-source |
| Hỗ trợ snapshot | CSI snapshots, Restic, Kopia | CSI snapshots, Kanister | Restic, VolumeSnapshot |
| Lên lịch backup | Dựa trên Cronjob | Chi tiết theo policy, SLA-driven | Dựa trên Cronjob |
| Mã hóa | Restic/Kopia built-in encryption | AES-256 encryption | Restic encryption |
| DR/Migration | Cross-cluster restore, cluster migration | Cross-cluster, cross-cloud | Cross-cluster restore |
| Backup Database | Phải tự cấu hình pre/post backup hook | Được dựng sẵn các kịch bản trong Blueprint của Kanister | Built-in database backup (MySQL, PostgreSQL, MongoDB, Elasticsearch) |
| Helm install | Có | Có | Có |
| Cộng đồng | Rất lớn  |  Lớn |  Trung bình  |
| Tài liệu | Tốt | Tốt | Khá, đang cải thiện |
| Độ phức tạp | Trung bình | Thấp (dùng UI) | Trung bình-Cao |
| Khả năng restore | Theo cả namespace, hoặc theo loại resource (ít tùy chọn restore) | Từng object hoặc file (chi tiết nhất) | Restore theo từng workload (Deployment, StatefulSet, Database). Chi tiết hơn namespace nhưng không chi tiết bằng object/file-level |
| Multi-cluster | Mỗi cluster riêng | Quản lý tập trung | Mỗi cluster riêng |

## 4. Phân tích chi tiết từng tool

### Khả năng Disaster Recovery

| Scenario | Velero | Kasten K10 | Stash |
|---|---|---|---|
| Migrate cluster on-prem → cloud (EKS) | ✅ Native support, 1 câu lệnh | ⚠️ Cần config phức tạp | ❌ Không hỗ trợ tốt |
| K8s version upgrade: backup cluster cũ → restore cluster mới | ✅ Best practice | ⚠️ Cần config phức tạp | ❌ Không hỗ trợ |
| Full cluster DR sang region khác | ✅ Native support | ✅  Có support multi-cluster để failover tập trung | ❌ Chỉ DR workload |
| Migrate workload giữa các distro, ví dụ Rancher → OpenShift | ✅ Cross-distro, miễn là cùng API K8s | ⚠️ Limited, cần Kasten chạy trên cả 2 distro | ❌ Không support migrate cross-distro |
| Clone production sang staging | ✅ Restore namespace dễ dàng | ✅ Có UI để clone, dễ thao tác | ⚠️ Có thể restore data nhưng phải tự setup workload trước|

### Khả năng backup DB

Xét riêng về backup database, Stash (KubeStash) là tool support đầy đủ và chuyên sâu nhất, nhưng Kasten K10 lại có support rộng hơn về số lượng DB engine nhờ tính năng blueprint. Velero là yếu nhất trong khía cạnh này.

| Tính năng Backup DB     | Velero   | Kasten K10       | Stash       |   |
|--------------------------------------------------|-----------------------------------|--------------------------------------------------------------------|----------------------------------------------------------------------------------------|---|
| Phương pháp backup mặc định   | Volume snapshot (block-level)     | Volume snapshot + Kanister blueprint      | Logical dump (mysqldump, pg_dump)    |   |
| Khả năng backup | ⚠️ Phải tự config pre/post hooks   | ✅ Blueprint built-in  | ✅ Native addon   |   |
| Database engine support | Bất kỳ (nhưng phải tự viết hooks) | MySQL, PostgreSQL, MongoDB, Cassandra, Kafka, Elasticsearch, Redis | MySQL, MariaDB, PostgreSQL, MongoDB, Elasticsearch, Redis, Percona XtraDB, etcd, Vault |   |
| Point-in-Time Recovery (PITR)| ❌    | ⚠️ Support một vài DB dưới dạng extension, còn lại chỉ restore vào thời điểm snapshot    | ✅ Tính năng native, MySQL backup binlog liên tục → restore chính xác tới từng giây/phút bất kỳ, PostgreSQL archive WAL + base backup → recover tới một timestamp cụ thể  |   |
| Restore lệch version DB   | ❌ Môi trường restore phải cùng version với khi backup/snapshot    | ✅ Blueprint có logical dump nên portable | ✅ Native logical dump nên portable |   |
| Restore theo table/database | ❌ Restore cả volume  | ⚠️ Tùy blueprint có hỗ trợ hay không | ✅ Chi tiết tới từng table  |   |
| Restore theo schema     | ❌ Không hỗ trợ  | ⚠️ Tùy blueprint có hỗ trợ hay không  | ✅ Native support |   |
| Backup HA cluster (replica-set, primary-replica) | ❌ Phải lock manual   | ✅ Một số blueprint hiểu topology để có thể backup từ secondary | ✅ Tự động phát hiện primary/replica, backup từ replica, restore với đúng topology    |   |
| Tự phát hiện DB workload | ❌ | ✅  | ⚠️ Phải khai báo    |   |
| Tích hợp DB operator    | ❌ | ⚠️  | ✅ Tích hợp native |   |


### Mô hình kiến trúc

| Chi tiết | Velero | Kasten K10 | Stash |
|---|---|---|---|
| Các cầu phần trong cluster | ✅ 1 Deployment (velero) + 1 DaemonSet (node-agent) ở mức đơn giản backup dạng file dùng Kopia | ❌ Khoảng 15 microservices (catalog, dashboard, executor, gateway, kanister-svc...) | ⚠️ Operator + CRDs + sidecar injected vào pod app hoặc Job pod tạm thời |
| Resource overhead | ✅ Khoảng 100-200 MB RAM, tốn rất ít CPU | ❌ Khuyến nghị hoảng 4-8 GB RAM, 2-4 CPU dedicated | ⚠️ Khoảng 500 MB RAM cho operator + extra cho mỗi pod được backup |

### Xét về tính chất open-source thực sự

| Chi tiết | Velero | Kasten K10 | Stash |
|---|---|---|---|
| License | ✅ Apache 2.0, miễn phí 100% | ❌ Free ≤5 nodes, sau đó tính phí | ⚠️ Apache 2.0 cho core, Enterprise tier có phí |
| Vendor | ✅ Thuộc CNCF, không bị bó buộc vào một vendor | ❌ Thuộc Veeam | ⚠️ AppsCode (phần nhiều là free, một phần trả phí) |
| Backup format | ✅ Mở (TAR + JSON metadata) | ❌ Độc quyền của Kasten | ✅ Restic format (open standard) |
| Extract backup thủ công | ✅ Đọc bằng tar thường + JSON parser | ❌ Bắt buộc qua Kasten, không extract được | ✅ Restic CLI standard, ai cũng dùng được |

Dựa vào kết quả trên có thể liên hệ tới một trường hợp khi sử dụng backup Kasten 5 năm trước, nếu Kasten thay format hoặc Veeam dừng không hẳn product, thì có thể dẫn tới mất data. Backup Velero của 5 năm trước vẫn extract được bằng `tar -xvf` + xem metadata trong JSON

## 5. Kết luận

### **Velero** không phải giải pháp backup tốt nhất cho DB, nhưng là cung cụ tốt nhất cho K8s backup nói chung

1. Community lớn nhất → support tốt, nhiều tài liệu, ít rủi ro project bị ngừng phát triển
2. Lightweight, dễ triển khai và vận hành
3. Đủ tính năng cho hầu hết các trường hợp (backup, restore, migration, DR)
4. Tích hợp tốt với các cloud provider và các giải pháp on-premise enterprise tới open-source

Đối với 2 công cụ còn lại chọn:
- Kasten: Khi cần UI trực quan, có đội non-tech quản lý backup
- Stash: Khi cần backup Database một cách chỉnh chu nhất

## 6. Thử nghiệm tính năng với Velero

### Lab Environment

```
- Kubernetes: v1.35.5
- Cluster: 1 node K3S
- Storage: MinIO (S3-compatible) làm backup storage
```

### Setup Velero

Setup Velero và MinIO:

```bash
wget https://github.com/vmware-tanzu/velero/releases/download/v1.13.0/velero-v1.13.0-linux-amd64.tar.gz
tar -xvf velero-v1.13.0-linux-amd64.tar.gz
sudo mv velero-v1.13.0-linux-amd64/velero /usr/local/bin/

kubectl create namespace velero

kubectl apply -f manifests/minio.yaml

# Tạo bucket "velero" trong MinIO
kubectl exec -n velero deploy/minio -- mkdir -p /data/velero

# Tạo credentials file để Velero auth với MinIO
cat <<EOF > credentials-velero
[default]
aws_access_key_id = minio
aws_secret_access_key = minio123
EOF

# Cài đặt Velero với AWS plugin để tương thích với MinIO, s3Url sử dụng node IP do svc minio là NodePort
velero install \
  --provider aws \
  --plugins velero/velero-plugin-for-aws:v1.10.1 \
  --bucket velero \
  --secret-file /tmp/credentials-velero \
  --use-node-agent \
  --default-volumes-to-fs-backup \
  --backup-location-config region=minio,s3ForcePathStyle="true",s3Url=http://${NODE_IP}:30900 \ 
  --snapshot-location-config region=minio

# Verify cài đặt
kubectl get pods -n velero
NAME                      READY   STATUS    RESTARTS      AGE
minio-f768dfc77-kcjz9     1/1     Running   1 (96m ago)   4h42m
node-agent-4qlhv          1/1     Running   1 (97m ago)   4h17m
velero-768855485b-7g4q2   1/1     Running   1 (97m ago)   4h17m
```


## Test 1: Full namespace backup & restore

### Setup tài nguyên test

Setup tài nguyên test sẽ bao gồm:
- 1 file index.html của Nginx
- 1 file to ~250MB
- 1 database deployment kèm theo 2 hook mô phỏng hành động dump DB trước khi thực hiện backup

```bash
kubectl create namespace test-backup

kubectl apply -f manifests/test-app.yaml

# Ghi data test vào PVC
kubectl exec -n test-app deploy/nginx-app -- bash -c 'echo "Data - Created at $(date)" > /usr/share/nginx/html/index.html'
kubectl exec -n test-app deploy/nginx-app -- sh -c 'for i in $(seq 1 10000); do echo "Line $i test 123456789" >> /usr/share/nginx/html/bigfile.txt; done'
kubectl exec -n test-app deploy/postgresql -- bash -c '
  PGPASSWORD=mysecret psql -U postgres -d appdb -c "
    CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(100), created_at TIMESTAMP DEFAULT NOW());
    INSERT INTO users (name, email) VALUES 
      ('"'"'Nguyen Van A'"'"', '"'"'a@example.com'"'"'),
      ('"'"'Tran Thi B'"'"', '"'"'b@example.com'"'"'),
      ('"'"'Le Van C'"'"', '"'"'c@example.com'"'"');
  "
'

# Confirm dữ liệu trước test
curl 10.43.70.182
<h1>Important Data - Created at Mon Jun  8 04:51:00 UTC 2026</h1>

kubectl exec -n test-app deploy/postgresql -- bash -c 'PGPASSWORD=mysecret psql -U postgres -d appdb -c "SELECT * FROM users;"'
 id |     name     |     email     |         created_at
----+--------------+---------------+----------------------------
  1 | Nguyen Van A | a@example.com | 2026-06-08 04:52:23.685672
  2 | Tran Thi B   | b@example.com | 2026-06-08 04:52:23.685672
  3 | Le Van C     | c@example.com | 2026-06-08 04:52:23.685672
(3 rows)
```

### Thực hiện backup và restore full namespace

```bash
velero backup create full-backup \
  --include-namespaces test-app \
  --wait
```

```bash
# Kiểm tra backup trên minio đã có object được lưu
mc ls backup/velero/kopia/test-app
[2026-06-09 02:41:20 UTC]   784B STANDARD _log_20260609024120_c343_1780972880_1780972880_1_e2ac9857b79e4f51ee20c728e746c00a
[2026-06-09 02:41:24 UTC] 1.4KiB STANDARD _log_20260609024123_de7f_1780972883_1780972884_1_c16865e0a5de31795a83e9282242da6a
[2026-06-09 02:41:18 UTC]    30B STANDARD kopia.blobcfg
[2026-06-09 02:41:18 UTC] 1.0KiB STANDARD kopia.repository
[2026-06-09 02:41:34 UTC]  20MiB STANDARD p742bbd879370b4d2e4f55f22e95c56d8-s865e5c537f696894141
[2026-06-09 02:41:37 UTC] 4.2KiB STANDARD pa873066febea4b575525b15e42a49955-sa44fe99f0526a540141
[2026-06-09 02:41:23 UTC]   143B STANDARD xn0_1564886883fd1829c4658f1a3070b79d-s0d1df7315c61a868141-c1
[2026-06-09 02:41:30 UTC]   143B STANDARD xn0_2038ea017ec9ec4db56eb19b26a86a71-s63cbc8313127d8dd141-c1
...


```bash
# Verify backup thành công
velero backup describe full-backup --details
# Một số các field chính
Name:         full-backup
Namespace:    velero
Labels:       velero.io/storage-location=default
Annotations:  velero.io/resource-timeout=10m0s
              velero.io/source-cluster-k8s-gitversion=v1.35.5+k3s1
              velero.io/source-cluster-k8s-major-version=1
              velero.io/source-cluster-k8s-minor-version=35

Phase:  Completed                       # Trạng thái backup toàn bộ thành công

Storage Location:  default              # Bucket velero trong MinIO

TTL:  720h0m0s # Thời gian giữ backup trước khi tự xóa

Hooks:  <none> # Không có hook khi chỉ định backup, chỉ có trong pod annotation

Total items to be backed up:  36        # Tổng số object cần backup
Items backed up:              36        # Tổng số object đã backup

Resource List:
  apps/v1/Deployment:
    - test-app/nginx-app
    - test-app/postgresql
  apps/v1/ReplicaSet:
    - test-app/nginx-app-5d8c9f9f9b
    - test-app/postgresql-959f9b485
  discovery.k8s.io/v1/EndpointSlice:
    - test-app/nginx-svc-w6pm6
    - test-app/postgresql-svc-wj6pt
  v1/ConfigMap:
    - test-app/app-config
    - test-app/kube-root-ca.crt
    # ... các resource trong namespace, bao gồm cả PV và PVC

Backup Volumes:
  Velero-Native Snapshots: <none included>  # Không snapshot PV (sử dụng khi storage class base trên cloud storage)

  CSI Snapshots: <none included>            # Không sử dụng snapshot từ CSI (hữu dụng khi backend storage có tính năng snapshot như Ceph RBD,...)

  Pod Volume Backups - kopia:               # Kopia là cơ chế đọc file trong PVC rồi upload lên backup target
    Completed:
      test-app/nginx-app-5d8c9f9f9b-4fztt: html-storage     # ở đây có thể hiểu pod nginx-app đã được backup dữ liệu PVC html-storage
      test-app/postgresql-959f9b485-8j94s: backup, pgdata   # pod postgresql đã được backup dữ liệu PVC backup, pgdata

HooksAttempted:  2 # Postgre deployment có 2 pre-hook và post-hook mục đích mô phỏng hành động dump DB trước khi backup
HooksFailed:     0
```

```bash
velero backup get
NAME          STATUS      ERRORS   WARNINGS   CREATEDEXPIRES   STORAGE LOCATION   SELECTOR
full-backup   Completed   0        0          2026-06-09 02:41:11 +0000 UTC   29d       default            <none>
```

```bash
# Mô phỏng lại disaster mất 1 namespace
kubectl delete namespace test-app
kubectl get all -n test-app # kết quả trống
```

Thực hiện restore:

```bash

velero restore create full-restore \
  --from-backup full-backup \
  --wait
```

Verify dữ liệu có toàn vẹn:

```bash
curl 10.43.237.81
<h1>Important Data - Created at Mon Jun  8 04:51:00 UTC 2026</h1>

kubectl exec -n test-app deploy/nginx-app -- ls -l /usr/share/nginx/html/bigfile.txt
-rw-r--r--    1 root     root      256.7K Jun  8 10:02 /usr/share/nginx/html/bigfile.txt

kubectl exec -n test-app deploy/postgresql -- sh -c 'PGPASSWORD=mysecret psql -U postgres -d appdb -c "SELECT * FROM users;"'
 id |     name     |     email     |         created_at
----+--------------+---------------+----------------------------
  1 | Nguyen Van A | a@example.com | 2026-06-08 04:52:23.685672
  2 | Tran Thi B   | b@example.com | 2026-06-08 04:52:23.685672
  3 | Le Van C     | c@example.com | 2026-06-08 04:52:23.685672
(3 rows)
```

Kiểm tra hook dump DB trước snapshot có hoạt động 
```bash
kubectl exec -n test-app deploy/postgresql -- ls -lah /backup
total 20K
drwxrwxrwx    3 root     root        4.0K Jun  9 04:02 .
drwxr-xr-x    1 root     root        4.0K Jun  9 04:02 ..
drwxr-xr-x    2 root     root        4.0K Jun  9 04:02 .velero
-rw-r--r--    1 root     root        2.2K Jun  9 02:41 dump.sql     # Dump thành công
-rw-r--r--    1 root     root          93 Jun  9 02:41 hook.log
```


## Test 2: Backup/restore statefulset app

Stateful app có yêu cầu đồng bộ cao giữ nguyên identity, tên pod, PVC mount,... Vì vậy khi restore cũng cần giữ nguyên các identity như cũ.

Verify khi apply xong statefulset:

```bash
for i in {0..2}; do echo -n "pod web-$i: ";  kubectl exec -n test-app web-$i -- cat /data/identity.txt; done
pod web-0: pod=web-0 created=2026-06-10T02:47:00Z uid=438d1376-216c-406e-b14f-eaa6c6809a01
pod web-1: pod=web-1 created=2026-06-10T02:47:05Z uid=cb5a30e9-cd6c-4d62-96e8-52f9ac40fbc2
pod web-2: pod=web-2 created=2026-06-10T02:47:11Z uid=d6203eae-1092-4c15-961a-dee7b09dcdea
```

Thực hiện backup theo label `app=web-sts`:

```bash
velero backup create sts-backup \
  --include-namespaces test-app \
  --selector app=web-sts \
  --wait
```

Backup thành công:

```bash
velero backup get sts-backup
NAME         STATUS      ERRORS   WARNINGS   CREATEDEXPIRES   STORAGE LOCATION   SELECTOR
sts-backup   Completed   0        0          2026-06-10 02:53:57 +0000 UTC   29d       default            app=web-sts
```

Mô phỏng disaster:

```bash
kubectl delete statefulset web -n test-app
kubectl delete service web-headless -n test-app
kubectl delete pvc -n test-app -l app=web-sts

kubectl get statefulset,pvc -n test-app -l app=web-sts
No resources found in test-app namespace.
```

Thực hiện restore test:

```bash
velero restore create sts-restore --from-backup sts-backup --wait
```

Verify volume đã được gắn đúng pod sau khi restore:


```bash
kubectl get po -n test-app -l app=web-sts -o custom-columns=CONTAINER:metadata.name,VOLUME:spec.volumes[0].persistentVolumeClaim.claimName
CONTAINER   VOLUME
web-0       data-web-0
web-1       data-web-1
web-2       data-web-2
```

Verify dữ liệu của volume nằm trên đúng pod identity như trước:

```bash
for i in {0..2}; do echo -n "pod web-$i: ";  kubectl exec -n test-app web-$i -c app -- cat /data/identity.txt; done
pod web-0: pod=web-0 created=2026-06-10T02:47:00Z uid=438d1376-216c-406e-b14f-eaa6c6809a01
pod web-1: pod=web-1 created=2026-06-10T02:47:05Z uid=cb5a30e9-cd6c-4d62-96e8-52f9ac40fbc2
pod web-2: pod=web-2 created=2026-06-10T02:47:11Z uid=d6203eae-1092-4c15-961a-dee7b09dcdea
```

Như vậy mỗi pod web-0/1/2 lấy lại đúng PVC + identity của chính nó. Thứ tự của StatefulSet và identity được giữ nguyên.


### 6. Một số lưu ý

1. **Performance**: Kopia backup ở lớp file-level nên có tốc độ chậm hơn CSI snapshot
2. **PVC backup**: Kopia sẽ được enable bằng `--default-volumes-to-fs-backup` hoặc annotate trong pod `backup.velero.io/backup-volumes`
3. **Định dạng secret**: Velero backup Secrets ở dạng base64 và có thể decode, nên cần đảm bảo backup storage được encrypt

## Tài liệu tham khảo

- [Velero file system backup (Restic / Kopia)](https://velero.io/docs/main/file-system-backup/)
- [Velero backup hooks](https://velero.io/docs/main/backup-hooks/)
- [Velero DR & Migration](https://velero.io/docs/main/disaster-case/)
- [Kasten K10 tài liệu](https://docs.kasten.io/latest/)
- [Kasten backup theo loại application](https://docs.kasten.io/latest/usage/protect.html)
- [Stash tài liệu](https://stash.run/docs/latest/)
- [Stash database backup addons](https://stash.run/docs/latest/addons/)