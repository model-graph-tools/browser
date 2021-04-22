#!/bin/bash

# Starts the Nginx image with the static browser assets

docker run \
  --detach \
  --env MGT_API=http://localhost:9911 \
  --name=mgt-browser \
  --publish=80:80 \
  modelgraphtools/browser
