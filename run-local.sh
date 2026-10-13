#!/usr/bin/env bash
set -euo pipefail

tag='springer-bot:local'
name='springer-local'
# Multi-platform (amd64 + arm64) lichess-bot base
bot_image='lichessbotdevs/lichess-bot@sha256:59bf9e108935ebc8c2cc09532434e1b1e37963c755d5404eec9a0133dd70d4a5'

cd "$(dirname "${BASH_SOURCE[0]}")"

echo "Building $tag (unit tests run during the build)..."
if ! docker build --build-arg "BOT_IMAGE=$bot_image" --tag "$tag" .; then
    echo 'Image build failed.' >&2
    exit 1
fi

# Only one container may use the bot account at a time, so replace any existing container
if [[ -n "$(docker ps -a --filter "name=^/${name}\$" --format '{{.Names}}')" ]]; then
    echo "Stopping and removing existing container '$name'..."
    docker stop --time 30 "$name" > /dev/null
    docker rm "$name" > /dev/null
fi

read -rsp 'Lichess bot token: ' token
echo
if [[ ! "$token" =~ ^[A-Za-z0-9_]{16,}$ ]]; then
    echo 'That does not look like a Lichess token.' >&2
    exit 1
fi

if ! LICHESS_BOT_TOKEN="$token" docker run -d --name "$name" --init --env LICHESS_BOT_TOKEN \
        --log-opt max-size=10m --log-opt max-file=3 "$tag" > /dev/null; then
    unset token
    echo 'Container failed to start.' >&2
    exit 1
fi
unset token

sleep 5
docker ps -a --filter "name=^/${name}\$" --format 'table {{.Names}}\t{{.Status}}'
docker logs --tail 20 "$name"
echo
echo "Follow logs: docker logs -f $name"
echo "Stop:        docker stop --time 30 $name"
