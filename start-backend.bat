@echo off
echo Starting backend...
cd /d "%~dp0"
java -Xmx512m -jar target\backend-1.0.0.jar
pause
