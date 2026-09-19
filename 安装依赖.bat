@echo off
chcp 65001 >nul
title 安装依赖 (纯本地方案)
cd /d "%~dp0"
echo ============================================================
echo   安装依赖: 在本目录创建 .venv 虚拟环境并安装
echo   pycryptodome (解密码) + openpyxl (导出 Excel)
echo ============================================================
echo.

rem --- 1. 找 python ---
where python >nul 2>nul
if errorlevel 1 (
    echo [!] 未找到 python。
    echo     请先安装 Python 3.8+: https://www.python.org/downloads/
    echo     安装时务必勾选 "Add python.exe to PATH"。
    echo     装完重新打开本窗口再运行本脚本。
    echo.
    pause
    exit /b 1
)
echo [✓] 找到 python:
python --version

rem --- 2. 建虚拟环境 (仅首次) ---
if not exist ".venv\Scripts\python.exe" (
    echo.
    echo [*] 首次运行, 创建虚拟环境 .venv ...
    python -m venv .venv
    if errorlevel 1 (
        echo [!] 创建 .venv 失败。
        pause
        exit /b 1
    )
) else (
    echo [✓] 虚拟环境 .venv 已存在, 跳过创建。
)

rem --- 3. 升级 pip 并装依赖 ---
echo.
echo [*] 安装依赖 (pycryptodome, openpyxl) ...
".venv\Scripts\python.exe" -m pip install --upgrade pip >nul
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if errorlevel 1 (
    echo.
    echo [!] 依赖安装失败。若网络受限, 可手动:
    echo     .venv\Scripts\python.exe -m pip install -r requirements.txt
    pause
    exit /b 1
)

echo.
echo ============================================================
echo   [✓] 依赖安装完成。
echo   现在双击 "运行纯本地登录.bat" 即可。
echo ============================================================
pause
