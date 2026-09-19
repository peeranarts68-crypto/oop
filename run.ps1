# Android Phone OOP Simulator PowerShell Launcher

$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   Android Smartphone OOP Architecture Simulator        " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan

# Detect Java
$javac = "javac"
$java = "java"

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    $jbrJavac = "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
    $jbrJava = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    if (Test-Path $jbrJavac) {
        $javac = $jbrJavac
        $java = $jbrJava
    } else {
        Write-Error "Java compiler not found in PATH or Android Studio JBR."
        exit 1
    }
}

Write-Host "[1/3] Using Java: $java" -ForegroundColor Yellow

# Create bin directory
if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

# Compile
Write-Host "[2/3] Compiling Java source files..." -ForegroundColor Yellow
$sources = Get-ChildItem -Path src -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d bin $sources

# Run
Write-Host "[3/3] Starting Server at http://localhost:8088" -ForegroundColor Green
Start-Process "http://localhost:8088"
& $java -cp bin com.android.Main
