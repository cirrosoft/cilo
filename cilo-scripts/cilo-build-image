#!/usr/bin/env bash
docker image ls | awk '{IFS="\t"; if ($1 == "cirrosoft/cilo") {system("docker rmi -f " $3)}}'
docker build -t cirrosoft/cilo .
