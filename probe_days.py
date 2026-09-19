#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""探测 QueryPlaybillListStcProps 的日期范围(默认回看 type=2):
- 今天是否全天
- 过去能回看几天(盒子=6天)
- live 模式(type=1)对比
用法: python probe_days.py [频道ID]
"""
import os, sys, json
from datetime import datetime, timedelta
import importlib.util

_HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("fll", os.path.join(_HERE, "full_local_login.py"))
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

CST = m.CST
CHANNEL = sys.argv[1] if len(sys.argv) > 1 else "42329858"


def login():
    usertoken = final_port = None
    reg_final = None
    for _ in range(6):
        ok, userid, password, reg_final = m.stb_register()
        if not ok:
            continue
        ch = m.get_challenge()
        usertoken, code, final_port = m.digest_auth(ch, userid, password)
        if usertoken:
            break
    if not usertoken:
        sys.exit("[!] 登录失败")
    # 若 VSP_HOST 仍是占位符, 从 307 重定向自动推导
    if m.VSP_HOST in ("", "YOUR_VSP_IP"):
        d = m._host_from_url(reg_final or "")
        if d and d != m._host_from_url(m.BASE):
            m.VSP_HOST = d
    port, jsid, new_token = m.find_working_port(usertoken, final_port or "7182")
    return port, jsid, new_token


def probe(port, jsid, token, day_str, mode):
    try:
        code, body2 = m.query_playbill(jsid, token, port, CHANNEL, day_str, mode=mode)
        data = json.loads(body2.decode("utf-8", "replace"))
        progs = m.parse_playbill(data)
        first = progs[0]["start"] if progs else "-"
        last = progs[-1]["end"] if progs else "-"
        return len(progs), first, last
    except Exception as e:
        return 0, str(e)[:20], "-"


def main():
    port, jsid, token = login()
    today = datetime.now(CST).date()
    print(f"\n[✓] 登录 port={port}  今天={today}  现在={datetime.now(CST).strftime('%H:%M')}\n")

    print("== 回看模式 (catchup, type=2) ==")
    print(f"{'偏移':>5}  {'日期':<12} {'条数':>4}  {'首条':<7} {'末条':<7}")
    print("-" * 48)
    for off in range(-8, 2):
        d = (today + timedelta(days=off)).strftime("%Y-%m-%d")
        n, f, l = probe(port, jsid, token, d, "catchup")
        label = "今天" if off == 0 else f"{off}天"
        print(f"{label:>5}  {d:<12} {n:>4}  {f:<7} {l:<7}")

    print("\n== live 模式 (type=1) 对比 ==")
    for off in (0, 1):
        d = (today + timedelta(days=off)).strftime("%Y-%m-%d")
        n, f, l = probe(port, jsid, token, d, "live")
        label = "今天" if off == 0 else f"+{off}天"
        print(f"{label:>5}  {d:<12} {n:>4}  {f:<7} {l:<7}")


if __name__ == "__main__":
    main()
