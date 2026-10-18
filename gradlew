#!/bin/sh

# Standard POSIX Gradle Wrapper script for HephaistOS
set -e

DIRNAME="$(cd "$(dirname "$0")" && pwd)"

# Check if system gradle exists
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

# Fallback to Gradle wrapper jar if present
CLASSPATH="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$CLASSPATH" ]; then
    JAVA_EXE="java"
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JAVA_EXE="$JAVA_HOME/bin/java"
    fi
    exec "$JAVA_EXE" -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "Erreur : Gradle n'est pas installé sur le système et gradle-wrapper.jar est manquant." >&2
echo "Veuillez installer Gradle (https://gradle.org/install/) ou utiliser l'action GitHub Actions fournie." >&2
exit 1
