@echo off
chcp 65001
cls
setlocal enabledelayedexpansion
echo Build tool start

echo 0. Path check
cd /d "%~dp0"

echo 1. Cleanup files
if exist bin rmdir /s /q bin
mkdir bin
if exist docToText.jar del /f /q docToText.jar

echo 2. Compiling Java sourceCode ...
javac -encoding UTF-8 -cp "lib/*" -d bin src/*.java

if %errorlevel% neq 0 (
	echo [ERROR] Error compling JAVA
	pause
	exit /b
)

echo 3. Set Manifest
set "CLASSPATH_STR="
for %%f in (lib\*.jar) do (
	set "CLASSPATH_STR=!CLASSPATH_STR! lib/%%~nxf"
)

(
echo Manifest-Version: 1.0
echo Main-Class: DocToTextApp
echo Class-Path: !CLASSPATH_STR!
echo.
) > manifest.txt

echo 4. Generate JAR
jar cvfm docToText.jar manifest.txt -C bin . -C src images

if exist manifest.txt del /f /q manifest.txt
echo Successfully built

pause