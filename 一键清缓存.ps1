# 一键清缓存+释放8080端口（复制即用）
$port = 8080
$processes = netstat -ano | findstr ":$port" | findstr "LISTENING"
if ($processes) {
    $pid = ($processes -split '\s+')[-1]
    Write-Host "发现端口 $port 被 PID:$pid 占用，正在释放..."
    taskkill /PID $pid /F
    Write-Host "✅ 端口已释放" -ForegroundColor Green
} else {
    Write-Host "✅ 端口 $port 空闲" -ForegroundColor Green
}
Write-Host ""
Write-Host "正在清理target目录..."
Remove-Item -Recurse -Force "target" -ErrorAction SilentlyContinue
Write-Host "✅ 缓存已清理" -ForegroundColor Green
Write-Host ""
Write-Host "👉 现在回IDEA重新启动项目即可"
