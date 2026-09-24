#!/usr/bin/env bash
# poll-health.sh — wait for the validator app to report healthy (max ~20 min), then show onboarding lines.
for i in $(seq 1 40); do
  s=$(sudo docker inspect --format '{{.State.Health.Status}}' splice-validator-validator-1 2>/dev/null)
  echo "$(date -u +%H:%M:%S) $i $s"
  [ "$s" = "healthy" ] && break
  sleep 30
done
echo "===ONBOARDLOG"
sudo docker logs splice-validator-validator-1 2>&1 \
  | grep -iE 'onboard|validator party|party id|is ready|healthy|ERROR|WARN' \
  | grep -v db-storage | sed 's/secret[^,]*/secret=x/g' | cut -c1-300 | tail -25
echo "===HEALTH"
sudo docker inspect --format '{{json .State.Health.Log}}' splice-validator-validator-1 | tail -c 400
