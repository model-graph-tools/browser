#!/bin/bash

# Builds a Nginx image with the static browser assets
#
# Requires a gradle production build
# and expects the HTML/JS assets in build/distributions


# Prerequisites
if ! [[ -f "build/distributions/model-graph-browser.js" ]]
then
    echo "No browser build found! Please build browser using './gradlew build' first'."
    exit 1
fi


docker build \
  --file src/main/nginx/Dockerfile \
  --tag modelgraphtools/browser \
  .
