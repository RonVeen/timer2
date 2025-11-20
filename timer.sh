#!/bin/bash
# Timer Application Wrapper Script
# Runs the timer JAR with appropriate JVM arguments to suppress Java 25 warnings

java --enable-native-access=ALL-UNNAMED \
     -XX:+IgnoreUnrecognizedVMOptions \
     -jar target/timer-app.jar "$@"
