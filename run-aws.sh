#!/usr/bin/env bash
set -euo pipefail

name='springer-bot'
# Multi-platform (amd64 + arm64) lichess-bot base
bot_image='lichessbotdevs/lichess-bot@sha256:59bf9e108935ebc8c2cc09532434e1b1e37963c755d5404eec9a0133dd70d4a5'
region='us-east-1'
token_parameter='/springer/lichess-bot-token'
log_group='/springer/lichess-bot'
# How long stopping the bot waits for current games to finish before force-killing it
drain_seconds=3600

cd "$(dirname "${BASH_SOURCE[0]}")"

# Tag the image with the commit it was built from, so an older version can be run again
commit="$(git rev-parse --short HEAD)"
tag="springer-bot:$commit"

echo "Building $tag (unit tests run during the build)..."
if ! docker build --build-arg "BOT_IMAGE=$bot_image" --tag "$tag" .; then
    echo 'Image build failed.' >&2
    exit 1
fi

# Read the token before stopping the running bot, so a failure here doesn't leave it stopped
echo "Reading the token from Parameter Store ($token_parameter)..."
if ! token="$(aws ssm get-parameter --region "$region" --name "$token_parameter" \
        --with-decryption --query Parameter.Value --output text)"; then
    echo 'Could not read the token. Check the instance role.' >&2
    exit 1
fi
if [[ ! "$token" =~ ^[A-Za-z0-9_]{16,}$ ]]; then
    echo 'The stored value does not look like a Lichess token.' >&2
    exit 1
fi

# Only one container may use the bot account at a time, so replace any existing container.
if [[ -n "$(docker ps -a --filter "name=^/${name}\$" --format '{{.Names}}')" ]]; then
    echo "Stopping '$name'. If a game is in progress, this waits for it to finish (up to $drain_seconds s)..."
    docker stop --time "$drain_seconds" "$name" > /dev/null
    docker rm "$name" > /dev/null
fi

if ! LICHESS_BOT_TOKEN="$token" docker run -d --name "$name" --init --env LICHESS_BOT_TOKEN \
        --restart unless-stopped --stop-timeout "$drain_seconds" \
        --log-driver awslogs \
        --log-opt awslogs-region="$region" \
        --log-opt awslogs-group="$log_group" \
        --log-opt awslogs-stream="$name-$commit" \
        --log-opt mode=non-blocking --log-opt max-buffer-size=4m \
        "$tag" > /dev/null; then
    unset token
    echo 'Container failed to start.' >&2
    exit 1
fi
unset token

sleep 5
docker ps -a --filter "name=^/${name}\$" --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
docker logs --tail 20 "$name"
echo
echo "Follow logs: sudo docker logs -f $name"
echo "CloudWatch:  log group $log_group, stream $name-$commit"
echo "Stop:        sudo docker stop $name   (waits up to $drain_seconds s for games to finish)"
