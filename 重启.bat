@echo off
chcp 65001 >nul
title 宝源坊项目 - 一键重启
color 0B

echo.
echo ╔══════════════════════════════════════════╗
echo ║     宝源坊项目 - 一键重启服务           ║
echo ╚══════════════════════════════════════════╝
echo.

:: ============ 第1步：停止现有服务 ============
echo [1/4] 停止现有服务...
set "found=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    set "found=1"
    echo     终止进程 PID: %%a
    taskkill /PID %%a /F >nul 2>&1
)
if "%found%"=="0" echo     无运行中的服务
timeout /t 2 /nobreak >nul

:: ============ 第2步：清理缓存 ============
echo [2/4] 清理编译缓存...
if exist "target" (
    rmdir /s /q target >nul 2>&1
    echo     ✓ 已清理target目录
) else (
    echo     - 无需清理
)

:: ============ 第3步：编译项目 ============
echo [3/4] 编译项目...
echo     (首次启动或代码有改动时需要等待较久)
cd /d d:\JAVA\xiaxiang
call D:\Maven\apache-maven-3.9.14\bin\mvn.cmd compile -q
if errorlevel 1 (
    echo     ⚠️ 编译失败，请检查代码！
    pause
    exit /b 1
)
echo     ✓ 编译完成

:: ============ 第4步：启动服务 ============
echo [4/4] 启动服务...
echo.
echo ────────────────────────────────────────
echo   ✓ 启动成功！
echo   访问: http://localhost:8080
echo   按 Ctrl+C 可停止服务
echo ────────────────────────────────────────
echo.

call D:\Maven\apache-maven-3.9.14\bin\mvn.cmd spring-boot:run -DskipTests

pause
