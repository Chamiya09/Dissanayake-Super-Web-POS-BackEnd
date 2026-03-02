@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

echo.
echo   Starting Dissanayake Super Web POS Backend ...
echo   Profile: local  ^|  Port: 8080
echo.

C:\Users\user\.m2\maven-3.9.12\bin\mvn.cmd spring-boot:run -Dspring-boot.run.profiles=local --no-transfer-progress
