# TaCZ mod downloader script for PowerShell
$url = "https://www.curseforge.com/api/v1/mods/682728/files/6069349/download"
$output = "compile-mods-1.19.2\tacz-1.19.2-1.1.4-hotfix-release.jar"

Write-Host "Downloading TaCZ mod..."
Write-Host "URL: $url"
Write-Host "Output: $output"

try {
    # Create directory if it doesn't exist
    if (!(Test-Path "compile-mods-1.19.2")) {
        New-Item -ItemType Directory -Path "compile-mods-1.19.2"
    }
    
    # Download with browser user agent
    $headers = @{
        "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    Invoke-WebRequest -Uri $url -OutFile $output -Headers $headers -UseBasicParsing
    
    Write-Host "Download complete!" -ForegroundColor Green
    Write-Host "File saved to: $output"
} catch {
    Write-Host "Download failed. Please download manually from:" -ForegroundColor Red
    Write-Host "https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files/6069349" -ForegroundColor Yellow
    Write-Host "Save the file as: compile-mods-1.19.2\tacz-1.19.2-1.1.4-hotfix-release.jar" -ForegroundColor Yellow
}