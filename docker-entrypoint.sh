#!/bin/sh
set -eu

SECRET_DIR="${SECRET_DIR:-/var/lib/budget-secrets}"
SECRET_FILE="${SECRET_FILE:-$SECRET_DIR/encryption_secret}"

mkdir -p "$SECRET_DIR"
chmod 700 "$SECRET_DIR"

if [ -s "$SECRET_FILE" ]; then
  ENCRYPTION_SECRET="$(cat "$SECRET_FILE")"
elif [ -n "${ENCRYPTION_SECRET:-}" ]; then
  printf '%s' "$ENCRYPTION_SECRET" > "$SECRET_FILE"
  chmod 600 "$SECRET_FILE"
else
  ENCRYPTION_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
  printf '%s' "$ENCRYPTION_SECRET" > "$SECRET_FILE"
  chmod 600 "$SECRET_FILE"
fi

export ENCRYPTION_SECRET

exec java -jar "$(find /app/target -maxdepth 1 -name '*.jar' ! -name '*original*' | head -n 1)"
