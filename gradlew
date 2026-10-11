#!/bin/sh
# Gradle Wrapper - ينزل Gradle تلقائياً ويبدأ البناء
# الاستخدام: ./gradlew assembleDebug

# Detect project directory
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

# Java detection
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Check Java
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and java is not in PATH."
    echo "Please install Java 17+ from https://adoptium.net/"
    exit 1
fi

# Gradle wrapper JAR location
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle wrapper..."
    
    # Read distribution URL from properties
    DIST_URL=$(grep "distributionUrl" "$WRAPPER_PROPERTIES" | sed 's/.*=\s*//' | sed 's/\\//g')
    
    GRADLE_VERSION=$(echo "$DIST_URL" | sed 's/.*gradle-\(.*\)-bin.*/\1/')
    GRADLE_DIR="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}"
    
    mkdir -p "$GRADLE_DIR"
    
    # Download Gradle
    GRADLE_ZIP="$GRADLE_DIR/gradle-${GRADLE_VERSION}-bin.zip"
    if [ ! -f "$GRADLE_ZIP" ]; then
        echo "Downloading Gradle $GRADLE_VERSION..."
        curl -L -o "$GRADLE_ZIP" "$DIST_URL"
    fi
    
    # Extract
    GRADLE_HOME="$GRADLE_DIR/gradle-${GRADLE_VERSION}"
    if [ ! -d "$GRADLE_HOME" ]; then
        unzip -q -o "$GRADLE_ZIP" -d "$GRADLE_DIR"
    fi
    
    # Run Gradle directly
    exec "$GRADLE_HOME/bin/gradle" "$@"
else
    # Use wrapper JAR
    exec "$JAVACMD" \
        -classpath "$WRAPPER_JAR" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"
fi
