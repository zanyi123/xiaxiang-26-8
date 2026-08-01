# ============================================================
# 薪火侨乡 - 本地打包+上传 一键脚本
# 用法: powershell -ExecutionPolicy Bypass -File deploy-local.ps1
# ============================================================

param(
    [string]$ServerHost = "your-server-ip",
    [string]$ServerUser = "root",
    [string]$ServerPath = "/opt/xiaxiang",
    [string]$SshKey = ""
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JarName = "xiaxiang-building-tour-1.0-SNAPSHOT.jar"
$LocalTarget = Join-Path $ProjectDir "target"
$LocalJar = Join-Path $LocalTarget $JarName
$LocalYml = Join-Path $LocalTarget "application.yml"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  薪火侨乡 本地打包+上传脚本" -ForegroundColor Cyan
Write-Host "  服务器: $ServerUser@$ServerHost" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ---------- Step 1: Maven 打包 ----------
Write-Host "[1/4] Maven 打包 (clean package -DskipTests)..." -ForegroundColor Yellow
Push-Location $ProjectDir
try {
    & mvn clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "错误: Maven 打包失败" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Write-Host "  打包成功" -ForegroundColor Green
} finally {
    Pop-Location
}

# ---------- Step 2: 检查产物 ----------
Write-Host "[2/4] 检查打包产物..." -ForegroundColor Yellow
if (-not (Test-Path $LocalJar)) {
    Write-Host "错误: 找不到 $JarName" -ForegroundColor Red
    exit 1
}
$jarSize = [math]::Round((Get-Item $LocalJar).Length / 1MB, 2)
Write-Host "  JAR: $JarName ($jarSize MB)" -ForegroundColor Green

# 检查 yml 是否被自动拷贝到 target 根
if (Test-Path $LocalYml) {
    Write-Host "  YML: application.yml (已自动拷贝到 target/ 根目录)" -ForegroundColor Green
} else {
    Write-Host "  警告: application.yml 不在 target/ 根目录，手动拷贝..." -ForegroundColor Yellow
    Copy-Item (Join-Path $ProjectDir "src\main\resources\application.yml") $LocalYml -Force
}

# ---------- Step 3: 上传到服务器 ----------
Write-Host "[3/4] 上传到服务器 $ServerHost ..." -ForegroundColor Yellow

$sshArgs = @()
if ($SshKey -ne "") {
    $sshArgs += "-i"
    $sshArgs += $SshKey
}

# 创建远程目录
$mkdirCmd = "mkdir -p $ServerPath"
& ssh @sshArgs "$ServerUser@$ServerHost" $mkdirCmd

# 上传 jar
Write-Host "  上传 $JarName ..." -ForegroundColor Gray
$scpJarArgs = @()
if ($SshKey -ne "") { $scpJarArgs += "-i"; $scpJarArgs += $SshKey }
$scpJarArgs += $LocalJar
$scpJarArgs += "$ServerUser@${ServerHost}:$ServerPath/"
& scp @scpJarArgs

# 上传 application.yml
Write-Host "  上传 application.yml ..." -ForegroundColor Gray
$scpYmlArgs = @()
if ($SshKey -ne "") { $scpYmlArgs += "-i"; $scpYmlArgs += $SshKey }
$scpYmlArgs += $LocalYml
$scpYmlArgs += "$ServerUser@${ServerHost}:$ServerPath/"
& scp @scpYmlArgs

Write-Host "  上传完成" -ForegroundColor Green

# ---------- Step 4: 远程重启 ----------
Write-Host "[4/4] 远程重启服务..." -ForegroundColor Yellow
$restartCmd = @"
cd $ServerPath
# 停旧进程
if [ -f app.pid ]; then
    kill $(cat app.pid) 2>/dev/null
    sleep 2
fi
# 启动新进程
nohup java -jar $JarName > app.log 2>&1 &
echo \$! > app.pid
sleep 3
# 验证
if kill -0 \$(cat app.pid) 2>/dev/null; then
    echo '启动成功, PID='\$(cat app.pid)
    tail -5 app.log
else
    echo '启动失败! 日志:'
    tail -20 app.log
fi
"@

& ssh @sshArgs "$ServerUser@$ServerHost" $restartCmd

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  部署完成!" -ForegroundColor Green
Write-Host "  访问: http://$ServerHost:8080/" -ForegroundColor White
Write-Host "  后台: http://$ServerHost:8080/admin/login" -ForegroundColor White
Write-Host "  日志: ssh $ServerUser@$ServerHost 'tail -f $ServerPath/app.log'" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
