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
