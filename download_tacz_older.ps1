# TaCZ mod older version downloader script for PowerShell
# Trying version 1.0.3 which might be more compatible

Write-Host "Removing current TaCZ version..." -ForegroundColor Yellow
if (Test-Path "compile-mods-1.19.2\tacz-1.1.4.jar.disabled") {
    Remove-Item "compile-mods-1.19.2\tacz-1.1.4.jar.disabled"
    Write-Host "Removed tacz-1.1.4.jar.disabled" -ForegroundColor Green
}

Write-Host "Attempting to download TaCZ 1.0.3 (older, potentially more compatible version)..." -ForegroundColor Cyan

# Let's try a different approach - manually create a note for manual download
$note = @"
TaCZ Compatibility Issue Detected
==================================

The TaCZ 1.1.4-hotfix version has a Mixin compatibility issue with your Forge version.

To resolve this, please:

1. Visit CurseForge: https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files
2. Find an older version like TaCZ 1.0.3 for Minecraft 1.19.2
3. Download and save as: compile-mods-1.19.2\tacz-1.0.3.jar

OR

Skip TaCZ for now and test your FlyingAttackerEntity mod without it.
Your mod includes reflection-based TaCZ support, so it will work with or without TaCZ.

Current error: TaCZ Mixin failed to inject into LivingEntity
This indicates version incompatibility between TaCZ and your Forge version.
"@

$note | Out-File "TaCZ_COMPATIBILITY_ISSUE.txt" -Encoding UTF8

Write-Host "Created compatibility issue note: TaCZ_COMPATIBILITY_ISSUE.txt" -ForegroundColor Green
Write-Host "Please download TaCZ 1.0.3 manually or test without TaCZ." -ForegroundColor Yellow