param(
    [switch]$NoDeploy
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "========== AduAja - Fly.io Deploy ==========" -ForegroundColor Cyan

# 1. Cek flyctl
$flyctl = Get-Command flyctl -ErrorAction SilentlyContinue
if (-not $flyctl) {
    $flyctl = Get-Command "$env:USERPROFILE\.fly\bin\flyctl.exe" -ErrorAction SilentlyContinue
}
if (-not $flyctl) {
    Write-Host "[ERROR] flyctl tidak ditemukan. Install dulu:" -ForegroundColor Red
    Write-Host "  powershell -NoProfile -ExecutionPolicy Bypass -Command ""iwr https://fly.io/install.ps1 -UseBasicParsing | iex""" -ForegroundColor Yellow
    exit 1
}
$flyPath = $flyctl.Source
Write-Host "[OK] flyctl: $((Get-Item $flyPath).VersionInfo.ProductVersion)" -ForegroundColor Green

# 2. Cek login
$whoami = & $flyPath auth whoami 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ACTION] Belum login. Buka browser untuk login..." -ForegroundColor Yellow
    & $flyPath auth login
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Login gagal." -ForegroundColor Red
        exit 1
    }
}
Write-Host "[OK] Login: $(& $flyPath auth whoami)" -ForegroundColor Green

# 3. Cek app sudah ada atau belum
$appList = & $flyPath apps list 2>&1
$appExists = $appList -match "aduaja"

if (-not $appExists) {
    Write-Host "[INFO] Membuat app aduaja..." -ForegroundColor Yellow
    & $flyPath apps create aduaja
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Gagal buat app." -ForegroundColor Red
        exit 1
    }
}
Write-Host "[OK] App aduaja siap" -ForegroundColor Green

# 4. Cek volume
$volList = & $flyPath volumes list -a aduaja 2>&1
$volExists = $volList -match "aduaja_data"
if (-not $volExists) {
    Write-Host "[INFO] Membuat volume aduaja_data (1GB) di region sjc..." -ForegroundColor Yellow
    & $flyPath volumes create aduaja_data --app aduaja --region sjc --size 1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Gagal buat volume." -ForegroundColor Red
        exit 1
    }
}
Write-Host "[OK] Volume aduaja_data siap" -ForegroundColor Green

# 5. Set secrets dari .env
Write-Host "[INFO] Set secrets dari .env..." -ForegroundColor Yellow
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.+)$') {
            $key = $matches[1].Trim()
            $val = $matches[2].Trim()
            if ($key -ne "" -and $key -notlike '#*') {
                & $flyPath secrets set --app aduaja "$key=$val" 2>&1 | Out-Null
            }
        }
    }
    Write-Host "[OK] Secrets ter-set" -ForegroundColor Green
} else {
    Write-Host "[WARN] .env tidak ditemukan. Set secrets manual!" -ForegroundColor Yellow
}

# 6. Deploy
if (-not $NoDeploy) {
    Write-Host "[INFO] Deploy ke Fly.io (build remote)..." -ForegroundColor Yellow
    Write-Host "      Ini akan memakan waktu 5-10 menit." -ForegroundColor DarkYellow
    & $flyPath deploy --remote-only --app aduaja
    if ($LASTEXITCODE -eq 0) {
        Write-Host "" -ForegroundColor Green
        Write-Host "========== DEPLOY BERHASIL! ==========" -ForegroundColor Green
        Write-Host "Akses: https://aduaja.fly.dev" -ForegroundColor Cyan
        Write-Host "Logs: fly logs -a aduaja" -ForegroundColor Gray
        Write-Host "======================================" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Deploy gagal. Cek logs: fly logs -a aduaja" -ForegroundColor Red
    }
} else {
    Write-Host "[INFO] Siap deploy. Jalankan:" -ForegroundColor Yellow
    Write-Host "  fly deploy --remote-only --app aduaja" -ForegroundColor Cyan
}
