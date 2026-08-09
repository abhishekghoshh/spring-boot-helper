#!/bin/bash
set -e

echo "=== Stopping CommerceMesh Platform ==="

docker compose down

echo "CommerceMesh stopped successfully."
