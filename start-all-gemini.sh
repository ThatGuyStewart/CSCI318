#!/usr/bin/env bash

export SPRING_PROFILES_ACTIVE=gemini
cd -- "$(dirname -- "${BASH_SOURCE[0]}")" || exit 1
exec bash ./start-all.sh "$@"
