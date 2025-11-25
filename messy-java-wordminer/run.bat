@echo off
echo Starting WordMiner Application...
echo.
echo This will take about 30 seconds to initialize Stanford CoreNLP...
echo.
cd /d "%~dp0"
mvn exec:java
pause
