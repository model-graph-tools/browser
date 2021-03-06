#!/bin/bash

# Builds the browser and copies the static assets into a Nginx image
#
# Parameters
#   1. version (optional)
#
# See https://stackoverflow.com/a/40771884
# for the ${TAG}${VERSION:+:$VERSION} syntax


VERSION=$1
TAG=quay.io/modelgraphtools/browser


./gradlew build
docker build \
  --file src/main/nginx/Dockerfile \
  --tag ${TAG}${VERSION:+:$VERSION} \
  .
