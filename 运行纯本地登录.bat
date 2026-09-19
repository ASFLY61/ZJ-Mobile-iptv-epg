@echo off
chcp 65001 >nul
title 纯本地登录 (不依赖 Frida)
cd /d "%~dp0"
echo ============================================================
echo   纯本地登录 (只需 STBID, 无需 EPG/Frida/抓包)
echo   用法: 双击运行(默认中央一套+今天, 回看模式)
echo         或带参数: 运行纯本地登录.bat 7200 42329207 2026-09-19
echo                  运行纯本地登录.bat 42329858 week   (今天+过去6天)
echo ============================================================
echo.

rem 优先用本目录 .venv (先运行过 安装依赖.bat), 否则退回系统 python
if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" full_local_login.py %*
) else (
    if exist "full_local_login.py" (
        python full_local_login.py %*
    ) else (
        echo [!] 未找到 full_local_login.py
        pause
        exit /b 1
    )
)
echo.
echo ============================================================
pause
