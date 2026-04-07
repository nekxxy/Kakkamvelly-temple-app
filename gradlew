#!/bin/sh
exec "$JAVA_HOME/bin/java" -jar "$( cd "$(dirname "$0")" ; pwd -P )"/gradle/wrapper/gradle-wrapper.jar "$@"
