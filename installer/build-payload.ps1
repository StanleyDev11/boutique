# =====================================================================
#  build-payload.ps1
#  Prepare le dossier  installer\payload\  consomme par boutique.iss.
#
#  Ce script :
#    1. (re)construit le fat jar Spring Boot (mvnw clean package),
#    2. genere un runtime Java autonome via jlink (avec java.exe/javaw.exe),
#    3. copie le runtime + le jar + icon.ico dans installer\payload\.
#
#  IMPORTANT : on utilise jlink (et NON le "runtime" de jpackage), car le
#  runtime jpackage ne contient PAS javaw.exe ; les raccourcis Inno Setup
#  lancent l'app via javaw.exe -> on evite le bug "Failed to launch JVM".
#
#  Prerequis : JDK 21 installe. Adapte $JavaHome si besoin.
#
#  Usage (depuis n'importe quel dossier) :
#      powershell -ExecutionPolicy Bypass -File installer\build-payload.ps1
#  Option : -SkipBuild pour reutiliser le jar deja construit.
# =====================================================================
param(
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

# Racine du projet = dossier parent de ce script (installer\..)
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$PayloadDir  = Join-Path $ProjectRoot 'installer\payload'
$JarName     = 'boutique-1.0.0-SNAPSHOT.jar'
$JarPath     = Join-Path $ProjectRoot "target\$JarName"

Write-Host "== Projet   : $ProjectRoot"
Write-Host "== JavaHome : $JavaHome"

$env:JAVA_HOME = $JavaHome
Set-Location $ProjectRoot

# --- 1. Build du fat jar --------------------------------------------
if (-not $SkipBuild) {
    Write-Host "== [1/3] Build du fat jar (mvnw clean package -DskipTests)..."
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Echec du build Maven (code $LASTEXITCODE)" }
} else {
    Write-Host "== [1/3] Build ignore (-SkipBuild)."
}
if (-not (Test-Path $JarPath)) { throw "Jar introuvable : $JarPath" }

# --- 2. Preparation du dossier payload + runtime jlink --------------
Write-Host "== [2/3] Generation du runtime Java (jlink)..."
Remove-Item -Recurse -Force $PayloadDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $PayloadDir | Out-Null

$RuntimeDir = Join-Path $PayloadDir 'runtime'
& "$JavaHome\bin\jlink.exe" --add-modules ALL-MODULE-PATH `
    --strip-debug --no-header-files --no-man-pages `
    --output $RuntimeDir
if ($LASTEXITCODE -ne 0) { throw "Echec de jlink (code $LASTEXITCODE)" }
if (-not (Test-Path (Join-Path $RuntimeDir 'bin\javaw.exe'))) {
    throw "javaw.exe absent du runtime genere."
}

# --- 3. Copie du jar et de l'icone ----------------------------------
Write-Host "== [3/3] Copie du jar et de l'icone dans le payload..."
Copy-Item $JarPath (Join-Path $PayloadDir $JarName) -Force
Copy-Item (Join-Path $ProjectRoot 'icon.ico') (Join-Path $PayloadDir 'icon.ico') -Force

Write-Host ""
Write-Host "== Payload pret : $PayloadDir"
Get-ChildItem $PayloadDir | Select-Object Name, Length | Format-Table -AutoSize
Write-Host "== Ouvrez maintenant installer\boutique.iss dans Inno Setup puis Build > Compile."
