@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Android Smartphone OOP Architecture Simulator
echo ========================================================

:: 1. Detect Java Compiler and Runtime
set "JAVAC_BIN=javac"
set "JAVA_BIN=java"

where javac >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" (
        set "JAVAC_BIN=C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
        set "JAVA_BIN=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    ) else (
        echo [ERROR] Java SDK was not found! Please install JDK or Android Studio.
        pause
        exit /b 1
    )
)

echo [1/3] Using Java: "%JAVA_BIN%"

:: 2. Create bin folder and compile
if not exist "bin" mkdir "bin"

echo [2/3] Compiling Java classes...
dir /s /b src\*.java > sources.txt
"%JAVAC_BIN%" -encoding UTF-8 -d bin @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo [3/3] Starting Localhost HTTP Server on port 8088...
start "" http://localhost:8088
"%JAVA_BIN%" -cp bin com.android.Main

pause
