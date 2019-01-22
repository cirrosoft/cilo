#!/usr/bin/env bash
isWindows() {
    unameOut="$(uname -s)"
    case "${unameOut}" in
        Linux*)     machine=Linux;;
        Darwin*)    machine=Mac;;
        CYGWIN*)    machine=Cygwin;;
        MINGW*)     machine=MinGw;;
        *)          machine="UNKNOWN:${unameOut}"
    esac
    if  [ "$machine" = "MinGw" ]; then
        true
        return
    fi
    false
}
convertPath() {
    unameOut="$(uname -s)"
    case "${unameOut}" in
        Linux*)     machine=Linux;;
        Darwin*)    machine=Mac;;
        CYGWIN*)    machine=Cygwin;;
        MINGW*)     machine=MinGw;;
        *)          machine="UNKNOWN:${unameOut}"
    esac
    if  [ "$machine" = "MinGw" ]; then
        printf "%s" $1 | sed 's|/c/|//c/|g'
        return
    fi
    printf "%s" $1
}

if isWindows; then
    WD=`convertPath $PWD`
    CILO_HOST_HOME=`convertPath $HOME`
    CILO_MOUNTS="-v $WD:/home/cilo/workspace -v $CILO_HOST_HOME/.cilo:/home/cilo/.cilo"
    winpty docker run -it --rm ${CILO_MOUNTS} --network="host" -e CILO_BOOTSTRAP="true" -e CILO_IS_WINDOWS="true" -e CILO_MOUNTS="${CILO_MOUNTS}" -e CILO_HOST_HOME="${CILO_HOST_HOME}" -e DOCKER_HOST_ADDRESS="host.docker.internal" cirrosoft/cilo:bootstrap $@
else
    CILO_MOUNTS="-v $PWD:/home/cilo/workspace -v $HOME/.cilo:/home/cilo/.cilo"
    docker run -it --rm ${CILO_MOUNTS} --network="host" -e CILO_BOOTSTRAP="true" -e CILO_MOUNTS="${CILO_MOUNTS}" -e DOCKER_HOST_ADDRESS="localhost" cirrosoft/cilo:bootstrap $@
fi


