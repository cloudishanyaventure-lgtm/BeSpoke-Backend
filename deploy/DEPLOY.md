# Deploying BeSpoke backend to GCP (minimum cost)

Goal: cheapest realistic hosting. Strategy = **one small VM running both the
Spring Boot app and PostgreSQL** (no Cloud SQL — that alone costs $10–25/mo).

## Cost summary

| VM | RAM | Approx cost | Use when |
|----|-----|-------------|----------|
| `e2-micro` | 1 GB | **~$0** (free tier*) | Truly free; needs the 2G swap this setup adds |
| `e2-small` | 2 GB | ~$13/mo | Want it comfortable / reliable |

*Free tier = 1 non-preemptible `e2-micro`/month + 30 GB standard disk, only in
`us-west1`, `us-central1`, or `us-east1`.

**Selected: `e2-small` (2 GB RAM, ~$13/mo) with plain HTTP on port 8080.**
The commands below are already set for this. To drop to the free `e2-micro`
instead, change `--machine-type` and set `JAVA_OPTS=-Xmx256m` in the service.

---

## 1. Build the jar locally

```bash
./gradlew clean bootJar
# produces build/libs/BeSpoke-backend-0.0.1-SNAPSHOT.jar
```

## 2. Create the VM + firewall (run locally, needs gcloud CLI)

```bash
# Log in and pick a project once:
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud config set compute/region us-central1
gcloud config set compute/zone us-central1-a

# Create the VM (e2-small = 2GB RAM, ~$13/mo)
gcloud compute instances create bespoke-vm \
  --machine-type=e2-small \
  --image-family=debian-12 --image-project=debian-cloud \
  --boot-disk-size=30GB --boot-disk-type=pd-standard \
  --tags=http-server

# Allow inbound traffic to port 8080 (the API)
gcloud compute firewall-rules create allow-bespoke-8080 \
  --allow=tcp:8080 --target-tags=http-server \
  --source-ranges=0.0.0.0/0
```

## 3. Provision the VM (Java + Postgres + swap)

```bash
# Copy the setup files up
gcloud compute scp deploy/setup-vm.sh deploy/bespoke.service bespoke-vm:~

# SSH in
gcloud compute ssh bespoke-vm

# On the VM:
export DB_PASSWORD='pick-a-strong-db-password'
sudo -E bash setup-vm.sh
```

## 4. Ship the jar and start the service

```bash
# From your laptop:
gcloud compute scp build/libs/BeSpoke-backend-0.0.1-SNAPSHOT.jar bespoke-vm:~/app.jar

# On the VM:
sudo mv ~/app.jar /opt/bespoke/app.jar
sudo chown bespoke:bespoke /opt/bespoke/app.jar

sudo mv ~/bespoke.service /etc/systemd/system/bespoke.service
sudo nano /etc/systemd/system/bespoke.service   # set DB_PASSWORD, JWT_SECRET, CORS origins
#   -> if on e2-small, bump JAVA_OPTS to -Xmx512m

sudo systemctl daemon-reload
sudo systemctl enable --now bespoke
sudo systemctl status bespoke
sudo journalctl -u bespoke -f      # watch startup + Flyway migrations
```

## 5. Verify

```bash
# Get the VM's public IP:
gcloud compute instances describe bespoke-vm \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'

curl http://EXTERNAL_IP:8080/   # hit any real endpoint from your API
```

---

## Image uploads (GCS)

Logos, covers, portfolio photos and drawings are uploaded through the CRM. With
`STORAGE_BUCKET` unset they are written to `/opt/bespoke/uploads` on this VM —
fine for a first look, but they are lost whenever the VM is rebuilt, and they are
served off the API host rather than a CDN. Point them at a bucket instead.

### 1. Create the bucket and make it publicly readable

Uploaded images are shown to anonymous visitors on bespokedesign.in, so the
objects have to be world-readable. `--uniform-bucket-level-access` is the modern
default and is why the app sets **no** per-object ACLs — public read comes from
this one IAM binding.

