#!/usr/bin/env bash
set -euo pipefail

repo='/srv/springer'
name='springer-bot'
region='us-east-1'
log_group='/springer/lichess-bot'
state_dir='/var/lib/springer-deploy'
last_attempt="$state_dir/last-attempt"

log() {
    local message="${*//,/;}"
    echo "$message"
    aws logs create-log-stream --region "$region" --log-group-name "$log_group" \
        --log-stream-name deploy > /dev/null 2>&1 || true
    aws logs put-log-events --region "$region" --log-group-name "$log_group" \
        --log-stream-name deploy \
        --log-events "timestamp=$(date +%s%3N),message=$message" > /dev/null 2>&1 \
        || echo 'could not write to CloudWatch' >&2
}

mkdir -p "$state_dir"
cd "$repo"

git fetch --quiet origin main
target="$(git rev-parse --short origin/main)"
running="$(docker ps --filter "name=^/${name}\$" --format '{{.Image}}')"

if [[ "$running" == "$name:$target" ]]; then
    exit 0
fi

if [[ -n "$running" && -f "$last_attempt" && "$(cat "$last_attempt")" == "$target" ]]; then
    exit 0
fi

echo "$target" > "$last_attempt"
log "deploying $target"
if git merge --ff-only --quiet origin/main && bash run-aws.sh; then
    log "deployed $target"
else
    log "deploy of $target failed; see journalctl -u springer-deploy"
    exit 1
fi
