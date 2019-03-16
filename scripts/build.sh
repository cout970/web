#!/bin/sh
set -euo pipefail

echo "Starting build process"
if [[ `pwd` == */scripts ]]; then
    cd ..
fi

echo "Running build..."
./gradlew build

echo "done"