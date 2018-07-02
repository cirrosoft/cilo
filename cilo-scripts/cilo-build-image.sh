#!/usr/bin/env bash
docker image ls | awk '{IFS="\t"; if ($1 == "cilo") {system("docker rmi -f " $3)}}'
docker build -t cilo .