```bash
gcloud config set project YOUR_PROJECT_ID

# Bucket names are globally unique. asia-south1 = Mumbai, closest to your users.
gcloud storage buckets create gs://bespoke-uploads \
  --location=asia-south1 --uniform-bucket-level-access

# Anyone can read an object; nobody anonymous can write or list.
gcloud storage buckets add-iam-policy-binding gs://bespoke-uploads \
  --member=allUsers --role=roles/storage.objectViewer
```

### 2. Let the backend write to it

**Option A — the VM's own service account (recommended, no key file).**
A default GCE VM only gets *read-only* storage scope, so it must be widened
once, which needs a stop/start:

```bash
SA=$(gcloud compute instances describe bespoke-vm \
  --format='get(serviceAccounts[0].email)')

gcloud storage buckets add-iam-policy-binding gs://bespoke-uploads \
  --member="serviceAccount:$SA" --role=roles/storage.objectAdmin

gcloud compute instances stop bespoke-vm
gcloud compute instances set-service-account bespoke-vm \
  --service-account="$SA" --scopes=cloud-platform
gcloud compute instances start bespoke-vm
```

**Option B — a service-account key file** (use when the app runs anywhere that
isn't a GCP instance):

```bash
gcloud iam service-accounts create bespoke-uploads --display-name="BeSpoke uploads"
SA=bespoke-uploads@YOUR_PROJECT_ID.iam.gserviceaccount.com

gcloud storage buckets add-iam-policy-binding gs://bespoke-uploads \
  --member="serviceAccount:$SA" --role=roles/storage.objectAdmin

gcloud iam service-accounts keys create gcs-key.json --iam-account="$SA"
gcloud compute scp gcs-key.json bespoke-vm:~
gcloud compute ssh bespoke-vm --command '
  sudo mv ~/gcs-key.json /opt/bespoke/gcs-key.json &&
  sudo chown bespoke:bespoke /opt/bespoke/gcs-key.json &&
  sudo chmod 600 /opt/bespoke/gcs-key.json'
```

Then uncomment `GOOGLE_APPLICATION_CREDENTIALS` in `bespoke.service`.

### 3. Switch it on

```bash
sudo nano /etc/systemd/system/bespoke.service   # STORAGE_BUCKET=bespoke-uploads
sudo systemctl daemon-reload && sudo systemctl restart bespoke
sudo journalctl -u bespoke | grep UPLOADS
```

That last line is the whole check. You want:

```
[UPLOADS] enabled — writing to gs://bespoke-uploads/uploads/
```

If instead you see `credentials could not be resolved`, step 2 didn't take —
the app keeps working and falls back to local disk rather than failing uploads,
so this log line is the only thing that tells you. If you see
`app.storage.bucket is not set`, step 3 didn't take.

Upload an image from the CRM and the URL stored against it should now read
`https://storage.googleapis.com/bespoke-uploads/uploads/<uuid>.png` — open it in
a private window to confirm it is genuinely public.

> Images uploaded *before* this change are stored as URLs pointing at this VM.
> They are not migrated; re-upload them, or copy `/opt/bespoke/uploads` into the
> bucket and rewrite those DB values.

---

## Redeploying after code changes

```bash
./gradlew clean bootJar
gcloud compute scp build/libs/BeSpoke-backend-0.0.1-SNAPSHOT.jar bespoke-vm:~/app.jar
gcloud compute ssh bespoke-vm --command '
  sudo mv ~/app.jar /opt/bespoke/app.jar &&
  sudo chown bespoke:bespoke /opt/bespoke/app.jar &&
  sudo systemctl restart bespoke'
```

## Notes / further cost control

- **Stop the VM when idle**: `gcloud compute instances stop bespoke-vm`. You stop
  paying for CPU/RAM (you still pay a few cents/mo for the disk).
- **Static IP**: an *in-use* ephemeral IP is free; a reserved static IP that's
  unattached is billed. Leave it ephemeral unless you need DNS stability.
- **HTTPS**: port 8080 is plain HTTP. For a real domain, put Caddy or nginx in
  front for free Let's Encrypt TLS, or use a Cloudflare proxy (free).
- **Backups**: `sudo -u postgres pg_dump BeSpoke > backup.sql` on a cron, or
  enable scheduled disk snapshots.
