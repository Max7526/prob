#!/usr/bin/env sh

DIR="$(cd "$(dirname "$0")" && pwd)"

JAVA_HOME=${JAVA_HOME:-}

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD=java
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

"$JAVA_CMD" -version >/dev/null 2>&1 || {
  echo "Java not found, please install JDK" >&2
  exit 1
}

exec "$JAVA_CMD" -Dorg.gradle.appname=gradlew -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
