#!/bin/bash
echo "Building Hostel Management System Java Backend..."
javac -cp ".:sqlite-jdbc.jar:slf4j-api.jar:slf4j-simple.jar" -d . src/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi
echo "Starting Hostel Management Server on http://localhost:8080 ..."
java -cp ".:sqlite-jdbc.jar:slf4j-api.jar:slf4j-simple.jar" HostelServer
