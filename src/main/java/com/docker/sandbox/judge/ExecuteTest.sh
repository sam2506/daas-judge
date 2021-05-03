#!/bin/bash

COMPILER=$1
FILE_NAME=$2
EXECUTABLE=$3
CONTAINER_ID=$4
TEST_CASE_NO=$5
SUBMISSION_FOLDER_PATH=$6
TIME_LIMIT=$7
MEMORY_LIMIT_IN_MB=$8

TEST_CASE_FILE_PATH=$SUBMISSION_FOLDER_PATH"/input/input-$TEST_CASE_NO.txt"
OUTPUT_FILE_PATH=$SUBMISSION_FOLDER_PATH"/output/output-$TEST_CASE_NO.txt"
USER_OUTPUT_FILE_PATH=$SUBMISSION_FOLDER_PATH"/user-output-$TEST_CASE_NO.txt"

if [ "$EXECUTABLE" = "" ]; then
  docker exec $CONTAINER_ID bash -c "ulimit -t $TIME_LIMIT -v $((MEMORY_LIMIT_IN_MB*1024));{ /usr/bin/time -f '%U' $COMPILER $SUBMISSION_FOLDER_PATH/$FILE_NAME < $TEST_CASE_FILE_PATH > $USER_OUTPUT_FILE_PATH; } 2> $SUBMISSION_FOLDER_PATH/time-$TEST_CASE_NO.txt"
else
  docker exec $CONTAINER_ID bash -c "ulimit -t $TIME_LIMIT -v $((MEMORY_LIMIT_IN_MB*1024));{ /usr/bin/time -f '%U' $SUBMISSION_FOLDER_PATH/$EXECUTABLE < $TEST_CASE_FILE_PATH > $USER_OUTPUT_FILE_PATH; } 2> $SUBMISSION_FOLDER_PATH/time-$TEST_CASE_NO.txt"
fi

EXIT_STATUS=$?

if [ $EXIT_STATUS -eq 0 ]; then
  echo "user output file generated"
  docker exec $CONTAINER_ID bash -c "diff -B -Z $USER_OUTPUT_FILE_PATH $OUTPUT_FILE_PATH &>/dev/null"
  if [ $? -eq 0 ]; then
    echo "test case $TEST_CASE_NO passed"
    exit 0;
  else
    echo "test case $TEST_CASE_NO failed";
    exit 1;
  fi

else
  if [ $EXIT_STATUS -eq 137 ]; then
    echo "time limit exceeded on test case $TEST_CASE_NO"
    exit 124;
  else
    if [ $EXIT_STATUS -eq 127 ]; then
      echo "memory limit exceeded on test case $TEST_CASE_NO"
      exit 139;
    else
      echo "runtime error on testCase $TEST_CASE_NO"
      exit 134;
    fi
  fi
fi


