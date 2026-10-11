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
    "$PY" -m venv .venv || {
        echo "[!] 创建 venv 失败, 请安装 venv 模块后重试:"
        echo "      Debian/Ubuntu:      sudo apt install python3-venv python3-pip"
        echo "      RHEL/CentOS/阿里云: sudo yum install python3-pip python3-venv"
        exit 1
    }
fi

# 2) 确保 venv 里有 pip (部分系统的 venv 不带 pip)
if ! .venv/bin/python -m pip --version >/dev/null 2>&1; then
    echo "[*] venv 缺少 pip, 尝试初始化 ..."
    .venv/bin/python -m ensurepip --upgrade >/dev/null 2>&1 || {
        echo "[!] venv 无法初始化 pip (ensurepip 缺失)。请安装后删除 .venv 重跑:"
        echo "      Debian/Ubuntu:      sudo apt install python3-venv python3-pip && rm -rf .venv"
        echo "      RHEL/CentOS/阿里云: sudo yum install python3-pip python3-venv && rm -rf .venv"
        exit 1
    }
fi

# 3) 依赖 (缺哪个装哪个; pycryptodome 解密码 / openpyxl 导 Excel, 均可选)
need=0
.venv/bin/python -c "import Cryptodome" 2>/dev/null || need=1
.venv/bin/python -c "import openpyxl"   2>/dev/null || need=1
if [ "$need" -eq 1 ]; then
    echo "[*] 安装依赖 pycryptodome + openpyxl ..."
    .venv/bin/python -m pip install --quiet --upgrade pip
    .venv/bin/python -m pip install --quiet pycryptodome openpyxl
fi

# 4) 运行
exec .venv/bin/python full_local_login.py "$@"
