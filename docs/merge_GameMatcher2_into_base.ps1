# Merge C:\GameMatcher (2) into GameMatcher1_base (base wins on path conflicts).
# Run from repo root: powershell -ExecutionPolicy Bypass -File docs\merge_GameMatcher2_into_base.ps1
$ErrorActionPreference = 'Stop'
$BaseRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $BaseRoot 'pom.xml'))) { $BaseRoot = 'c:\GameMatcher1_base' }
$Gm2 = 'C:\GameMatcher (2)'
$LogDir = Join-Path $BaseRoot 'docs\import_GameMatcher_2'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$log = @()
$log += "Base: $BaseRoot"
$log += "Source (GameMatcher 2): $Gm2"
$log += "Started: $(Get-Date -Format o)"

if (-not (Test-Path $Gm2)) { throw "Not found: $Gm2" }

# 1) Docs + root planning files from GM2
$destDocs = Join-Path $LogDir 'original_docs_from_GameMatcher2'
if (Test-Path (Join-Path $Gm2 'docs')) {
    robocopy (Join-Path $Gm2 'docs') $destDocs /E /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
    $log += "Robocopy docs -> $destDocs"
}
$rootDest = Join-Path $LogDir 'root_files_from_GameMatcher2'
New-Item -ItemType Directory -Force -Path $rootDest | Out-Null
Get-ChildItem -LiteralPath $Gm2 -File | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $rootDest $_.Name) -Force
}
$log += "Copied GM2 root files -> $rootDest"

Copy-Item -LiteralPath (Join-Path $Gm2 'pom.xml') -Destination (Join-Path $LogDir 'pom_GameMatcher2_reference.xml') -Force
$log += "Saved pom_GameMatcher2_reference.xml"

# 2) Frontend (separate folder; base keeps existing frontend/)
$gf = Join-Path $BaseRoot 'gamematcher-frontend'
if (Test-Path $gf) {
    $log += "SKIP gamematcher-frontend (already exists at $gf)"
} else {
    Copy-Item -LiteralPath (Join-Path $Gm2 'gamematcher-frontend') -Destination $gf -Recurse -Force
    $log += "Copied gamematcher-frontend -> $gf"
}

# 3) src: copy only paths that do not exist under base (base wins on 23 overlaps)
$baseSrc = Join-Path $BaseRoot 'src'
$gm2Src = Join-Path $Gm2 'src'
$baseFiles = @{}
Get-ChildItem -Path $baseSrc -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($baseSrc.Length + 1).Replace('\', '/')
    $baseFiles[$rel] = $true
}
$added = 0
Get-ChildItem -Path $gm2Src -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($gm2Src.Length + 1).Replace('\', '/')
    if (-not $baseFiles.ContainsKey($rel)) {
        $target = Join-Path $baseSrc ($rel -replace '/', '\')
        $dir = Split-Path $target -Parent
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        Copy-Item -LiteralPath $_.FullName -Destination $target -Force
        $added++
    }
}
$log += "src files added from GM2 (non-conflicting paths only): $added"

$log += "Finished: $(Get-Date -Format o)"
$log | Set-Content -Path (Join-Path $LogDir 'merge_log.txt') -Encoding UTF8
Write-Host ($log -join "`n")
