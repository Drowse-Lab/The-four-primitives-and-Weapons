# TaCZ regular version downloader (not -all version)
Write-Host "Downloading TaCZ regular version (not -all)..." -ForegroundColor Cyan

$url = "https://www.curseforge.com/api/v1/mods/682728/files/6069349/download"
$output = "compile-mods-1.19.2\tacz-1.1.4.jar"

Write-Host "URL: $url" -ForegroundColor Yellow
Write-Host "Output: $output" -ForegroundColor Yellow

try {
    if (!(Test-Path "compile-mods-1.19.2")) {
        New-Item -ItemType Directory -Path "compile-mods-1.19.2"
    }
    
    $headers = @{
        "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    Write-Host "Downloading regular TaCZ version..." -ForegroundColor Green
    Invoke-WebRequest -Uri $url -OutFile $output -Headers $headers -UseBasicParsing
    
    $fileInfo = Get-Item $output
    Write-Host "Download complete!" -ForegroundColor Green
    Write-Host "File size: $($fileInfo.Length) bytes" -ForegroundColor White
    Write-Host "File saved to: $output" -ForegroundColor White
    
    # Check file size to confirm it's not the -all version
    if ($fileInfo.Length -lt 50MB) {
        Write-Host "SUCCESS: Regular version downloaded (small size)" -ForegroundColor Green
    } else {
        Write-Host "WARNING: File size is large, might be -all version" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Download failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Please download manually from:" -ForegroundColor Yellow
    Write-Host "https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files/6069349" -ForegroundColor Yellow
    Write-Host "Save as: compile-mods-1.19.2\tacz-1.1.4.jar (NOT the -all version)" -ForegroundColor Yellow
}