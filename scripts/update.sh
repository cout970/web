#!/bin/sh
set -euo pipefail

function checkForUpdates() {
    UPSTREAM=${1:-'@{u}'}
    LOCAL=$(git rev-parse @)
    REMOTE=$(git rev-parse "$UPSTREAM")
    BASE=$(git merge-base @ "$UPSTREAM")

    if [[ ${LOCAL} = ${REMOTE} ]]; then
        echo "Up-to-date"
    elif [[ ${LOCAL} = ${BASE} ]]; then
        echo "Need to pull"
        return 0
    elif [[ ${REMOTE} = ${BASE} ]]; then
        echo "Need to push"
    else
        echo "Diverged"
    fi

    return 1
}

if checkForUpdates; then
    echo "Downloading latest version"
    git pull origin master
    echo "Compiling code"
    ./build.sh
    echo "done"
fi