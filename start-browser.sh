#!/bin/bash

# Starts the browser Nginx image
#
# Parameters
#   1. Port number


PORT=$1


# Prerequisites
if [[ "$#" -ne 1 ]]; then
  echo "Illegal number of parameters. Please use $0 <port>"
  exit 1
fi
if ! [[ $VERSION =~ ^[0-9]+$ ]] ; then
  echo "Illegal port. Must be numeric."
  exit 1
fi


docker run \
  --detach \
  --name=mgt-browser \
  --publish=$PORT:80 \
  --env MGT_API=localhost:9911 \
  modelgraphtools/browser
