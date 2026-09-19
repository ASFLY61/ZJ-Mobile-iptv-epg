#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
完整本地登录 (不依赖 EPG 登录 / Frida)
======================================
只需: STBID  (设备序列号, adb shell getprop ro.serialno)
  - UserID  自动从 STBRegister 响应 <UserID> 读取
  - 密码    自动从 STBRegister 响应 <Password> 解密 (AES-128-ECB, key 见 AES_KEY)
  - VSP端口 自动探测 (7176-7200)
换盒子只改 STBID 一个值即可。

流程:
  1. STBRegister       (POST /EDS/STBRegister, 307 跟随) -> UserID + 加密Password
  2. Digest challenge  (POST /EDS/pub/authentication_initial_challenge_v1, 空body)
  3. Digest auth       (POST /EDS/pub/authentication_initial, 带 Authorization) -> UserToken
  4. ZJLogin           (POST /VSP/V3/ZJLogin, 不带任何自定义头) -> JSESSIONID + 新UserToken
                        (端口自动探测: 扫 7176-7200 找 retCode=000000000 的端口)
  5. QueryPlaybill     (POST /VSP/V3/QueryPlaybillListStcProps) -> 节目单

节目单两种模式 (QueryPlaybill.type 字段, 反编译 catchup/TVOD*Presenter):
  catchup (默认, type=2 + isFillProgram=1 + sortType=STARTTIME:ASC):
     复刻盒子"回看" —— 今天全天 + 过去6天全天; 第7天起早期缺失, 第8天起无数据
  live (type=1):
     今天(自当前节目起) + 明天; 无历史

算法 (反编译 AuthManagerImpl.java:434-471 精确还原):
  HA1 = MD5(username:realm:BASE64(password))
  HA2 = MD5("POST:/EDS/pub/authentication_initial:" + MD5(body))
  response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
  nc=00000001, cnonce=MD5(随机), body=<AuthenticationRequest><STBID>..</STBID></AuthenticationRequest>

密码解密 (DataProcessing.decryptAes, libdataprocessing.so):
  AES-128-ECB + PKCS7, key = YOUR_AES_KEY (.so 偏移 0x7004)

运行(你 PC 上, 能访问 YOUR_VSP_IP):
  python full_local_login.py [频道ID] [日期] [VSP端口hint]   # 参数顺序无关, 按类型识别
  例: python full_local_login.py 42329858 2026-09-19          # 默认回看(全天+过去6天)
  全部频道: python full_local_login.py all 2026-09-19
  live 模式(今天现在起+明天): python full_local_login.py live 42329858
  周回看(今天+过去6天, 共7天, 逐天拉): python full_local_login.py 42329858 week
