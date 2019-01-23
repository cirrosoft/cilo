:: turn off echo for docker command
@echo off
set CURRENT_DIR=%cd%
set CILO_HOST_HOME=%userprofile%
set CILO_MOUNTS=-v %CURRENT_DIR%:/home/cilo/workspace -v %CILO_HOST_HOME%/.cilo:/home/cilo/.cilo
docker run -it --rm %CILO_MOUNTS% --network="host" -e CILO_IS_NESTED="true" -e CILO_HOST_OS="windows" -e CILO_MOUNTS="%CILO_MOUNTS%" -e CILO_HOST_HOME="%CILO_HOST_HOME%" -e CILO_DOCKER_ADDRESS="host.docker.internal" cirrosoft/cilo:bootstrap %*
:: turn echo back on
@echo on
