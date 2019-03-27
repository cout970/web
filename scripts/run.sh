#!/bin/sh

if [[ `pwd` == */scripts ]]; then
    cd ..
fi

# Define secret key
echo "Enter secret key: "
read key

echo "Enter DDBB user: "
read user

echo "Enter DDBB pass: "
read password

export ktor_private_key=${key}
export ktor_db_user=${user}
export ktor_db_pass=${password}

echo "Key '${ktor_private_key}', DB user '$ktor_db_user', DB pass '$ktor_db_pass'"

runCommand='java -jar build/libs/web.jar -port=80'

while echo "Starting..."; do
    ${runCommand}
    echo "Server crashed with exit code $?." >&2
    scripts/update.sh | tee /var/log/web.update.log
    echo " Respawning in 2 seconds..."
    sleep 2
done
