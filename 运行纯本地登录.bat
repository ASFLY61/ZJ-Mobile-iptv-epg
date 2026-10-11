@echo off
chcp 65001 >nul
title Full Local Login
cd /d "%~dp0"
if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" full_local_login.py %*
) else (
    python full_local_login.py %*
)
echo.
pause