#!/bin/bash

SOURCE_PATH=$1
TARGET_PATH=$2
docker run -t -d -v "$SOURCE_PATH:$TARGET_PATH" sandbox
