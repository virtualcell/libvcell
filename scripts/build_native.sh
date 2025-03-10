#!/usr/bin/env bash

set -e   # exit on error
set -u   # exit on using unset variable
set -x   # echo all commands

# ROOT_DIR of this repository
ROOT_DIR="$( cd "$( dirname "$( dirname "${BASH_SOURCE[0]}" )" )" && pwd )"
echo "ROOT_DIR: $ROOT_DIR"

cd "$ROOT_DIR"/vcell || ( echo "'vcell' directory not found" && exit 1 )
mvn clean install -DskipTests

# test if JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is not set"
  exit 1
fi

# test if native-image is installed
if ! command -v native-image &> /dev/null; then
  echo "native-image could not be found"
  exit 1
fi

cd "$ROOT_DIR"/vcell-native || ( echo "'vcell-native' directory not found" && exit 1 )

# build vcell-native as java
mvn clean install

# run with native-image-agent to record configuration for native-image
java -agentlib:native-image-agent=config-output-dir=target/recording \
     -jar target/vcell-native-1.0-SNAPSHOT.jar \
     "$ROOT_DIR/vcell-native/src/test/resources/TinySpacialProject_Application0.xml" \
     "$ROOT_DIR/target/sbml-input"

# build vcell-native as native shared object library
mvn package -P shared-dll
