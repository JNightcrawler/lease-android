#!/bin/sh
#
# Standard Gradle wrapper launch script. Needs gradle/wrapper/gradle-wrapper.jar,
# which isn't included (it's a binary file) - see README.md to regenerate it.
#
DIR="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
