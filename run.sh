#!/usr/bin/env bash
# Linux 一键启动: 自动建 venv + 装依赖 + 运行
# 用法: ./run.sh [频道ID] [日期] [参数...]   (参数同 python full_local_login.py)
#   ./run.sh                          默认: 中央一套 + 今天(回看)
#   ./run.sh 42329858 2026-09-19      频道 + 日期
#   ./run.sh all 2026-09-19           全部频道
set -e
cd "$(dirname "$0")"

PY=python3
command -v "$PY" >/dev/null 2>&1 || { echo "[!] 未找到 python3, 请先安装 Python 3.8+"; exit 1; }

# 1) 虚拟环境 (若不存在)
if [ ! -x .venv/bin/python ]; then
    echo "[*] 首次运行: 创建虚拟环境 .venv ..."
    if ! "$PY" -m venv .venv; then
        echo "[!] 创建 venv 失败, 请安装 venv 模块后重试:"
        echo "      Debian/Ubuntu: sudo apt install python3-venv"
        echo "      Fedora/RHEL  : sudo dnf install python3-venv"
        exit 1
    fi
fi

# 2) 依赖 (缺哪个装哪个; pycryptodome 解密码 / openpyxl 导 Excel, 均可选)
need=0
.venv/bin/python -c "import Cryptodome" 2>/dev/null || need=1
.venv/bin/python -c "import openpyxl"   2>/dev/null || need=1
if [ "$need" -eq 1 ]; then
    echo "[*] 安装依赖 pycryptodome + openpyxl ..."
    .venv/bin/pip install --quiet --upgrade pip
    .venv/bin/pip install --quiet pycryptodome openpyxl
fi

# 3) 运行
exec .venv/bin/python full_local_login.py "$@"
