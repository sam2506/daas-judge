#!/bin/bash

COMPILER=$1
FILE_NAME=$2
CONTAINER_ID=$3
SUBMISSION_FOLDER_PATH=$4
EXECUTABLE=$5

docker exec $CONTAINER_ID bash -c "$COMPILER -o $SUBMISSION_FOLDER_PATH/$EXECUTABLE $SUBMISSION_FOLDER_PATH/$FILE_NAME > $SUBMISSION_FOLDER_PATH/errors.txt"

if [ $? -eq 0 ]; then
  echo "compilation successful"
  exit 0;
else
  echo "compilation error"
  exit 1;
fi

