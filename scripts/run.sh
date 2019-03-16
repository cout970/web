#!/bin/sh

if [[ `pwd` == */scripts ]]; then
    cd ..
fi

runCommand='java -jar build/libs/web.jar -port=8080'

until ${runCommand}; do
    echo "Server crashed with exit code $?. Respawning in 1 second.." >&2
    sleep 1
done
