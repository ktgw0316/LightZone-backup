#!/bin/sh -e
MY_DIR=$(realpath $(dirname "$0"))
java -jar "$MY_DIR"/gjavah.jar $@

