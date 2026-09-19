# China Mobile OTT Set-Top Box · Full Local Login Technical Document

> **Goal:** Reproduce the entire authentication chain in pure local Python — **no EPG login UI, no Frida, no live packet capture** — using only **STBID + static device password + VSP port**, then pull the playbill (TV schedule).
>
> This document covers: the full authentication flow, the encryption/decryption details of each step, the request-construction reasoning, and **where every piece of information came from** (decompilation / Frida / packet capture / box config / trial-and-error).

---

## Table of Contents

1. [Overview & Results](#1-overview--results)
2. [Environment & Key Constants](#2-environment--key-constants)
3. [Overall Flow](#3-overall-flow)
4. [Step-by-Step Details](#4-step-by-step-details)
   - Step 0: STBRegister
   - Step 1: Digest Challenge
   - Step 2: Digest Authentication (encryption core)
   - Step 3: ZJLogin
   - Step 4: QueryPlaybillListStcProps
5. [Encryption / Decryption Deep-Dive](#5-encryption--decryption-deep-dive)
6. [Provenance of Every Step](#6-provenance-of-every-step)
7. [Pitfalls & Lessons](#7-pitfalls--lessons)
8. [Appendix](#8-appendix)

---

## 1. Overview & Results

### 1.1 Background

The China Mobile OTT set-top box (Huawei EPG, package `com.pukka.ydepg`) gets its playbill data from a VSP server (`YOUR_VSP_IP`), which requires a full authentication chain. On the box, the EPG app and a separate `CMCC_Auth` process cooperate to log in.

Originally the only way to query the playbill was to attach Frida to the EPG process and read JSESSIONID + token from memory (`local_playbill.py`). The goal of this project was to **reproduce the whole chain in pure local Python**, removing any dependency on the running box.

### 1.2 Final Result

```
STBRegister  →  Digest Challenge  →  Digest Auth  →  ZJLogin  →  QueryPlaybill
  (register)       (get challenge)    (get UserToken)  (session)    (playbill)
```

- Inputs are only 3 items: `STBID`, `PASSWORD` (static `YOUR_PASSWORD`), VSP port (auto-detected).
- Verified end-to-end: ZJLogin `retCode=000000000`, playbill returned **36 programs** (CCTV-1).
- No EPG login, no Frida, no live capture needed.

### 1.3 One-line Principle

> The core is standard **HTTP Digest (MD5, qop=auth-int)** to get a UserToken, then **ZJLogin** with that token to get a JSESSIONID session, then query the playbill with JSESSIONID + token. The only "secret" is the static device password `YOUR_PASSWORD`; everything else is standard MD5.

---

## 2. Environment & Key Constants

| Item | Value | Note |
|---|---|---|
| EPG package | `com.pukka.ydepg` | Huawei EPG app |
| Auth process | `CMCC_Auth` | Separate auth process on the box |
| Box | `YOUR_BOX_IP:5555` | Android 9, armeabi-v7a, root, SELinux Permissive |
| STBID | `YOUR_STBID` | = `ro.serialno` (readable via ADB) |
| UserID | `YOUR_USERNAME` | Subscriber ID (= profileID) |
| PASSWORD | `YOUR_PASSWORD` | **Static** device password, 6 digits |
| MAC | `2c:16:db:a5:db:e2` | Box NIC MAC |
| EDS entry | `http://YOUR_EDS_HOST:PORT` | Auth entry, 307-redirects to VSP |
| VSP host | `YOUR_VSP_IP` | Playbill / ZJLogin server |
| VSP port | `7176`–`7200` (rotating) | Current edsUrl port, changes per run |
| Timezone | CST (UTC+8) | For playbill time window |

### 2.1 The Three Roles

```
YOUR_EDS_HOST:PORT   ← auth entry (EDS); POST then 307-redirects
        │  307 Location
        ▼
YOUR_VSP_IP:<port>/EPG/XML/...    ← auth handling (STBRegister / Digest)
YOUR_VSP_IP:<port>/VSP/V3/...     ← business (ZJLogin / playbill)
```

> Key insight: `YOUR_EDS_HOST:PORT` is only an **entry point**; every POST is **307-redirected** to some port on `YOUR_VSP_IP`. That port is the current edsUrl port and **rotates** (7176–7200).

---

## 3. Overall Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│  Step 0  STBRegister                                                 │
│  POST /EDS/STBRegister  body=<STBRegisterReq><STBID>..</..>          │
│  → 307 → YOUR_VSP_IP:<p>/EPG/XML/STBRegister                      │
│  → RsltCode=0, UserID, Password(encrypted)                          │
├─────────────────────────────────────────────────────────────────────┤
│  Step 1  Digest Challenge                                            │
│  POST /EDS/pub/authentication_initial_challenge_v1  (empty body)     │
│  → 307 → YOUR_VSP_IP:<p>/EPG/XML/...challenge_v1                  │
│  → WWW-Authenticate: Digest realm=..,nonce=..,qop=auth-int          │
├─────────────────────────────────────────────────────────────────────┤
│  Step 2  Digest Authentication                                       │
│  POST /EDS/pub/authentication_initial  + Authorization: Digest ...   │
│  HA1=MD5(user:realm:BASE64(pw))  HA2=MD5(POST:uri:MD5(body))        │
│  response=MD5(HA1:nonce:nc:cnonce:qop:HA2)                          │
│  → 307 → YOUR_VSP_IP:<p>/EPG/XML/authentication_initial           │
│  → StatusCode=0, UserToken, ValidFrom/ValidTo (24h)                  │
├─────────────────────────────────────────────────────────────────────┤
│  Step 3  ZJLogin                                                     │
│  POST YOUR_VSP_IP:<p>/VSP/V3/ZJLogin  body={"userToken":..}       │
│  ★ ZERO request headers (no UA / no Content-Type / no Cookie)       │
│  → retCode=000000000, JSESSIONID, new userToken, subscriberID...    │
├─────────────────────────────────────────────────────────────────────┤
│  Step 4  QueryPlaybillListStcProps                                   │
│  POST YOUR_VSP_IP:<p>/VSP/V3/QueryPlaybillListStcProps?..=1589670010│
│  headers: Cookie:JSESSIONID, authorization:<token>, UA:Huawei-EPG-APK│
│  body: {queryChannel:{channelIDs:[..]}, queryPlaybill:{start,end}}   │
│  → retCode=000000000, channelPlaybills[].playbillLites[]            │
└─────────────────────────────────────────────────────────────────────┘
```

### Step Summary

| Step | Method/URL | Body | Key Headers | Success Flag | Output |
|---|---|---|---|---|---|
| 0 | `POST /EDS/STBRegister` | `<STBRegisterReq><STBID>..</..>` | `Content-Type: text/xml` | `STBRegisterRsltCode=0` | UserID, encrypted Password |
| 1 | `POST /EDS/pub/authentication_initial_challenge_v1` | empty | — | returns `WWW-Authenticate` | realm/nonce/opaque/algorithm/qop |
| 2 | `POST /EDS/pub/authentication_initial` | `<AuthenticationRequest><STBID>..</..>` | `Authorization: Digest ...` | `StatusCode=0` | **UserToken** (24h) |
| 3 | `POST /VSP/V3/ZJLogin` | `{"userToken":..}` | **zero headers** | `retCode=000000000` | **JSESSIONID** + new token |
| 4 | `POST /VSP/V3/QueryPlaybillListStcProps` | JSON | `Cookie`+`authorization`+`UA` | `retCode=000000000` | playbill |

---

## 4. Step-by-Step Details

### Step 0: STBRegister (device registration)

**Purpose:** Register the STB with the server to activate its auth state. Must be done before Digest, else 20244/11302.

**Request:**
```
POST http://YOUR_EDS_HOST:PORT/EDS/STBRegister
Content-Type: text/xml; charset=UTF-8

<STBRegisterReq><STBID>YOUR_STBID</STBID></STBRegisterReq>
```

**Response** (after 307 redirect to `YOUR_VSP_IP:<port>/EPG/XML/STBRegister`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<STBRegisterRes>
  <STBRegisterRsltCode>0</STBRegisterRsltCode>
  <Description>Success</Description>
  <UserID>YOUR_USERNAME</UserID>
  <Password>UnpueFds%2BkEb0R%2B7vNsOGg%3D%3D</Password>
</STBRegisterRes>
```

**Notes:**
- `STBRegisterRsltCode=0` = registration success.
- `UserID` = subscriber ID (the Digest username).
- `Password` is **URL-encoded Base64** (`UnpueFds%2BkEb0R%2B7vNsOGg%3D%3D` → `UnpueFds+kEb0R+7vNsOGg==`), an AES-encrypted device password.
- **This solution does NOT decrypt it** — the plaintext is the known static `YOUR_PASSWORD` (see §5.4).
- Registration state has a TTL; when expired, Digest returns 20244 and you must STBRegister again then Digest (the script's 6-retry loop implements this).

---

### Step 1: Digest Challenge (get challenge)

**Purpose:** Request the Digest auth parameters (realm/nonce, etc.).

**Request:**
```
POST http://YOUR_EDS_HOST:PORT/EDS/pub/authentication_initial_challenge_v1
Content-Type: text/plain; charset=UTF-8

(empty body)
```

**Response** (after 307 redirect to `YOUR_VSP_IP:<port>/EPG/XML/authentication_initial_challenge_v1`):
```
WWW-Authenticate: Digest realm="YOUR_REALM",nonce="YOUR_NONCE",opaque="",algorithm="MD5",qop="auth-int"
```

**Parsed fields:**
| Field | Example | Note |
|---|---|---|
| realm | `YOUR_REALM` | Auth realm (may vary per run) |
| nonce | `YOUR_NONCE` | Server random |
| opaque | `` (empty) | Pass-through |
| algorithm | `MD5` | Hash |
| qop | `auth-int` | **Integrity** (HA2 must include body hash) |

> `qop=auth-int` is critical: it forces HA2 to include the request-body MD5 — the biggest difference from plain Digest.

---

### Step 2: Digest Authentication (encryption core)

**Purpose:** Compute the `Authorization: Digest ...` header from STBID + password to get a UserToken.

**Request:**
```
POST http://YOUR_EDS_HOST:PORT/EDS/pub/authentication_initial
Content-Type: text/xml; charset=UTF-8
Authorization: Digest realm="YOUR_REALM",nonce="YOUR_NONCE",opaque="",algorithm="MD5",qop="auth-int",userid="YOUR_USERNAME",nc="000000001",cnonce="<32hex>",response="<32hex>"

<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><AuthenticationRequest><STBID>YOUR_STBID</STBID></AuthenticationRequest>
```

**Response** (after 307 redirect to `YOUR_VSP_IP:<port>/EPG/XML/authentication_initial`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<AuthenticationResponse>
  <StatusCode>0</StatusCode>
  <UserToken>YOUR_USERTOKEN</UserToken>
  <ValidFrom>20260918194403</ValidFrom>
  <ValidTo>20260919194403</ValidTo>
</AuthenticationResponse>
```

**Notes:**
- `StatusCode=0` = auth success.
- `UserToken` valid for 24h (ValidFrom→ValidTo).
- **The request body must be byte-exact**: there MUST be a **space** before `?>` in `standalone='yes' ?>` (see §7.1 — the biggest pitfall).
- The body's XML declaration uses **single quotes** (`version='1.0'`) and `standalone='yes'`.

---

### Step 3: ZJLogin (create session)

**Purpose:** Use the UserToken to create a JSESSIONID session on the VSP. This bridges "auth" to "business".

**Request:**
```
POST http://YOUR_VSP_IP:<port>/VSP/V3/ZJLogin
(★ ZERO request headers: no User-Agent, no Content-Type, no Cookie)

{"userToken":"YOUR_USERTOKEN"}
```

**Response:**
```json
{
  "result": {"retMsg": "Successfully", "retCode": "000000000"},
  "jSessionID": "YOUR_JSESSIONID",
  "userToken": "YOUR_USERTOKEN",
  "bossID": "ZHEJIANGMOBILE",
  "subscriberID": "YOUR_USERNAME",
  "profileSN": "YOUR_PROFILE_SN",
  "profileID": "YOUR_USERNAME",
  "RRSAddr": "http://hwltc.tv.cdn.zj.chinamobile.com",
  "subnetID": "YOUR_SUBNET",
  "returnJSessionIdInBody": "true",
  "returnCSessionIdInBody": "true"
}
```

**Notes (two fatal details here):**
1. **Zero request headers:** The box's real ZJLogin request has **empty** headers. If the script sends `User-Agent: Huawei-EPG-APK` or `Content-Type`, it fails with `125023001 Find session failed`. In Python you must explicitly suppress urllib's default UA (set it to empty string).
2. **Port must be the current edsUrl port:** The UserToken's session is bound to a specific VSP instance (port); wrong port → `Find session failed`. Ports rotate 7176–7200; the script auto-detects (prefers the port from Step 2's 307 landing).
3. The `userToken` in the response is a **new token** (may differ from Step 2's, or may be identical); subsequent business calls must use **this one**.
4. `jSessionID` appears both in the body and in `Set-Cookie`.

---

### Step 4: QueryPlaybillListStcProps (pull playbill)

**Purpose:** Query a channel's playbill for a day, with JSESSIONID + token.

**Request:**
```
POST http://YOUR_VSP_IP:<port>/VSP/V3/QueryPlaybillListStcProps?userPlaybillListFilter=1589670010
User-Agent: Huawei-EPG-APK
Cookie: JSESSIONID=YOUR_JSESSIONID
Set-Cookie: JSESSIONID=YOUR_JSESSIONID; Path=/VSP/
authorization: <userToken from Step 3>
Content-Type: application/json; charset=UTF-8

{
  "needChannel": "0",
  "queryChannel": {"channelIDs": ["42329858"], "contentType": "CHANNEL", "isReturnAllMedia": "1"},
  "queryPlaybill": {"count": "100", "endTime": "<ms of 23:59:59>", "offset": "0", "startTime": "<ms of 00:00:00>", "type": "1"}
}
```

**Response:**
```json
{
  "result": {"retCode": "000000000", "retMsg": "QueryPlaybillListStcProps successfully!"},
  "channelPlaybills": [
    { "channelID": "42329858", "playbillLites": [ {prog1}, {prog2}, ... ] }
  ]
}
```

**Notes:**
- `userPlaybillListFilter=1589670010` is a fixed query filter (from capture).
- `startTime`/`endTime` are **millisecond timestamps**, computed for the day 00:00:00–23:59:59 in CST (UTC+8).
- `channelIDs` is an array of channel IDs (e.g. `42329858`=CCTV-1); full list of 119 channels in `channel_ids.json`.
- Sending `Set-Cookie` as a **request header** is a box quirk (seen in capture); kept for exact alignment.
- This step **does** send `User-Agent: Huawei-EPG-APK` (opposite of ZJLogin).

---

## 5. Encryption / Decryption Deep-Dive

### 5.1 Auth Algorithm: HTTP Digest (MD5, qop=auth-int)

Standard HTTP Digest Authentication (RFC 2617), **auth-int** variant. Core is three MD5s:

```
HA1      = MD5( username : realm : BASE64(password) )
HA2      = MD5( "POST:/EDS/pub/authentication_initial:" + MD5(body) )
response = MD5( HA1 : nonce : nc : cnonce : qop : HA2 )
```

> `qop=auth-int` makes HA2 include the **request-body MD5** (plain auth only hashes method:URI). This is the only algorithmic difference from plain Digest.

### 5.2 Step-by-Step Computation with Real Values (reproducible)

From a successful run:

**Inputs:**
- `username = YOUR_USERNAME`
- `realm = YOUR_REALM`
- `password = YOUR_PASSWORD`
- `nonce = YOUR_NONCE`
- `nc = 000000001`
- `cnonce = <MD5(random int)>` (32-hex)
- `qop = auth-int`
- `body = <?xml version='1.0' encoding='UTF-8' standalone='yes' ?><AuthenticationRequest><STBID>YOUR_STBID</STBID></AuthenticationRequest>`

**① Compute BASE64(password):**
```
BASE64("YOUR_PASSWORD") = "BASE64(YOUR_PASSWORD)"
```

**② Compute HA1:**
```
HA1 = MD5("YOUR_USERNAME:YOUR_REALM:BASE64(YOUR_PASSWORD)")
    = YOUR_HA1
```

**③ Compute body MD5, then HA2:**
```
MD5(body) = YOUR_BODY_MD5
HA2 = MD5("POST:/EDS/pub/authentication_initial:YOUR_BODY_MD5")
    = YOUR_HA2
```

**④ Compute response:**
```
response = MD5("YOUR_HA1:YOUR_NONCE:000000001:<cnonce>:auth-int:YOUR_HA2")
```
> With a fixed demo cnonce = MD5("12345") = `827ccb0eea8a706c4c34a16891f84e7b`:
> `response = e069bbf52a5aa8ff69664508ce775d49`
> The real response differs per run (random cnonce), but the formula + HA1/HA2 are verified; the server recomputes and compares.

**⑤ Assemble the Authorization header:**
```
Authorization: Digest realm="YOUR_REALM",nonce="YOUR_NONCE",opaque="",algorithm="MD5",qop="auth-int",userid="YOUR_USERNAME",nc="000000001",cnonce="<cnonce>",response="<32hex>"
```

> The server recomputes HA1→response with its own realm/password and compares to the client's response; a match means auth success.

### 5.3 nc / cnonce Generation (error-prone)

- **nc = `000000001`** (9 characters, NOT 8!)
  - Decompiled `AuthManagerImpl.countCN(1)`: `Long.toHexString(1)="1"`, pad to 8 then append the count → 9-char `000000001`.
  - Writing 8-char `00000001` fails auth.
- **cnonce = `MD5(str(random_int))`**
  - Client picks a 31-bit random int, stringifies, MD5 → 32-hex.
  - Different each request; the server doesn't check randomness, only format.

### 5.4 Device Password: static plaintext + AES ciphertext

- **Plaintext `YOUR_PASSWORD`:** 6-digit device password, **static/immutable**.
  - Source: attached Frida to `CMCC_Auth`, read the plaintext from memory/return values (see §6).
  - Because it's static, it's hardcoded; no box needed at runtime.
- **STBRegister's `Password` field:**
  - `UnpueFds%2BkEb0R%2B7vNsOGg%3D%3D` (URL-encoded) → `UnpueFds+kEb0R+7vNsOGg==` (Base64) → AES ciphertext.
  - It's the **same password** as plaintext `YOUR_PASSWORD`, in two forms.
  - **This solution does NOT decrypt it:** Digest uses the plaintext `YOUR_PASSWORD` (BASE64'd) directly for HA1, never touching the AES ciphertext.
  - (Early on, `test_aes.py` tried AES decryption to verify consistency, but the simpler "read plaintext directly" path won.)

### 5.5 Encryption/Decryption Summary

| Stage | Algorithm | Input | Output | Required? |
|---|---|---|---|---|
| Digest HA1 | MD5 | user:realm:BASE64(pw) | 32hex | yes |
| Digest HA2 | MD5 | POST:uri:MD5(body) | 32hex | yes (auth-int) |
| Digest response | MD5 | HA1:nonce:nc:cnonce:qop:HA2 | 32hex | yes |
| cnonce | MD5 | random int | 32hex | yes |
| Device password | AES | plaintext YOUR_PASSWORD | ciphertext | **no** (plaintext known) |
| UserToken/JSESSIONID | server-generated | — | opaque string | pass-through |

> UserToken and JSESSIONID are **server-generated opaque tokens**; the client doesn't encrypt them, just carries them.

---

## 6. Provenance of Every Step

> This is the complete answer to "where each step came from." Four source types: **decompiled EPG source**, **Frida dynamic hook**, **offline packet capture**, **box config/ADB**, plus **trial-and-error**.

### 6.1 Source Matrix

| Info | Source | Specific origin |
|---|---|---|
| STBRegister URL + body format | decompile + capture | `AuthenticatePresenter` login chain; `yidong.pcapng` |
| STBID value | ADB | `ro.serialno` = `YOUR_STBID` |
| UserID / password plaintext | Frida (CMCC_Auth) | `decrypt_password.py` reads CMCC_Auth memory/return |
| 307 redirect behavior | capture + live test | all EDS POSTs 307 to `YOUR_VSP_IP:<port>` |
| Digest challenge URL | decompile | `AuthManagerImpl` auth flow |
| Digest algorithm (HA1/HA2/response) | decompile | `AuthManagerImpl.java:434-471` |
| nc=000000001 (9 chars) | decompile | `countCN(1)` zero-pad logic |
| Digest body exact bytes (the space!) | **Frida** | `get_body.py` calls box `XMLParseCls.getPostInfo` live |
| body MD5 check value | Frida + local recompute | `verify_body.py` = `YOUR_BODY_MD5` |
| ZJLogin URL | decompile | `AuthenticatePresenter.zjLogin()` (1175–1197) + `Constant.URL.ZJ_LOGIN` |
| ZJLogin zero headers | decompile + **Frida live** | `LoginNetService.zjLogin` no `@Headers`; `LogInterceptor:129`; `hook_zjlogin.py` capture |
| ZJLogin uses current edsUrl port | box config + Frida | `config.json` `edsUrl`; `hook_zjlogin.py` captured 7200 |
| ZJLogin response fields | Frida live | `hook_zjlogin.py` printed full response |
| Playbill URL + body + headers | capture | `yidong.pcapng` |
| `userPlaybillListFilter=1589670010` | capture | fixed filter param |
| Channel ID table | capture/curated | `channel_ids.json` (119 channels) |
| `User-Agent: Huawei-EPG-APK` | decompile | `EPG_APK_USER_AGENT` (line 54), business requests only |

### 6.2 Key Decompiled Source Locations

- `com/pukka/ydepg/launcher/mvp/presenter/AuthenticatePresenter.java`
  - `zjLogin()` (1175–1197): `getEdsURL() + "/VSP/V3/ZJLogin"`, body=`{"userToken":localToken}`
  - `doBaseAuthenticate` (386–425): Digest main flow
  - Login chain: getToken → ZJLogin → querySubscriberInfo → queryCustomizeConfig
- `com/pukka/ydepg/launcher/http/LoginNetService.java`
  - `zjLogin` is `@POST @Body` with **no `@Headers`** → proves ZJLogin sends no custom headers
- `com/pukka/ydepg/common/http/interceptor/LogInterceptor.java`
  - line 129: for ZJLogin URL, cookie var set empty → no Cookie sent
  - `EPG_APK_USER_AGENT="Huawei-EPG-APK"` (line 54) applied only in the else-branch (business requests), **not** to ZJLogin
- `AuthManagerImpl.java:434-471`: Digest HA1/HA2/response computation
- `tools/XMLParseCls.java`: `getPostInfo` is an **instance** method, generates the exact XML body

### 6.3 Key Frida Hook Scripts

| Script | Purpose | What it solved |
|---|---|---|
| `decrypt_password.py` | attach CMCC_Auth, read plaintext password | got static `YOUR_PASSWORD` |
| `get_body.py` | attach CMCC_Auth, call `XMLParseCls.$new().getPostInfo("AuthenticationRequest",{STBID})` | got the **exact bytes** of the Digest body (incl. the space) |
| `verify_body.py` | recompute body MD5 locally | confirmed `YOUR_BODY_MD5` |
| `hook_zjlogin.py` | attach EPG (by pid), hook `okhttp3.RealCall.execute`, trigger `zjLogin()` | captured ZJLogin URL/zero-headers/full response |
| `list_procs.py` | list Frida-visible processes | found EPG absent from enumeration → must attach by pid |

> Frida version: only **16.5.9** works (17.x SIGSEGV). The EPG process is NOT in `enumerate_processes`; you must **attach by pid** directly.

### 6.4 What the Packet Capture (yidong.pcapng) Provided

- Full request/response for STBRegister / Digest / ZJLogin / playbill
- The 307 redirect chain
- All playbill request headers (incl. the `Set-Cookie`-as-request-header quirk)
- Fixed params like `userPlaybillListFilter=1589670010`
- Channel IDs and playbill structure

---

## 7. Pitfalls & Lessons

### 7.1 Pitfall 1: 20244 — Digest body missing a space (biggest, longest blocker)

- **Symptom:** Digest auth kept returning 20244.
- **Root cause:** In the body's XML declaration `standalone='yes' ?>`, there MUST be a **space** before `?>`. The locally-built body lacked it → different `MD5(body)` → different HA2 → different response → auth fail.
- **Fix:** Used Frida to call the box's own `XMLParseCls.getPostInfo` to capture the exact body (`get_body.py`), confirmed the space before `?>`; local MD5 recompute = `YOUR_BODY_MD5` matched the box.
- **Lesson:** Byte-level protocols can't be judged by "looks the same"; compare byte-for-byte against the reference implementation.

### 7.2 Pitfall 2: 125023001 "Find session failed" (ZJLogin failure)

- **Symptom:** ZJLogin returned `125023001 Find session failed`.
- **Root cause (two, compounding):**
  1. **Wrong port:** script hardcoded `7182` (stale config.json), but the current edsUrl port was `7200` (later measured `7183`). The UserToken session is bound to a specific VSP instance; wrong port → session not found.
  2. **Extra headers:** script injected global `User-Agent: Huawei-EPG-APK` + `Content-Type`, but the box's ZJLogin has **zero** headers.
- **Fix:**
  - Port: added `find_working_port()` auto-detection (prefers the Digest 307 landing port, then scans 7176–7200).
  - Headers: added a `no_ua` switch in `_raw_post`/`post`; ZJLogin uses `no_ua=True` to suppress UA.
- **Lesson:** VSP multi-instance + port rotation; session bound to port; different endpoints have different header requirements (ZJLogin zero headers, business requests need UA).

### 7.3 Pitfall 3: nc written as 8 chars

- **Symptom:** Digest auth failed.
- **Root cause:** `nc` should be 9-char `000000001`, mistakenly written as 8-char `00000001`.
- **Fix:** Per decompiled `countCN(1)` logic, confirmed 9 chars.

### 7.4 Pitfall 4: urllib doesn't follow POST 307

- **Symptom:** Missing manual redirect logic; POST 307 lost the body.
- **Fix:** `post()` manually loops through 301/302/303/307/308, preserving the original body.

### 7.5 Pitfall 5: challenge header parsing

- **Symptom:** `WWW-Authenticate` value has a `Digest ` prefix; splitting by comma directly mis-parses.
- **Fix:** First `split(" ",1)[1]` to strip the `Digest` prefix, then parse fields.

### 7.6 Pitfall 6: PowerShell inline XML `<`

- **Symptom:** pwsh inline XML with `<` errors "The '<' operator is reserved".
- **Fix:** Use a `.py` file; don't inline XML in the command line.

### 7.7 Pitfall 7: Frida can't see the EPG process

- **Symptom:** `enumerate_processes()` (122 procs) lacks `com.pukka.ydepg`, but `ps -A` shows it.
- **Fix:** EPG must be **attached by pid** directly (`device.attach(pid)`), not by name.

---

## 8. Appendix

### 8.1 Full Runnable Script

See `full_local_login.py` (271 lines) in the same directory. Core structure:

```python
STBID    = "YOUR_STBID"
USERNAME = "YOUR_USERNAME"
PASSWORD = "YOUR_PASSWORD"
BASE     = "http://YOUR_EDS_HOST:PORT"
VSP_HOST = "YOUR_VSP_IP"

def _raw_post(url, data, headers, no_ua=False): ...   # no_ua suppresses UA
def post(url, data=b"", headers=None, no_ua=False): ... # manual 307 follow
def stb_register(): ...                                # Step 0
def get_challenge(): ...                               # Step 1
def digest_auth(ch): ...                               # Step 2 (returns usertoken, code, final_port)
def zjlogin(usertoken, port): ...                      # Step 3 (no_ua=True, returns jsid, new_token, retcode)
def find_working_port(usertoken, hint): ...            # port auto-detection
def query_playbill(jsid, usertoken, port, ch, day): ...# Step 4
def main(): ...                                        # wires it all up
```

**Run** (on a machine that can reach `YOUR_VSP_IP`):
```
python full_local_login.py [port-hint] [channel-id] [date]
# e.g.:
python full_local_login.py 7200 42329207 2026-09-19
```

### 8.2 Error Code Quick Reference

| Code | Meaning | Handling |
|---|---|---|
| `20244` / `20245` | registration expired / Digest body wrong | re-STBRegister then Digest; check body bytes |
| `11302` | registration error | re-STBRegister + retry |
| `20105` | realm param error | check realm parsing |
| `125023001` | Find session failed | **wrong port or extra headers** (ZJLogin) |
| `125010001` | queryChannel is null | check channel ID / body structure |
| `000000000` | success | — |

### 8.3 Common Channel IDs

| Channel | ID |
|---|---|
| CCTV-1 | `42329858` |
| Shaanxi TV | `42329207` |

Full list of 119 channels in `D:\deepseek\节目单工具\channel_ids.json`.

### 8.4 Key Constants Quick Reference

| Constant | Value |
|---|---|
| STBID | `YOUR_STBID` |
| USERNAME | `YOUR_USERNAME` |
| PASSWORD | `YOUR_PASSWORD` (static) |
| BASE | `http://YOUR_EDS_HOST:PORT` |
| VSP_HOST | `YOUR_VSP_IP` |
| VSP port | `7176`–`7200` (rotating) |
| Digest URI | `/EDS/pub/authentication_initial` |
| ZJLogin URI | `/VSP/V3/ZJLogin` |
| Playbill URI | `/VSP/V3/QueryPlaybillListStcProps?userPlaybillListFilter=1589670010` |
| nc | `000000001` (9 chars) |
| body MD5 (check) | `YOUR_BODY_MD5` |

### 8.5 Fallback

- `local_playbill.py`: Frida attaches to EPG, reads JSESSIONID + token from memory, queries playbill directly (verified, 38 programs CCTV-1). Serves as a fallback to the pure-local solution.

---

## Conclusion

The whole auth chain has **no real "strong encryption"** — the core is standard HTTP Digest (MD5), and the only sensitive input is the static device password `YOUR_PASSWORD`. The real difficulty is not the algorithm but:

1. **Byte-level exactness** (the space in the Digest body);
2. **Header differences** (ZJLogin zero headers vs business requests need UA);
3. **Port rotation** (VSP multi-instance, session bound to port).

Master these three, and the entire chain can be stably reproduced in pure local Python.
