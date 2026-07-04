#!/bin/sh
set -e
cd "$(dirname "$0")"

if [ ! -x .venv/bin/mkdocs ]; then
  python3 -m venv .venv
  ./.venv/bin/pip install -q -r requirements.txt
fi

exec ./.venv/bin/mkdocs serve -f mkdocs.yml -a 127.0.0.1:8001
