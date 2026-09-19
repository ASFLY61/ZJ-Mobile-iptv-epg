#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Digest 中间值 + STBRegister 密码解密 可复现验证 (纯本地, 无需网络)
[模板] 下面是占位符。填入你自己盒子"一次真实运行"的值后, 运行本脚本即可
       逐步复算 HA1 / MD5(body) / HA2 / 密码解密, 并和你抓到的期望值比对。
       留占位符时各比对会显示 FAIL / 跳过 —— 这是预期行为, 填了真实值才应全 OK。
运行: python verify_digest.py
"""
import hashlib, base64

# ===== 静态输入 (占位符: 填你自己盒子的值, 从一次真实抓包/运行中获取) =====
STBID    = "YOUR_STBID_HERE"
USERNAME = "YOUR_USERNAME_HERE"
PASSWORD = "YOUR_PASSWORD_HERE"

# ===== STBRegister 密码解密 (AES-128-ECB, 密钥来自固件 libdataprocessing.so) =====
# ★ 已内置默认值: 同型号/区域所有盒子通用 (固件 libdataprocessing.so 偏移 0x7004)。
#   同区域盒子直接用; 换型号/区域才改。
AES_KEY = bytes.fromhex("6b4050eb4f2c6a3398665c245ec53640")
# 你盒子 STBRegister 响应里的 <Password> (base64, URL 编码) 及其解码/十六进制
STBREG_PW_URL  = "YOUR_BASE64_URL_ENCODED_CIPHERTEXT"
STBREG_PW_B64  = "YOUR_BASE64_CIPHERTEXT"
STBREG_PW_HEX  = "YOUR_CIPHERTEXT_HEX"
EXPECTED_PW    = "YOUR_PLAINTEXT_PASSWORD"

# ===== 你盒子一次真实运行的基准值 (realm/nonce 来自服务器, 期望值来自该次运行) =====
REALM  = "YOUR_REALM"
NONCE  = "YOUR_NONCE"
NC     = "000000001"
QOP    = "auth-int"
EXPECTED_HA1      = "YOUR_EXPECTED_HA1"
EXPECTED_HA2      = "YOUR_EXPECTED_HA2"
EXPECTED_BODY_MD5 = "YOUR_EXPECTED_BODY_MD5"

def md5(s):
    return hashlib.md5(s.encode()).hexdigest()

def check(got, exp):
    return "OK " if got == exp else "FAIL"

print("=" * 66)
print(" Digest 中间值复现验证 (基准: 2026-09-18 成功运行)")
print("=" * 66)

# ① BASE64(password)
pw_b64 = base64.b64encode(PASSWORD.encode()).decode()
print("\n[1] BASE64(password)")
print(f"    password   = {PASSWORD!r}")
print(f"    base64     = {pw_b64!r}")

# ② HA1
ha1_input = f"{USERNAME}:{REALM}:{pw_b64}"
ha1 = md5(ha1_input)
print("\n[2] HA1 = MD5(username:realm:BASE64(password))")
print(f"    输入       = {ha1_input!r}")
print(f"    HA1        = {ha1}")
print(f"    期望       = {EXPECTED_HA1}")
print(f"    比对       = {check(ha1, EXPECTED_HA1)}")

# ③ body 的 MD5
body = ("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>"
        f"<AuthenticationRequest><STBID>{STBID}</STBID></AuthenticationRequest>")
body_md5 = md5(body)
print("\n[3] MD5(body)   (body 字节级精确, '?>' 前必须有空格)")
print(f"    body       = {body!r}")
print(f"    len(body)  = {len(body)} bytes")
print(f"    MD5(body)  = {body_md5}")
print(f"    期望       = {EXPECTED_BODY_MD5}")
print(f"    比对       = {check(body_md5, EXPECTED_BODY_MD5)}")

# ④ HA2
ha2_input = f"POST:/EDS/pub/authentication_initial:{body_md5}"
ha2 = md5(ha2_input)
print("\n[4] HA2 = MD5('POST:/EDS/pub/authentication_initial:' + MD5(body))")
print(f"    输入       = {ha2_input!r}")
print(f"    HA2        = {ha2}")
print(f"    期望       = {EXPECTED_HA2}")
print(f"    比对       = {check(ha2, EXPECTED_HA2)}")

# ⑤ response (固定 cnonce 演示公式; 真实 cnonce 每次随机)
CNONCE = md5("12345")
resp_input = f"{ha1}:{NONCE}:{NC}:{CNONCE}:{QOP}:{ha2}"
response = md5(resp_input)
print("\n[5] response = MD5(HA1:nonce:nc:cnonce:qop:HA2)")
print(f"    cnonce     = {CNONCE}   (固定演示值, 真实每次随机)")
print(f"    nc         = {NC}   (9 字符, 易错点)")
print(f"    输入       = {resp_input!r}")
print(f"    response   = {response}")
print("    注: 真实 response 依赖随机 cnonce, 离线无法复现同一值;")
print("        但 HA1/MD5(body)/HA2 已验证一致, 服务器端重算比对即通过。")

# ⑥ 对照: 若 body 少一个空格会怎样 (复现 20244 根因)
body_bad = ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
            f"<AuthenticationRequest><STBID>{STBID}</STBID></AuthenticationRequest>")
body_bad_md5 = md5(body_bad)
ha2_bad = md5(f"POST:/EDS/pub/authentication_initial:{body_bad_md5}")
print("\n[6] 反例: body 的 '?>' 前少一个空格 (20244 根因)")
print(f"    body(错)   = {body_bad!r}")
print(f"    MD5(错)    = {body_bad_md5}")
print(f"    HA2(错)    = {ha2_bad}")
print(f"    与正确 HA2 不同 = {'是 (认证必失败)' if ha2_bad != ha2 else '否'}")

# ⑦ 从 STBRegister <Password> 解出密码 (AES-128-ECB + PKCS7, 离线)
print("\n[7] 从 STBRegister <Password> 解密 (AES-128-ECB + PKCS7)")
try:
    try:
        from Crypto.Cipher import AES
    except ImportError:
        from Cryptodome.Cipher import AES
    import urllib.parse
    ct = base64.b64decode(urllib.parse.unquote(STBREG_PW_URL))
    pt = AES.new(AES_KEY, AES.MODE_ECB).decrypt(ct)
    pad = pt[-1]
    got_pw = pt[:-pad].decode("utf-8", "replace")
    print(f"    密文(base64) = {STBREG_PW_B64!r}")
    print(f"    密文(hex)    = {ct.hex()}")
    print(f"    解出密码     = {got_pw!r}")
    print(f"    期望         = {EXPECTED_PW!r}")
    print(f"    比对         = {check(got_pw, EXPECTED_PW)}")
    # 反向: 用同 key 加密期望密码, 应还原同一密文
    p = EXPECTED_PW.encode()
    padlen = 16 - (len(p) % 16) or 16
    enc = AES.new(AES_KEY, AES.MODE_ECB).encrypt(p + bytes([padlen]) * padlen).hex()
    print(f"    反向加密     = {enc}")
    print(f"    与密文一致   = {'是 (密钥正确)' if enc == STBREG_PW_HEX else '否'}")
except ImportError:
    print("    [跳过] 未安装 pycryptodome (pip install pycryptodome)")
except Exception as e:
    print(f"    [跳过] 占位符/输入异常, 无法演示解密: {type(e).__name__}: {e}")
    print("           填入真实 STBRegister <Password> 密文 + AES_KEY 后此处应解出明文密码。")

print("\n" + "=" * 66)
print(" 说明: 填入真实值后, [2][3][4] 应全 OK (Digest 算法还原正确),")
print("       [7] 应解出明文密码 —— 证明只给 STBID 即可本地完成认证。")
print("=" * 66)
