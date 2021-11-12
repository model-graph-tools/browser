#!/bin/bash

# Pushes the browser image to quay.io
#
# Parameters
#   1. version (optional)
#
# See https://stackoverflow.com/a/40771884
# for the ${TAG}${VERSION:+:$VERSION} syntax


VERSION=$1
TAG=quay.io/modelgraphtools/browser


# This requires a valid configuration in ~/.docker/config.json
docker login quay.io
docker push ${TAG}${VERSION:+:$VERSION}