"""
import os, csv, hashlib, base64, secrets, json, sys, re, urllib.request, urllib.error
from datetime import datetime, timezone, timedelta

# ===================== 使用前必须填你自己的值 =====================
# 下面是占位符, 请替换成你自己盒子/区域的值 (获取方法见 README「配置说明」)。
# 填好 STBID 后, USERNAME/PASSWORD 通常可留占位符 —— 它们会自动从 STBRegister 解出。

# [每盒必改] 机顶盒序列号: 机顶盒背面标签, 或 adb shell getprop ro.serialno
STBID = "YOUR_STBID_HERE"
# [兜底, 可留占位符] 设备密码(明文) —— 正常会自动解密 STBRegister 的 <Password> 得到;
#        仅当自动解密失败(缺 pycryptodome / 响应异常)时才用这个值
PASSWORD = "YOUR_PASSWORD_HERE"
# [兜底, 可留占位符] 订购用户 ID —— 自动从 STBRegister 的 <UserID> 读取
USERNAME = "YOUR_USERNAME_HERE"
# STBRegister <Password> 字段的 AES-128-ECB 密钥 (PKCS7 填充), 16 字节十六进制。
#   ★ 已内置默认值: 同型号/区域的所有盒子通用 (来自固件 libdataprocessing.so 偏移 0x7004)。
#   同区域盒子直接用即可, 无需修改; 仅当换了不同型号/区域的盒子才需要改。
AES_KEY = bytes.fromhex("6b4050eb4f2c6a3398665c245ec53640")
# [换区域才改] 认证入口(EDS)地址, 形如 http://<域名>:8082
#   这是整套认证的第一步入口。两种获取方式(任选其一):
#     (A) 盒子配置: EPG 应用 config.json 里的 "edsUrl" 字段
#         adb shell cat /data/data/com.pukka.ydepg/shared_prefs/*.xml  或抓包
#     (B) 抓包: 用盒子登录一次, 抓到的第一个请求就是
#         POST http://<EDS域名>:8082/EDS/STBRegister  —— 取它的 host:port
#   例: http://<你的EDS域名>:8082   (具体域名以你盒子 config.json 的 edsUrl 为准)
BASE = "http://YOUR_EDS_HOST:PORT"
# [可留占位符] VSP 业务服务器 IP —— 承载 /VSP/V3/ZJLogin、/VSP/V3/QueryPlaybill 等。
#   ★ 通常不用手填: 程序会用 BASE 发 STBRegister, 服务器 307 重定向到
#     http://<VSP_HOST>:<端口>/EPG/XML/STBRegister, 程序自动从该地址取出 VSP_HOST。
#   若要手填, 来源同 BASE:
#     (A) 抓包看 STBRegister 的 307 响应头 Location 里的 IP;
#     (B) 盒子 config.json 里对应的 VSP 地址。
VSP_HOST = "YOUR_VSP_IP"
# [换区域才改] 频道表: 默认用本目录的 channel_ids.json (已随包附带); 换区域替换该文件即可
_HERE = os.path.dirname(os.path.abspath(__file__))
CHANNELS_JSON = os.path.join(_HERE, "channel_ids.json")
# ===================== 以下通用, 不用改 =====================
CST = timezone(timedelta(hours=8))
OUT_DIR = _HERE   # 节目单输出到脚本所在目录

UA = {"User-Agent": "Huawei-EPG-APK"}


def _raw_post(url, data, headers, no_ua=False):
    h = {} if no_ua else dict(UA)
    if headers:
        h.update(headers)
    if no_ua:
        # 抑制 urllib 默认 User-Agent (盒子 ZJLogin 零头)
        h["User-Agent"] = ""
    req = urllib.request.Request(url, data=data, method="POST", headers=h)
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return r.status, dict(r.headers), r.read()
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read()


def post(url, data=b"", headers=None, no_ua=False):
    """POST, 手动跟随 307/302 重定向 (POST 保留 body), 返回 (status, headers, body, final_url)
    no_ua=True 时不注入全局 UA (ZJLogin 用)"""
    cur = url
    for _ in range(5):
        code, hdrs, body = _raw_post(cur, data, headers, no_ua)
        if code in (301, 302, 303, 307, 308):
            loc = hdrs.get("Location")
            if not loc:
                return code, hdrs, body, cur
            cur = loc
            continue
        return code, hdrs, body, cur
    return code, hdrs, body, cur


def decrypt_password(b64ct):
    """解密 STBRegister 的 <Password> 字段: AES-128-ECB + PKCS7, key=AES_KEY。
    失败返回 None (调用方回退到全局 PASSWORD)。"""
    try:
        try:
            from Crypto.Cipher import AES
        except ImportError:
            from Cryptodome.Cipher import AES
    except ImportError:
        return None
    try:
        ct = base64.b64decode(b64ct)
        pt = AES.new(AES_KEY, AES.MODE_ECB).decrypt(ct)
        pad = pt[-1]
        if not (1 <= pad <= 16) or pt[-pad:] != bytes([pad]) * pad:
            return None
        return pt[:-pad].decode("utf-8", "replace")
    except Exception:
        return None


def _host_from_url(url):
    """从 URL 取 hostname (失败返回 None)"""
    try:
        from urllib.parse import urlparse
        return urlparse(url).hostname
    except Exception:
        return None


def stb_register():
    """第0步: STBRegister 注册 STB, 返回 (success, userid, password, final_url)。
    userid 从 <UserID> 读, password 从 <Password> 解密 —— 换盒子只需 STBID。
    final_url = 307 重定向后的地址, 其主机即 VSP_HOST (可用来自动推导)。"""
    url = BASE + "/EDS/STBRegister"
    body = f"<STBRegisterReq><STBID>{STBID}</STBID></STBRegisterReq>".encode()
    print(f"[*] STBRegister -> {url}")
    code, hdrs, body2, final = post(url, body, {"Content-Type": "text/xml; charset=UTF-8"})
    print(f"    HTTP {code}, final={final}")
    text = body2.decode("utf-8", "replace")
    print("    响应:", text[:300])
    m = re.search(r"<STBRegisterRsltCode>(.*?)</STBRegisterRsltCode>", text, re.S)
    ok = bool(m) and m.group(1).strip() == "0"
    um = re.search(r"<UserID>(.*?)</UserID>", text, re.S)
    userid = um.group(1).strip() if um else None
    password = None
    pm = re.search(r"<Password>(.*?)</Password>", text, re.S)
    if pm:
        import urllib.parse
        password = decrypt_password(urllib.parse.unquote(pm.group(1).strip()))
    if ok:
        print(f"    [✓] 注册成功 (RsltCode=0)  UserID={userid}  Password={password or '(解密失败, 用兜底)'}")
    else:
        print(f"    [!] 注册失败 (RsltCode={m.group(1).strip() if m else 'N/A'})")
    return ok, userid, password, final


def get_challenge():
    """第1步: 拿 Digest challenge (realm/nonce/opaque/algorithm/qop)"""
    url = BASE + "/EDS/pub/authentication_initial_challenge_v1"
    print(f"[*] challenge -> {url}")
    code, hdrs, body, final = post(url, b"", {"Content-Type": "text/plain; charset=UTF-8"})
    print(f"    HTTP {code}, final={final}")
    www = hdrs.get("WWW-Authenticate") or hdrs.get("Www-Authenticate")
    if not www:
        print("    响应头:", {k: v for k, v in hdrs.items() if k.lower() in ("www-authenticate", "location", "set-cookie")})
        print("    body:", body.decode("utf-8", "replace")[:300])
        raise SystemExit("[!] 没拿到 WWW-Authenticate challenge")
    print(f"    WWW-Authenticate: {www}")
    # 解析 "Digest realm=..,nonce=..,opaque=..,algorithm=..,qop=.."
    www = www.split(" ", 1)[1] if www.lower().startswith("digest") else www
    fields = {}
    for part in www.split(","):
        if "=" in part:
            k, v = part.split("=", 1)
            fields[k.strip()] = v.strip().strip('"')
    return fields


def md5(s):
    return hashlib.md5(s.encode()).hexdigest()


def digest_auth(ch, username=None, password=None):
    """第2步: Digest 鉴权, 拿 UserToken。username/password 默认全局值, 可被 STBRegister 返回值覆盖"""
    username = username or USERNAME
    password = password or PASSWORD
    realm = ch.get("realm", "")
    nonce = ch.get("nonce", "")
    opaque = ch.get("opaque", "")
    algorithm = ch.get("algorithm", "MD5")
    qop = ch.get("qop", "")
    # 精确 body (Frida 调 getPostInfo 实抓, 注意 ?> 前有空格):
    body = ("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>"
            f"<AuthenticationRequest><STBID>{STBID}</STBID></AuthenticationRequest>").encode()

    ha1 = md5(f"{username}:{realm}:{base64.b64encode(password.encode()).decode()}")
    ha2 = md5(f"POST:/EDS/pub/authentication_initial:{md5(body.decode())}")
    # countCN(1): Long.toHexString(1)="1", length=8-1=7, 循环 i2<=7 共8次 -> "00000000"+"1"
    nc = "000000001"
    cnonce = md5(str(secrets.randbelow(2**31)))
    if qop:
        response = md5(f"{ha1}:{nonce}:{nc}:{cnonce}:{qop}:{ha2}")
    else:
        response = md5(f"{ha1}:{nonce}:{nc}:{cnonce}:{ha2}")

    auth = (f'Digest realm="{realm}",nonce="{nonce}",opaque="{opaque}",'
            f'algorithm="{algorithm}",qop="{qop}",userid="{username}",'
            f'nc="{nc}",cnonce="{cnonce}",response="{response}"')
    print(f"[*] HA1={ha1}\n    HA2={ha2}\n    response={response}")
    print(f"[*] auth -> {BASE}/EDS/pub/authentication_initial")
    code, hdrs, body2, final = post(
        BASE + "/EDS/pub/authentication_initial", body,
        {"Content-Type": "text/xml; charset=UTF-8", "Authorization": auth})
    print(f"    HTTP {code}, final={final}")
    text = body2.decode("utf-8", "replace")
    print("    响应:", text[:400])
    m = re.search(r"<UserToken>(.*?)</UserToken>", text, re.S)
    sc = re.search(r"<StatusCode>(.*?)</StatusCode>", text, re.S)
    sc_val = sc.group(1).strip() if sc else "?"
    if sc:
        print(f"    StatusCode={sc_val}")
    # 从 final URL 提取当前 edsUrl 端口 (307 重定向目标, 即 ZJLogin 该用的端口)
    final_port = None
    pm = re.search(r"://112\.15\.228\.65:(\d+)", final)
    if pm:
        final_port = pm.group(1)
    if not m:
        return None, sc_val, final_port
    return m.group(1).strip(), sc_val, final_port


def zjlogin(usertoken, port):
    """第3步: ZJLogin 建会话, 拿 JSESSIONID + 新 userToken
    关键: 不带任何自定义头 (Frida 实抓 REQ-HEADERS 为空), 端口用当前 edsUrl 端口
    返回 (jsid, new_usertoken)"""
    url = f"http://{VSP_HOST}:{port}/VSP/V3/ZJLogin"
    print(f"[*] ZJLogin -> {url}")
    body = json.dumps({"userToken": usertoken}).encode()
    # 不带任何自定义头 (UA/Content-Type 都不带, 与盒子一致)
    code, hdrs, body2, final = post(url, body, {}, no_ua=True)
    print(f"    HTTP {code}")
    text = body2.decode("utf-8", "replace")
    print("    响应:", text[:300])
    jsid = None
    new_token = usertoken
    retcode = None
    try:
        data = json.loads(text)
        jsid = data.get("jSessionID")
        if data.get("userToken"):
            new_token = data["userToken"]
        ret = data.get("result", {})
        retcode = ret.get("retCode")
        print(f"    retCode={retcode} {ret.get('retMsg')}")
    except Exception:
        pass
    if not jsid:
        for c in (hdrs.get("Set-Cookie") or "").split(","):
            if "JSESSIONID=" in c:
                jsid = c.split("JSESSIONID=")[1].split(";")[0].strip()
    return jsid, new_token, retcode


def find_working_port(usertoken, hint):
    """端口自动探测: 先试 hint, 失败则扫 7176-7200 找 ZJLogin 返回 000000000 的端口
    返回 (port, jsid, new_token); 找不到返回 (None, None, None)"""
    candidates = [hint] + [str(p) for p in range(7176, 7201) if str(p) != hint]
    for p in candidates:
        try:
            jsid, new_token, retcode = zjlogin(usertoken, p)
        except Exception as e:
            print(f"    [!] 端口 {p} 连接失败: {e}")
            continue
        if retcode == "000000000" and jsid:
            print(f"[✓] 端口 {p} ZJLogin 成功")
            return p, jsid, new_token
    return None, None, None


def fmt_ts(ms):
    """毫秒时间戳 -> HH:MM (CST)"""
    try:
        return datetime.fromtimestamp(int(ms) / 1000, CST).strftime("%H:%M")
    except Exception:
        return "??:??"


def parse_playbill(data):
    """解析节目单响应 -> 节目列表(按开始时间升序), 每项 {start,end,name,id}"""
    progs = []
    for cp in data.get("channelPlaybills", []):
        for p in cp.get("playbillLites", []):
            st = int(p.get("startTime", 0) or 0)
            progs.append({
                "_ms": st,
                "start": fmt_ts(st),
                "end": fmt_ts(p.get("endTime", 0)),
                "name": p.get("name", "(无标题)"),
                "id": p.get("ID", ""),
            })
    progs.sort(key=lambda x: x["_ms"])
    for p in progs:
        del p["_ms"]
    return progs


def save_playbill(progs, channel_id, day, retcode):
    """把完整节目列表存为 JSON + 可读文本, 返回 (json_path, txt_path)"""
    base = os.path.join(OUT_DIR, f"playbill_{channel_id}_{day}")
    payload = {
        "channel_id": channel_id,
        "date": day,
        "retCode": retcode,
        "count": len(progs),
        "generated_at": datetime.now(CST).strftime("%Y-%m-%d %H:%M:%S"),
        "programs": progs,
    }
    jp = base + ".json"
    with open(jp, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
    tp = base + ".txt"
    with open(tp, "w", encoding="utf-8") as f:
        f.write(f"节目单  频道={channel_id}  日期={day}  共{len(progs)}条\n")
        f.write("=" * 50 + "\n")
        for i, p in enumerate(progs, 1):
            f.write(f"{i:3d}. {p['start']} - {p['end']}  {p['name']}\n")
    return jp, tp


def load_channels():
    """读频道表 -> [(channel_id, name, group, no), ...] 按 分组/频道号 排序"""
    with open(CHANNELS_JSON, encoding="utf-8") as f:
        data = json.load(f)
    by_id = data.get("by_id", {})
    chans = [(cid, info.get("name", ""), info.get("group", ""), info.get("no", ""))
             for cid, info in by_id.items()]
    group_order = {"央视": 0, "卫视": 1, "特色": 2, "专题剧场": 3, "卡通": 4, "轮播": 5}
    def _key(c):
        no = int(c[3]) if str(c[3]).isdigit() else 999
        return (group_order.get(c[2], 99), no)
    chans.sort(key=_key)
    return chans


CSV_HEADERS = ["分组", "频道号", "频道ID", "频道", "开始", "结束", "节目", "节目ID"]


def _row(r):
    return [r["group"], r["no"], r["channel_id"], r["channel"],
            r["start"], r["end"], r["name"], r["id"]]


def export_csv(rows, path):
    """导出 CSV (UTF-8 BOM, Excel 双击直接打开不乱码)"""
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(CSV_HEADERS)
        for r in rows:
            w.writerow(_row(r))
    return path


def export_xlsx(rows, path):
    """导出 .xlsx (需 openpyxl; 不可用返回 None)"""
    try:
        from openpyxl import Workbook
        from openpyxl.styles import Font
    except ImportError:
        return None
    wb = Workbook()
    ws = wb.active
    ws.title = "节目单"
    ws.append(CSV_HEADERS)
    for c in ws[1]:
        c.font = Font(bold=True)
    for r in rows:
        ws.append(_row(r))
    for i, wd in enumerate([8, 8, 12, 16, 8, 8, 32, 14], 1):
        ws.column_dimensions[chr(64 + i)].width = wd
    ws.freeze_panes = "A2"
    wb.save(path)
    return path


def query_playbill(jsid, usertoken, port, channel_id, day,
                   mode="catchup", start_ms=None, end_ms=None):
    """第4步: 查节目单。
    mode="catchup" 回看(type=2): 今天全天 + 过去6天全天 (复刻盒子回看, 默认)
    mode="live"    普通(type=1): 今天现在起 + 明天
    start_ms/end_ms 可覆盖默认全天窗口 [00:00, 23:59:59]。"""
    if mode == "catchup":
        qp_type, is_fill, sort = "2", "1", "STARTTIME:ASC"
    else:  # live
        qp_type, is_fill, sort = "1", None, None
    if isinstance(day, str):
        day = datetime.strptime(day, "%Y-%m-%d").replace(tzinfo=CST)
    if start_ms is None:
        start_ms = int(day.replace(hour=0, minute=0, second=0, microsecond=0).timestamp() * 1000)
    if end_ms is None:
        end_ms = int(day.replace(hour=23, minute=59, second=59, microsecond=0).timestamp() * 1000)
    url = f"http://{VSP_HOST}:{port}/VSP/V3/QueryPlaybillListStcProps?userPlaybillListFilter=1589670010"
    qp = {"count": "100", "endTime": str(end_ms), "offset": "0", "startTime": str(start_ms), "type": qp_type}
    if is_fill is not None:
        qp["isFillProgram"] = is_fill
    if sort is not None:
        qp["sortType"] = sort
    body = {
        "needChannel": "0",
        "queryChannel": {"channelIDs": [channel_id], "contentType": "CHANNEL", "isReturnAllMedia": "1"},
        "queryPlaybill": qp,
    }
    headers = {
        "Cookie": f"JSESSIONID={jsid}",
        "Set-Cookie": f"JSESSIONID={jsid}; Path=/VSP/",
        "authorization": usertoken,
        "Content-Type": "application/json; charset=UTF-8",
        "Host": f"{VSP_HOST}:{port}",
    }
    print(f"[*] 节目单 -> {url}")
    code, hdrs, body2, final = post(url, json.dumps(body).encode(), headers)
    print(f"    HTTP {code}")
    return code, body2


def day_note(progs, mode="catchup"):
    """若该日节目未从 00:00 开始, 打印原因说明"""
    if progs and progs[0]["start"] > "00:00":
        if mode == "catchup":
            print(f"\n[提示] 该日未从 00:00 开始(首条 {progs[0]['start']}) —— 超过 6 天的回看, 早期节目已过期。")
            print("       回看范围: 今天全天 + 过去 6 天全天; 第 7 天起早期节目缺失, 第 8 天起无数据。")
        else:
            print(f"\n[提示] 该日未从 00:00 开始(首条 {progs[0]['start']}) —— live 模式把'今天'裁到当前时间。")
            print("       要完整全天(含已播完)和过去 6 天, 请用默认回看模式(不加 live)。")


def run_single(jsid, token, port, channel_id, day, mode="catchup"):
    """单频道: 打印完整列表 + 存 JSON/文本"""
    code, body2 = query_playbill(jsid, token, port, channel_id, day, mode=mode)
    try:
        data = json.loads(body2.decode("utf-8", "replace"))
        res = data.get("result", {})
        progs = parse_playbill(data)
        print(f"[✓] retCode={res.get('retCode')} {res.get('retMsg')}  节目数={len(progs)}")
        print("\n" + "=" * 56)
        print(f" 节目单  频道={channel_id}  日期={day}  共{len(progs)}条  [{mode}]")
        print("=" * 56)
        for i, p in enumerate(progs, 1):
            print(f"{i:3d}. {p['start']} - {p['end']}  {p['name']}")
        day_note(progs, mode)
        if progs:
            jp, tp = save_playbill(progs, channel_id, day, res.get("retCode"))
            print("\n[✓] 已保存完整节目列表:")
            print(f"    JSON: {jp}")
            print(f"    文本: {tp}")
        else:
            print("[!] 未解析到节目 (retCode 或 body 异常, 见下)")
            print(body2.decode("utf-8", "replace")[:500])
    except Exception:
        print(body2.decode("utf-8", "replace")[:500])


def run_batch(jsid, token, port, day, mode="catchup"):
    """全部频道: 复用同一会话逐个拉, 汇总导出 JSON/CSV/XLSX"""
    chans = load_channels()
    print(f"\n[*] 批量拉取全部 {len(chans)} 个频道 [{mode}] ...")
    all_rows, per_channel = [], {}
    ok = fail = 0
    for i, (cid, cname, cgroup, cno) in enumerate(chans, 1):
        try:
            code, body2 = query_playbill(jsid, token, port, cid, day, mode=mode)
            data = json.loads(body2.decode("utf-8", "replace"))
            progs = parse_playbill(data)
            for p in progs:
                all_rows.append({"group": cgroup, "no": cno, "channel_id": cid,
                                 "channel": cname, "start": p["start"], "end": p["end"],
                                 "name": p["name"], "id": p["id"]})
            per_channel[cid] = {"name": cname, "group": cgroup, "no": cno,
                                "count": len(progs), "programs": progs}
            ok += 1
            print(f"[{i:3}/{len(chans)}] {cgroup} {cname} ({cid})  ✓ {len(progs)} 条")
        except Exception as e:
            fail += 1
            print(f"[{i:3}/{len(chans)}] {cgroup} {cname} ({cid})  ✗ {e}")
    total = len(all_rows)
    print("\n" + "=" * 60)
    print(f" 批量完成: 成功 {ok} / 失败 {fail} 频道, 共 {total} 条节目")
    print("=" * 60)
    if mode == "catchup":
        print(" [提示] 回看模式: 今天全天 + 过去6天全天; 第7天起早期缺失, 第8天起无数据。")
    else:
        print(" [提示] live 模式: 今天(自当前节目起) + 明天; 无历史。")
        print("        要完整全天+过去6天, 用默认回看模式(不加 live)。")
    if not all_rows:
        print("[!] 未拉到任何节目")
        return
    base = os.path.join(OUT_DIR, f"playbill_all_{day}")
    jp = base + ".json"
    with open(jp, "w", encoding="utf-8") as f:
        json.dump({"date": day, "ok": ok, "fail": fail, "total": total,
                   "generated_at": datetime.now(CST).strftime("%Y-%m-%d %H:%M:%S"),
                   "channels": per_channel}, f, ensure_ascii=False, indent=1)
    cp = export_csv(all_rows, base + ".csv")
    xp = export_xlsx(all_rows, base + ".xlsx")
    print("\n[✓] 已保存汇总节目单:")
    print(f"    JSON : {jp}")
    print(f"    CSV  : {cp}")
    print(f"    XLSX : {xp}" if xp else "    XLSX : (openpyxl 不可用, 已跳过, 用 CSV)")


def run_week(jsid, token, port, channel_id, channel_all, mode="catchup"):
    """周回看: 今天 + 过去6天, 共7天, 复用同一会话逐天拉取。
    单频道 -> 每天存 playbill_<频道>_<日期>.json/.txt
    全频道 -> 每天存 playbill_all_<日期>.json/.csv/.xlsx"""
    today = datetime.now(CST).date()
    days = [(today + timedelta(days=off)).strftime("%Y-%m-%d") for off in range(0, -7, -1)]
    print(f"\n[*] 周回看: 今天 + 过去6天, 共 {len(days)} 天 (mode={mode})")
    for d in days:
        if channel_all:
            run_batch(jsid, token, port, d, mode)
        else:
            run_single(jsid, token, port, channel_id, d, mode)


def parse_args(argv):
    """类型识别、顺序无关地解析参数:
    - "all"            -> 全部频道
    - "live"           -> live 模式(type=1, 今天现在起+明天); 默认 catchup 回看
    - "week"           -> 周回看: 今天+过去6天, 共7天(逐天拉, 忽略单独日期)
    - 4位且 7176-7200  -> 端口 hint
    - YYYY-MM-DD       -> 日期
    - 其它纯数字        -> 频道 ID
    返回 (port, channel_id, day, channel_all, mode, week)"""
    channel_all = "all" in argv
    week = "week" in argv
    mode = "live" if "live" in argv else "catchup"
    port = channel_id = day = None
    for a in argv:
        if a in ("all", "live", "week"):
            continue
        if re.fullmatch(r"\d{4}", a) and 7176 <= int(a) <= 7200:
            port = a
        elif re.fullmatch(r"\d{4}-\d{2}-\d{2}", a):
            day = a
        elif re.fullmatch(r"\d+", a):
            channel_id = a
    if port is None:
        port = "7182"
    if channel_id is None:
        channel_id = "all" if channel_all else "42329858"
    if day is None:
        day = datetime.now(CST).strftime("%Y-%m-%d")
    return port, channel_id, day, channel_all, mode, week


def main():
    port, channel_id, day, channel_all, mode, week = parse_args(sys.argv[1:])

    print("=" * 60)
    print(" 完整本地登录 (只需 STBID, 无需 EPG/Frida)"
          + ("  [全部频道]" if channel_all else "") + f"  [{mode} 模式]"
          + ("  [周回看7天]" if week else ""))
    print("=" * 60)
    # 20244 = 注册过期, 需 STBRegister 后立刻 Digest, 失败重试 (盒子同款逻辑)
    usertoken = None
    final_port = None
    userid = None
    password = None
    reg_final = None
    for attempt in range(6):
        print(f"\n=== 尝试 {attempt+1}/6 ===")
        ok, userid, password, reg_final = stb_register()
        if not ok:
            continue
        ch = get_challenge()
        print(f"    challenge: realm={ch.get('realm')} nonce={ch.get('nonce')} qop={ch.get('qop')}")
        usertoken, code, final_port = digest_auth(ch, userid, password)
        if usertoken:
            break
        print(f"    [!] 未拿到 UserToken (code={code}), 重试...")
    if not usertoken:
        print("[!] 多次重试仍失败")
        return
    if userid:
        print(f"[✓] UserID (STBRegister 自动读取) = {userid}")
    print(f"[✓] Password (STBRegister 自动解密) = {password or '(解密失败, 用兜底 ' + PASSWORD + ')'}")
    print(f"[✓] UserToken = {usertoken}")
    # 若 VSP_HOST 仍是占位符, 从 STBRegister 的 307 重定向地址自动推导 (只需填 BASE)
    global VSP_HOST
    if VSP_HOST in ("", "YOUR_VSP_IP"):
        derived = _host_from_url(reg_final or "")
        base_host = _host_from_url(BASE)
        if derived and derived != base_host:
            VSP_HOST = derived
            print(f"[✓] VSP_HOST 自动推导 (来自 STBRegister 307 重定向) = {VSP_HOST}")
        else:
            print(f"[!] 无法自动推导 VSP_HOST (307 未跳转到新主机), 请手动填写 VSP_HOST")
    # 优先用 Digest 307 重定向的端口 (即当前 edsUrl 端口), 否则用命令行 hint
    hint = final_port if final_port else port
    print(f"[*] 开始端口探测 (hint={hint}, 来自 Digest 重定向={final_port})...")
    port, jsid, new_token = find_working_port(usertoken, hint)
    print(f"[✓] 工作端口 = {port}")
    print(f"[✓] JSESSIONID = {jsid}")
    print(f"[✓] 新 UserToken (ZJLogin 返回) = {new_token}")
    if not jsid:
        print("[!] 所有端口 ZJLogin 都失败 (看上方响应)")
        return
    if week:
        run_week(jsid, new_token, port, channel_id, channel_all, mode)
    elif channel_all:
        run_batch(jsid, new_token, port, day, mode)
    else:
        run_single(jsid, new_token, port, channel_id, day, mode)


if __name__ == "__main__":
    main()
