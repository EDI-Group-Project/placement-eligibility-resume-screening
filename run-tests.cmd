@echo off
setlocal
call mvn -q test
if errorlevel 1 exit /b 1
call mvn -q -DRUN_DB_TESTS=true test
