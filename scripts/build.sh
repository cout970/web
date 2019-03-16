#!/bin/sh

if [[ `pwd` == */scripts ]]; then
    cd ..
fi

chmod +x gradlew

./gradlew build && echo "done"