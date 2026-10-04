param(
    [Parameter(Mandatory = $true)]
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$backendTarget = Join-Path $repoRoot "backend\target"
$frontendDist = Join-Path $repoRoot "frontend\dist-desktop"
# Fixed path the web API streams (cwd = backend/ → target/OOP-AutoGrader-Practice.zip)
$webDownloadZip = Join-Path $backendTarget "OOP-AutoGrader-Practice.zip"

$desktopBackendJar = Join-Path $backendTarget "backend-1.0.0-desktop.jar"
$workerJar = Join-Path $backendTarget "backend-1.0.0-worker.jar"

function Get-DirectorySizeMb([string]$Path) {
    if (-not (Test-Path $Path)) { return 0 }
    $sum = (Get-ChildItem -LiteralPath $Path -Recurse -File -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum).Sum
    if (-not $sum) { return 0 }
    return [math]::Round($sum / 1MB, 1)
}

function Get-FileSizeMb([string]$Path) {
    if (-not (Test-Path $Path)) { return 0 }
    return [math]::Round((Get-Item -LiteralPath $Path).Length / 1MB, 1)
}

function Resolve-JdkHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\jlink.exe"))) {
        return $env:JAVA_HOME
    }
    $jlinkCmd = Get-Command jlink -ErrorAction SilentlyContinue
    if ($jlinkCmd) {
        $binDir = Split-Path -Parent $jlinkCmd.Source
        $jdkCandidate = Split-Path -Parent $binDir
        if (Test-Path (Join-Path $jdkCandidate "bin\jlink.exe")) {
            return $jdkCandidate
        }
    }
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaPath = $javaCmd.Source
        $binDir = Split-Path -Parent $javaPath
        $jdkCandidate = Split-Path -Parent $binDir
        if (Test-Path (Join-Path $jdkCandidate "bin\jlink.exe")) {
            return $jdkCandidate
        }
    }
    foreach ($candidate in @(
            "C:\Program Files\Java\jdk-23",
            "C:\Program Files\Java\jdk-21",
            "C:\Program Files\Java\jdk-17",
            "C:\Program Files\Eclipse Adoptium\jdk-17*"
        )) {
        foreach ($item in @(Get-Item -Path $candidate -ErrorAction SilentlyContinue)) {
            if (Test-Path (Join-Path $item.FullName "bin\jlink.exe")) {
                return $item.FullName
            }
        }
    }
    return $null
}

function New-PortableJdkImage {
    param(
        [Parameter(Mandatory = $true)][string]$JdkHome,
        [Parameter(Mandatory = $true)][string]$DesktopJar,
        [Parameter(Mandatory = $true)][string]$DestDir
    )

    $jlink = Join-Path $JdkHome "bin\jlink.exe"
    $jdeps = Join-Path $JdkHome "bin\jdeps.exe"
    if (-not (Test-Path $jlink)) {
        throw "jlink not found under $JdkHome"
    }

    $extractRoot = Join-Path $backendTarget "desktop-jlink-extract"
    if (Test-Path $extractRoot) {
        Remove-Item $extractRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null
    # Expand-Archive wants a .zip extension on some hosts.
    $zipCopy = Join-Path $backendTarget "backend-desktop-for-jdeps.zip"
    Copy-Item -LiteralPath $DesktopJar -Destination $zipCopy -Force
    Expand-Archive -LiteralPath $zipCopy -DestinationPath $extractRoot -Force
    $libDir = Join-Path $extractRoot "BOOT-INF\lib"
    $classPath = $libDir
    if (Test-Path $libDir) {
        $jars = Get-ChildItem -LiteralPath $libDir -Filter "*.jar" | ForEach-Object { $_.FullName }
        if ($jars.Count -gt 0) {
            $classPath = ($jars -join ";")
        }
    }

    $moduleDeps = "java.base,java.logging,java.xml,java.sql,java.naming,java.management,java.instrument,java.desktop,java.prefs,java.scripting,java.rmi,java.security.jgss,java.compiler,jdk.compiler,jdk.zipfs,jdk.unsupported,jdk.crypto.ec,java.net.http"
    if (Test-Path $jdeps) {
        Write-Host "Resolving jlink modules via jdeps..."
        $jdepsOut = & $jdeps --ignore-missing-deps --multi-release 17 --print-module-deps --class-path $classPath $DesktopJar 2>&1
        if ($LASTEXITCODE -eq 0 -and $jdepsOut) {
            $line = ($jdepsOut | Select-Object -Last 1).ToString().Trim()
            if ($line -and $line -notmatch " ") {
                $moduleDeps = $line
            } else {
                Write-Warning "jdeps returned an unexpected module list; using default module set."
            }
        } else {
            Write-Warning "jdeps failed (exit $LASTEXITCODE); using default module set. Output: $jdepsOut"
        }
    }
    # Always keep compiler + common Spring/H2 modules jdeps may omit.
    foreach ($required in @(
            "java.compiler", "jdk.compiler", "jdk.zipfs",
            "java.logging", "java.xml", "java.sql", "java.naming")) {
        if ($moduleDeps -notmatch [regex]::Escape($required)) {
            $moduleDeps = "$moduleDeps,$required"
        }
    }

    if (Test-Path $DestDir) {
        Remove-Item $DestDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $DestDir) | Out-Null

    Write-Host "Building portable JDK with jlink ($moduleDeps)..."
    & $jlink `
        --add-modules $moduleDeps `
        --strip-debug `
        --no-header-files `
        --no-man-pages `
        --compress=2 `
        --output $DestDir
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path (Join-Path $DestDir "bin\java.exe"))) {
        throw "jlink failed to produce $DestDir\bin\java.exe"
    }
}

