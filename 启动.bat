@echo off
chcp 65001 >nul
title 宝源坊项目 - 一键启动
color 0A

echo.
echo ╔══════════════════════════════════════════╗
echo ║     宝源坊项目 - 一键启动脚本           ║
echo ╚══════════════════════════════════════════╝
echo.

:: ============ 第1步：清理端口占用 ============
echo [1/3] 检查并清理端口占用...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    echo     检测到端口8080被占用 (PID: %%a)，正在释放...
    taskkill /PID %%a /F >nul 2>&1
    timeout /t 1 /nobreak >nul
)
echo     ✓ 端口已就绪

:: ============ 第2步：清理缓存 ============
echo [2/3] 清理编译缓存...
if exist "target" (
    rmdir /s /q target >nul 2>&1
    echo     ✓ 已清理target目录
) else (
    echo     - target目录不存在，跳过
)

:: ============ 第3步：启动项目 ============
echo [3/3] 启动项目...
echo.
echo ────────────────────────────────────────
echo   项目启动中，请稍候...
echo   启动成功后访问: http://localhost:8080
echo   按 Ctrl+C 可停止服务
echo ────────────────────────────────────────
echo.

:: 切换到项目目录并启动
cd /d d:\JAVA\xiaxiang
call D:\Maven\apache-maven-3.9.14\bin\mvn.cmd spring-boot:run -DskipTests

pause
