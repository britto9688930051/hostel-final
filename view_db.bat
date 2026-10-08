@echo off
echo Inspecting SQLite database (hostel.db)...
javac -cp ".;sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar" -d . src/ViewDatabase.java
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)
java -cp ".;sqlite-jdbc.jar;slf4j-api.jar;slf4j-simple.jar" ViewDatabase
pause
