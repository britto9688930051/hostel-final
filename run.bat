@echo off
echo Building Hostel Management System Java Backend...
javac -cp ".;sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar" -d . src/*.java
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)
echo Starting Hostel Management Server on http://localhost:8080 ...
java -cp ".;sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar" HostelServer
pause
