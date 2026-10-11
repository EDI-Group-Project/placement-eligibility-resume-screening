@echo off
setlocal
call mvn -q clean compile
if errorlevel 1 exit /b 1
mvn -q exec:java -Dexec.mainClass=com.placement.sockets.Server
