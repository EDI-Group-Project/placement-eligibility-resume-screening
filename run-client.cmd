@echo off
setlocal
call mvn -q clean package
if errorlevel 1 exit /b 1
java -jar target\placement-eligibility-resume-screening-1.0.0.jar
