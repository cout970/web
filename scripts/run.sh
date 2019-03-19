#!/bin/sh

if [[ `pwd` == */scripts ]]; then
    cd ..
fi

# Define secret key
echo "Enter secret key: "
read key
export ktor_private_key=${key}
echo "Key '${ktor_private_key}'"

runCommand='java -jar build/libs/web.jar -port=80'

while echo "Starting..."; do
    ${runCommand}
    echo "Server crashed with exit code $?. Respawning in 1 second.." >&2
    sleep 1
done
