#!/bin/sh
set -euo pipefail

echo "Starting build process"
if [[ `pwd` == */scripts ]]; then
    cd ..
fi

echo "Making sure gradle exists and can be executed"
chmod +x gradlew

echo "Running build..."
./gradlew build

echo "done"