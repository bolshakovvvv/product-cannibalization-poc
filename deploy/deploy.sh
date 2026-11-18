#!/usr/bin/env bash
set -Eeuo pipefail

release="${1:?Release ID is required}"
[[ "$release" =~ ^[a-f0-9]{40}-[0-9]+-[0-9]+$ ]] || {
  echo "Invalid release ID"
  exit 1
}

cd "$HOME/nis"

exec 9>.deploy.lock
flock -n 9 || {
  echo "Another deployment is running"
  exit 1
}

package="$PWD/releases/$release"
test -s "$package/back.jar"
test -s "$package/frontend/index.html"
test -s "$package/compose.yaml"
test -s "$package/nginx.conf"
test -f .env

if [[ ! -f .release.env ]]; then
  printf 'RELEASE_ID=initial\n' > .release.env
fi

backup="$package/previous"
mkdir -p "$backup"
cp compose.yaml nginx.conf .release.env "$backup/"

dc() {
  sudo -n docker compose \
    --project-name nis \
    --env-file .env \
    --env-file .release.env \
    -f compose.yaml "$@"
}

rollback() {
  trap - ERR
  set +e
  echo "Deployment failed. Restoring previous application."

  cp "$backup/compose.yaml" compose.yaml
  cp "$backup/nginx.conf" nginx.conf
  cp "$backup/.release.env" .release.env

  dc up -d --no-deps --force-recreate backend
  dc up -d --no-deps --force-recreate nginx
  dc ps

  echo "Previous configuration restored. Check application availability."
  exit 1
}

trap rollback ERR

cp "$package/compose.yaml" compose.yaml
cp "$package/nginx.conf" nginx.conf
printf 'RELEASE_ID=%s\n' "$release" > .release.env

dc config --quiet

dc up -d --no-deps --force-recreate backend
dc up -d --no-deps --force-recreate nginx
dc exec -T nginx nginx -t

healthy=false

for attempt in $(seq 1 30); do
  if dc exec -T nginx \
      wget -q -T 3 -O - http://backend:8080/actuator/health \
      > "$package/health.json" 2>/dev/null \
    && grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' \
      "$package/health.json" \
    && curl -fsS --max-time 3 http://127.0.0.1/ \
      > /dev/null \
    && curl -fsS --max-time 3 http://127.0.0.1/release.txt \
      > "$package/served-release.txt" \
    && grep -Fxq "$release" "$package/served-release.txt"; then
    healthy=true
    break
  fi
  sleep 3
done

if [[ "$healthy" != true ]]; then
  echo "Application did not pass health checks."
  dc logs --tail 60 backend nginx || true
  false
fi

trap - ERR
echo "Deployment successful: $release"
dc ps