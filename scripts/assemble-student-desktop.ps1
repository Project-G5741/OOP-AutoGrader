param(
    [Parameter(Mandatory = $true)]
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$backendTarget = Join-Path $repoRoot "backend\target"
$frontendDist = Join-Path $repoRoot "frontend\dist-desktop"

if (-not (Test-Path (Join-Path $backendTarget "backend-1.0.0.jar"))) {
    Write-Host "Building backend..."
    Push-Location (Join-Path $repoRoot "backend")
    mvn -q -DskipTests package
    Pop-Location
}

if (-not (Test-Path $frontendDist)) {
    Write-Host "Building desktop frontend..."
    Push-Location (Join-Path $repoRoot "frontend")
    npm run build:desktop
    Pop-Location
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $OutputDir "rubric") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $OutputDir "data") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $OutputDir "ui") | Out-Null

Copy-Item (Join-Path $backendTarget "backend-1.0.0.jar") (Join-Path $OutputDir "backend.jar") -Force
Copy-Item (Join-Path $backendTarget "backend-1.0.0-worker.jar") (Join-Path $OutputDir "worker.jar") -Force
Copy-Item $frontendDist (Join-Path $OutputDir "ui\dist-desktop") -Recurse -Force

$launcherSrc = Join-Path $repoRoot "backend\src\main\resources\student-desktop\OOP-AutoGrader-Practice.bat"
$readmeSrc = Join-Path $repoRoot "backend\src\main\resources\student-desktop\README.txt"
Copy-Item $launcherSrc (Join-Path $OutputDir "OOP-AutoGrader-Practice.bat") -Force
Copy-Item $readmeSrc (Join-Path $OutputDir "README.txt") -Force

$launcherProj = Join-Path $repoRoot "desktop\launcher\OOPAutoGraderPractice\OOPAutoGraderPractice.csproj"
$launcherPublish = Join-Path $repoRoot "desktop\launcher\publish"
if (Get-Command dotnet -ErrorAction SilentlyContinue) {
    Write-Host "Publishing WebView2 launcher..."
    dotnet publish $launcherProj -c Release -r win-x64 --self-contained true -o $launcherPublish
    Copy-Item (Join-Path $launcherPublish "*") $OutputDir -Recurse -Force
} else {
    Write-Warning "dotnet SDK not found; only OOP-AutoGrader-Practice.bat was copied."
}

Write-Host "Assembled student desktop layout at $OutputDir"
Write-Host "Students: run OOP-AutoGrader-Practice.exe (preferred) or .bat — Java 17+ or runtime/jdk"
Write-Host "Web download bundles rubric/term.agpack for the current quarter"
