@echo off
REM Requiere JDK 17 e internet. Genera build\libs\CustomHeads-1.20.1-Fabric.jar
call gradlew.bat build
echo.
echo JAR: %CD%\build\libs\CustomHeads-1.20.1-Fabric.jar
pause
