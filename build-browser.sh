#!/bin/bash

# Builds the browser and copies the static assets into a Nginx image

./gradlew build
docker build \
  --file src/main/nginx/Dockerfile \
  --tag modelgraphtools/browser \
  .