if (-not (Test-Path $desktopBackendJar) -or -not (Test-Path $workerJar)) {
    Write-Host "Building backend (desktop-dist)..."
    Push-Location (Join-Path $repoRoot "backend")
    mvn -q -DskipTests package -Pdesktop-dist
    Pop-Location
    if (-not (Test-Path $desktopBackendJar)) {
        throw "Expected desktop artifact missing after mvn -Pdesktop-dist package: $desktopBackendJar"
    }
    if (-not (Test-Path $workerJar)) {
        throw "Expected worker artifact missing after package: $workerJar"
    }
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

if (-not (Test-Path $desktopBackendJar)) {
    throw "Desktop classifier JAR missing: $desktopBackendJar. Rebuild with: mvn -DskipTests package -Pdesktop-dist"
}
if (-not (Test-Path $workerJar)) {
    throw "Worker JAR missing: $workerJar"
}
$shippedBackendJar = Join-Path $OutputDir "backend.jar"
Copy-Item $desktopBackendJar $shippedBackendJar -Force
Copy-Item $workerJar (Join-Path $OutputDir "worker.jar") -Force
$uiDest = Join-Path $OutputDir "ui\dist-desktop"
if (Test-Path $uiDest) {
    Remove-Item -LiteralPath $uiDest -Recurse -Force
}
Copy-Item $frontendDist $uiDest -Recurse -Force

$launcherSrc = Join-Path $repoRoot "backend\src\main\resources\student-desktop\OOP-AutoGrader-Practice.bat"
$readmeSrc = Join-Path $repoRoot "backend\src\main\resources\student-desktop\README.txt"
Copy-Item $launcherSrc (Join-Path $OutputDir "OOP-AutoGrader-Practice.bat") -Force
Copy-Item $readmeSrc (Join-Path $OutputDir "README.txt") -Force

$jdkHome = Resolve-JdkHome
$runtimeJdk = Join-Path $OutputDir "runtime\jdk"
if ($jdkHome) {
    New-PortableJdkImage -JdkHome $jdkHome -DesktopJar $shippedBackendJar -DestDir $runtimeJdk
} else {
    Write-Warning "No JDK with jlink found (set JAVA_HOME). Practice folder will not include runtime\jdk."
}

$practiceHostManifest = Join-Path $repoRoot "desktop\launcher\practice-host\Cargo.toml"
$practiceHostTarget = Join-Path $repoRoot "desktop\launcher\practice-host\target"
$practiceHostExe = Join-Path $practiceHostTarget "release\OOP-AutoGrader-Practice.exe"
if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
    throw "cargo not found. Install Rust stable (https://rustup.rs) and MSVC Build Tools, then re-run assemble."
}
Write-Host "Building Rust practice host (cargo --release)..."
# --target-dir pins output even when the shell exports a sandbox CARGO_TARGET_DIR.
cargo build --release --manifest-path $practiceHostManifest --target-dir $practiceHostTarget
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $practiceHostExe)) {
    throw "cargo build failed for the practice host (expected $practiceHostExe)"
}
Copy-Item $practiceHostExe (Join-Path $OutputDir "OOP-AutoGrader-Practice.exe") -Force

