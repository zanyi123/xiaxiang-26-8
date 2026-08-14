@echo off
chcp 65001 >nul
title 宝源坊项目 - 一键停止
color 0C

echo.
echo ╔══════════════════════════════════════════╗
echo ║     宝源坊项目 - 一键停止服务           ║
echo ╚══════════════════════════════════════════╝
echo.

echo [1/2] 查找占用端口8080的进程...
set "found=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    set "found=1"
    echo     发现进程 PID: %%a
    echo     正在终止...
    taskkill /PID %%a /F >nul 2>&1
    if errorlevel 1 (
        echo     ✓ 进程 %%a 已终止
    ) else (
        echo     ✓ 进程 %%a 已终止
    )
)

if "%found%"=="0" (
    echo     端口8080未被占用，没有运行中的服务
)

echo.
echo [2/2] 确认服务状态...
timeout /t 1 /nobreak >nul

:: 再次检查
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    echo     ⚠️ 端口8080仍被占用 (PID: %%a)，尝试强制结束...
    taskkill /F /PID %%a >nul 2>&1
    wmic process where "ProcessId=%%a" call terminate >nul 2>&1
)

echo.
echo ✓ 清理完成！
timeout /t 2 /nobreak >nul
