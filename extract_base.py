#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
extract_base.py — 从抓包文件自动提取 BASE (EDS 入口) 和 VSP_HOST

支持格式: HAR(.har) / PCAP(.pcap, .pcapng) / 代理文本日志(Charles/Fiddler/HTTP Toolkit/mitmproxy 导出)
          / 任何包含 URL 的文件。统一按文本扫描, 无需额外依赖。

原理:
  - BASE    = 认证入口(EDS)。抓包里所有 /EDS/ 路径请求的 host:port 就是它
              (盒子登录第一步 POST /EDS/STBRegister)。
  - VSP_HOST = 业务服务器。/EPG/XML/ 或 /VSP/ 路径的 URL(含 307 重定向 Location)
              的 host 就是它。

用法:
    python extract_base.py <抓包文件>
    python extract_base.py capture.har
    python extract_base.py capture.pcap
    python extract_base.py proxy.log
"""
import re
import sys
import os
import collections

# http(s)://host[:port]/path  (path 在空白/引号/括号/尖括号处截断)
URL_RE = re.compile(r'https?://([A-Za-z0-9.\-]+)(?::(\d+))?(/[^\s"\'\\<>\)\]\}]*)')
# 原始 pcap 里的 HTTP 头:  Host: host[:port]
HOST_HDR_RE = re.compile(r'(?im)^[ \t]*host:[ \t]*([A-Za-z0-9.\-]+)(?::(\d+))?')
# 原始 pcap 里的请求行:  POST /EDS/... HTTP/1.1
REQ_EDS_RE = re.compile(r'(?m)^[A-Z]+ +(/EDS/[^\s]*) +HTTP')


def scan(text):
    """扫描文本, 返回 (eds 计数, vsp 计数, eds 示例, vsp 示例)。"""
    eds = collections.Counter()
    vsp = collections.Counter()
    eds_ex, vsp_ex = [], []
    for host, port, path in URL_RE.findall(text):
        p = path or ""
        port = port or ""
        if "/EDS/" in p:
            eds[(host, port)] += 1
            if len(eds_ex) < 4:
                eds_ex.append("%s:%s%s" % (host, port, p))
        elif "/EPG/XML/" in p or "/VSP/" in p:
            vsp[host] += 1
            if len(vsp_ex) < 4:
                vsp_ex.append("%s:%s%s" % (host, port, p))
    return eds, vsp, eds_ex, vsp_ex


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    path = sys.argv[1]
    if not os.path.exists(path):
        print("[!] 找不到文件: %s" % path)
        sys.exit(1)
    with open(path, "rb") as f:
        raw = f.read()
    text = raw.decode("utf-8", "ignore")
    low = path.lower()
    fmt = "HAR" if low.endswith(".har") else (
        "PCAP" if low.endswith((".pcap", ".pcapng")) else "文本/其他")
    print("[*] 扫描 %s (%.0f KB, 按 %s 文本解析)" % (os.path.basename(path), len(raw) / 1024, fmt))

    eds, vsp, eds_ex, vsp_ex = scan(text)

    base = None
    if eds:
        (host, port), cnt = eds.most_common(1)[0]
        base = "http://%s:%s" % (host, port) if port else "http://%s" % host
        print("\n[✓] 找到 EDS 入口 (%d 个 /EDS/ 请求):" % cnt)
        print('    BASE = "%s"' % base)
        for e in eds_ex:
            print("        例: %s" % e)
    else:
        # 原始 pcap 兜底: Host 头 + /EDS/ 请求行
        host_hdrs = collections.Counter(HOST_HDR_RE.findall(text))
        if host_hdrs and REQ_EDS_RE.search(text):
            (host, port), cnt = host_hdrs.most_common(1)[0]
            base = "http://%s:%s" % (host, port) if port else "http://%s" % host
            print("\n[✓] (原始 pcap 兜底) 由 Host 头 + /EDS/ 请求行推断:")
            print('    BASE = "%s"' % base)
            print("        注意: 若抓包含多个 Host, 请核对哪个对应 /EDS/ 请求")
        else:
            print("\n[!] 未找到 /EDS/ 请求 —— 该抓包可能不含认证流量, 或格式无法按文本解析。")
            print("    请确认抓到了盒子登录时的 STBRegister 请求。")

    vsp_host = None
    if vsp:
        vsp_host, cnt = vsp.most_common(1)[0]
        print("\n[✓] 发现 VSP 服务器 (%d 个 /EPG//VSP/ 请求, 含 307 重定向):" % cnt)
        print('    VSP_HOST = "%s"' % vsp_host)
        for e in vsp_ex:
            print("        例: %s" % e)

    # 可直接粘贴的片段
    print("\n---- 可直接粘贴到 full_local_login.py ----")
    if base:
        print('BASE = "%s"' % base)
    if vsp_host:
        print('VSP_HOST = "%s"   # 可选: 留占位符也能自动推导' % vsp_host)
    elif base:
        print("# VSP_HOST 留占位符即可, 程序会从 307 重定向自动推导")
    if not base:
        print("# (未提取到 BASE, 请手动填写)")


if __name__ == "__main__":
    main()