# Strip local-run leftovers so a reused OutputDir never ships WebView2 caches or H2 DB files.
Get-ChildItem -LiteralPath $OutputDir -Force -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "*.WebView2" } |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force }
$dataDir = Join-Path $OutputDir "data"
if (Test-Path $dataDir) {
    Get-ChildItem -LiteralPath $dataDir -Force -ErrorAction SilentlyContinue |
        Remove-Item -Recurse -Force
} else {
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
}

# Drop jlink staging leftovers under backend/target (not part of the student layout).
foreach ($staging in @(
        (Join-Path $backendTarget "desktop-jlink-extract"),
        (Join-Path $backendTarget "backend-desktop-for-jdeps.zip")
    )) {
    if (Test-Path $staging) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
}

New-Item -ItemType Directory -Force -Path $backendTarget | Out-Null
if (Test-Path $webDownloadZip) {
    Remove-Item $webDownloadZip -Force
}
Write-Host "Creating web download zip at $webDownloadZip ..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $OutputDir,
    $webDownloadZip,
    [System.IO.Compression.CompressionLevel]::Optimal,
    $false)

$backendMb = Get-FileSizeMb (Join-Path $OutputDir "backend.jar")
$workerMb = Get-FileSizeMb (Join-Path $OutputDir "worker.jar")
$runtimeMb = Get-DirectorySizeMb $runtimeJdk
$uiMb = Get-DirectorySizeMb (Join-Path $OutputDir "ui")
$exeMb = Get-FileSizeMb (Join-Path $OutputDir "OOP-AutoGrader-Practice.exe")
# Approximate launcher footprint: everything except jars/ui/runtime/rubric/data/readme/bat
$launcherMb = Get-DirectorySizeMb $OutputDir
$known = $backendMb + $workerMb + $runtimeMb + $uiMb
$otherMb = [math]::Max(0, [math]::Round($launcherMb - $known, 1))
$zipMb = Get-FileSizeMb $webDownloadZip

Write-Host ""
Write-Host "=== Practice folder size breakdown (MB) ==="
Write-Host ("  backend.jar:                 {0}" -f $backendMb)
Write-Host ("  worker.jar:                  {0}" -f $workerMb)
Write-Host ("  runtime/jdk:                 {0}" -f $runtimeMb)
Write-Host ("  ui/:                         {0}" -f $uiMb)
Write-Host ("  OOP-AutoGrader-Practice.exe: {0}" -f $exeMb)
Write-Host ("  host + other (approx):       {0}" -f $otherMb)
Write-Host ("  TOTAL extracted (approx):   {0}" -f $launcherMb)
Write-Host ("  zip compressed:              {0}" -f $zipMb)
Write-Host ("  EXE present:                 {0}" -f (Test-Path (Join-Path $OutputDir "OOP-AutoGrader-Practice.exe")))
Write-Host ("  baseline (self-contained .NET era): ~281 MB extracted")
Write-Host "=========================================="
Write-Host ""
Write-Host "Assembled student desktop layout at $OutputDir"
Write-Host "Web download zip: $webDownloadZip"
Write-Host "Students: run OOP-AutoGrader-Practice.exe (preferred) or .bat"
Write-Host "Offline: bundled runtime\jdk + thin Rust WebView2 host; WebView2 Evergreen required on Windows (no .NET runtime)."
Write-Host "Web download has no .agpack; lecturers distribute packs separately."
