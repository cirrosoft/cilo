#!/usr/bin/env bash

docker build -t cirrosoft/cilo . -f Dockerfile
docker build -t cirrosoft/cilo:dotnet . -f Dockerfile.dotnet
