# ZJ-Mobile-iptv-epg

> 纯本地 VSP 节目单获取工具 —— 只靠一个 **STBID**(机顶盒序列号),在本地 Python 完成整套认证并拉取电视**节目单**,**不需要盒子、不需要 Frida、不需要实时抓包**。
> 认证链路(`STBRegister → Digest → ZJLogin → QueryPlaybill`)与密码解密算法均从盒子固件/EPG 反编译精确还原。

- **UserID** 自动从 `STBRegister` 响应 `<UserID>` 读取;
- **密码** 自动从 `STBRegister` 响应 `<Password>` 解密(AES-128-ECB,密钥从 `libdataprocessing.so` 提取);
- **VSP 端口** 自动探测(7176–7200)。

**换盒子只改 `STBID` 一个值。**

## 📺 设备信息(测试环境)

| 项 | 值 |
|---|---|
| **设备型号** | CM311-5 YST(移动高清盒子 6) |
| **设置界面密码** | `10086` |
| **开启 ADB 调试模式** | 通过 adb 算码工具得出文件,开启 ADB 调试 |

> `STBID` 可直接在**机顶盒背面**的标签上获取(也可开启 ADB 后用 `adb shell getprop ro.serialno`)。

---

## ⚠️ 免责声明

- 本项目**仅供个人学习与研究**,用于访问**你自己设备、你自己订购**的服务。
- 它通过反编译还原了浙江移动OTT 盒子的认证流程,请勿用于访问你未获授权的服务,或任何违反运营商服务条款的用途。
- 使用者需自行承担使用本工具产生的一切后果。作者不对任何滥用负责。

---

## 目录

- [设备信息](#-设备信息测试环境)
- [特性](#-特性)
- [安装(两步)](#-安装两步)
- [用法](#-用法)
- [项目结构](#-项目结构)
- [工作原理(5 步)](#-工作原理5-步)
- [两种模式(catchup / live)](#-两种模式catchup--live)
- [配置(填占位符)](#-配置填占位符)
- [离线自检](#-离线自检)
- [文档](#-文档)
- [License](#-license)

---

## ✨ 特性

- **纯本地、自包含**:整个文件夹拷到任意 Windows 电脑即可运行,无需改路径。
- **只需 STBID**:UserID 与密码全自动从 `STBRegister` 解出。
- **回看模式(默认)**:今天全天 + 过去 6 天全天 + 明天全天(复刻盒子回看界面)。
- **live 模式**:今天(自当前节目起)+ 明天。
- **批量导出**:一次登录拉全部 119 频道,导出 JSON / CSV / Excel。
- **端口自动探测**:VSP 端口在 7176–7200 间轮换,自动找通。

---

## 📦 安装(两步)

**前提**:
1. 一台**能访问目标 VSP 服务器**(本仓库默认 `YOUR_VSP_IP`,通常是盒子同网段)的 Windows PC;
2. 已安装 **Python 3.8+**(https://www.python.org/downloads/,安装时勾选 **"Add python.exe to PATH"**)。

```bat
:: ① 首次: 双击 "安装依赖.bat"  ->  在本目录建 .venv 并安装 pycryptodome + openpyxl
:: ② 之后: 双击 "运行纯本地登录.bat"  ->  默认中央一套 + 今天(回看模式)
```

命令行(在仓库目录内):
```bat
python full_local_login.py
```

> 依赖 `pycryptodome`(解密码)、`openpyxl`(导出 Excel)均为**可选**:不装也能跑,只是回退到兜底密码 / 只出 CSV。

---

## 🚀 用法

参数**顺序无关**,按类型自动识别:`4位7176-7200`=端口 / `YYYY-MM-DD`=日期 / 其它数字=频道 / `all`=全部频道 / `live`=live 模式 / `week`=周回看。

```bat
python full_local_login.py 42329916 2026-09-19        # 频道 + 日期(最常用, 默认回看)
python full_local_login.py 42329858                     # 只给频道, 日期默认今天
python full_local_login.py 2026-09-19                  # 只给日期, 频道默认中央一套
python full_local_login.py 7194 42329207 2026-09-19   # 端口 + 频道 + 日期
python full_local_login.py live 42329858               # live 模式(今天现在起+明天)
python full_local_login.py 42329858 week               # 周回看: 今天+过去6天, 共7天
python full_local_login.py all 2026-09-19              # 全部119频道某一天, 导出 CSV/Excel
```

- 端口可省略(自动探测 7176–7200);`42329858`=中央一套、`42329916`=黑龙江卫视、`42329207`=陕西卫视,完整 119 台见 `channel_ids.json`。
- **`week` = 周回看**:一次登录、循环拉"今天 + 过去 6 天"共 7 天,每天各存一份文件。
- 每次运行按 `频道_日期` 命名,输出到仓库目录:`playbill_<频道>_<日期>.json` / `.txt`;批量另出 `playbill_all_<日期>.json` / `.csv` / `.xlsx`(CSV 用 UTF-8 BOM,双击 Excel 直接不乱码)。

---

## 📁 项目结构

```
.
├── full_local_login.py      # 主程序: 认证 + 拉节目单(端到端)
├── verify_digest.py         # 离线自检: 复算 Digest 中间值 + 解密码(不发网络请求)
├── probe_days.py            # 诊断: 实测两种模式的日期范围(需联网)
├── extract_base.py          # 从抓包文件自动提取 BASE / VSP_HOST(零依赖)
├── channel_ids.json         # 119 频道表(换区域替换此文件)
├── requirements.txt         # 依赖: pycryptodome + openpyxl(均可选)
├── 安装依赖.bat             # 首次: 建 .venv + 装依赖
├── 运行纯本地登录.bat        # 双击运行主程序
├── README.md
├── LICENSE
├── docs/
│   ├── 完整本地登录技术文档.md              # 认证流程/加解密/请求构造/溯源/踩坑
│   ├── Full_Local_Login_Technical_Document_EN.md   # 上者英文版
│   └── 可复现实验记录.md                    # 以某次成功运行为基准, 每步带完整中间值
└── reverse-engineering/                   # 反编译源码(算法来源依据, 仅研究用)
    ├── cmcc_auth/                         #   CMCC_Auth 认证进程(Digest/ZJLogin/AES)
    ├── epg/                               #   EPG 应用(回看 type=2 / 查询 bean)
    └── README.md                          #   每个文件揭示什么 + .so 密钥提取 + 法律声明
```

---

## 🔧 工作原理(5 步)

```
STBRegister  →  Digest Challenge  →  Digest Auth  →  ZJLogin  →  QueryPlaybill
   注册            取挑战             换 UserToken     建会话        拉节目单
 (307→VSP)      (拿 realm/nonce)   (MD5 三哈希)    (★零请求头)    (带 JSESSIONID)
```

| Step | 端点 | 关键 | 成功标志 |
|---|---|---|---|
| 0 | `POST /EDS/STBRegister` | 307 到 VSP | `RsltCode=0` |
| 1 | `POST /EDS/pub/authentication_initial_challenge_v1` | 空 body | 返回 `WWW-Authenticate` |
| 2 | `POST /EDS/pub/authentication_initial` | Digest(MD5, auth-int) | `StatusCode=0` → UserToken |
| 3 | `POST /VSP/V3/ZJLogin` | **零请求头** + 当前端口 | `retCode=000000000` → JSESSIONID |
| 4 | `POST /VSP/V3/QueryPlaybillListStcProps` | 带 Cookie+token+UA | `retCode=000000000` → 节目单 |

Digest 算法(反编译 `AuthManagerImpl.java` 精确还原):
```
HA1 = MD5(username:realm:BASE64(password))
HA2 = MD5("POST:/EDS/pub/authentication_initial:" + MD5(body))
response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
nc=00000001, cnonce=MD5(随机), body=<AuthenticationRequest><STBID>..</STBID></AuthenticationRequest>
```

---

## 📊 两种模式(catchup / live)

同一接口 `QueryPlaybillListStcProps`,靠 `QueryPlaybill.type` 字段切两种行为(反编译 `catchup/TVOD*Presenter` 确认):

| 模式 | 参数 | 返回范围 |
|---|---|---|
| **catchup 回看(默认)** | `type=2` + `isFillProgram=1` + `sortType=STARTTIME:ASC` | **今天全天 + 过去 6 天全天 + 明天全天**;第 7 天起早期节目缺失,第 8 天起无数据 |
| **live 普通** | `type=1` | 今天(自当前节目起,不含已播完)+ 明天全天;无历史 |

> 关键:用 `type=1` 只能拿"今天现在起 + 明天";改成 `type=2`(回看)后,今天能拿全天、过去 6 天全天——正好复刻盒子回看界面。

---

## 🔑 配置(填占位符)

仓库为**模板**:代码里的真实值已替换为占位符。运行前,把 `full_local_login.py` 顶部的占位符填成你自己的值。

| 占位符 | 含义 | 如何获取 |
|---|---|---|
| `YOUR_STBID_HERE` | 机顶盒序列号(**每盒必改**) | 机顶盒**背面标签**;或 `adb shell getprop ro.serialno` |
| `YOUR_PASSWORD_HERE` | 设备密码(明文,兜底) | 通常**留占位符即可**——会自动从 `STBRegister` 解密得到 |
| `YOUR_USERNAME_HERE` | 订购用户 ID(兜底) | 通常**留占位符即可**——自动从 `STBRegister` 的 `<UserID>` 读取 |
| `AES_KEY`(已内置默认) | 解 `<Password>` 用 | **已内置默认值**,同区域盒子直接用;换型号/区域才改(来自固件 `.so`) |
| `YOUR_EDS_HOST:PORT` | 认证入口(EDS) | 见下方「如何获取 BASE / VSP_HOST」 |
| `YOUR_VSP_IP` | VSP 业务服务器 IP | **通常留占位符即可**——程序从 `STBRegister` 的 307 重定向自动推导 |

- **最小可用**:只填 `STBID` + `BASE` 两项!`AES_KEY` 已内置默认,`USERNAME`/`PASSWORD`/`VSP_HOST` 全部自动解出/推导。
- VSP 端口在 `7176`–`7200` 间轮换,**自动探测**,无需填。
- `verify_digest.py` 里的占位符同理:填入你自己盒子"一次真实运行"的值,即可离线复算验证算法。

### 如何获取 `BASE` 和 `VSP_HOST`

这两个是**服务器地址**,同一区域/运营商的盒子相同。核心关系:

```
你只填 BASE (EDS 入口)
   └─ 程序 POST 到  BASE/EDS/STBRegister
        └─ 服务器 307 重定向到  http://<VSP_HOST>:<端口>/EPG/XML/STBRegister
             └─ 程序自动从该地址取出 VSP_HOST   ← 所以 VSP_HOST 一般不用手填
```

**获取 `BASE`(EDS 入口)—— 三种方式:**

0. **用 `extract_base.py` 自动提取(推荐, 有抓包文件时)**:把抓包文件丢给脚本,它自动找出 `BASE`(顺带 `VSP_HOST`),并给出可直接粘贴的代码。支持 HAR / PCAP / 代理文本日志。
   ```
   python extract_base.py capture.har
   # 或 capture.pcap / proxy.log
   # 输出:
   #   BASE = "http://<你的EDS域名>:8082"
   #   VSP_HOST = "<VSP IP>"
   ```
1. **从盒子配置(无需抓包)**:EPG 应用的 `config.json` 里有 `edsUrl` 字段,就是 `BASE`。
   ```
   adb shell cat /data/data/com.pukka.ydepg/shared_prefs/config.json
   # 或搜索: adb shell "cat /data/data/com.pukka.ydepg/**/config.json" | findstr edsUrl
   ```
   形如 `"edsUrl":"http://<你的EDS域名>:8082"`。
2. **从抓包手动看**:用盒子正常登录一次,抓包看**第一个请求**:
   ```
   POST http://<EDS域名>:8082/EDS/STBRegister
   ```
   取它的 `host:port` 作为 `BASE`。

**获取 `VSP_HOST`(可选, 一般自动推导):**

- 若留占位符 `YOUR_VSP_IP`,程序会用 `BASE` 发 `STBRegister`,从 **307 响应的 `Location`** 里自动取出 VSP 的 IP。
- 若要手填:抓包看 `STBRegister` 的 `307` 响应头 `Location: http://<VSP_HOST>:<端口>/EPG/XML/STBRegister`,取 `<VSP_HOST>`;或看盒子 `config.json` 里对应的 VSP 地址。

> 一句话:**填对 `BASE` 后,`VSP_HOST` 基本不用管**——它会被 307 重定向自动带出来。

> 填好后,`python verify_digest.py` 的 `[2][3][4]` 应全 `OK`、`[7]` 应解出明文密码,即说明配置正确。

---

## 🧪 离线自检

不发网络请求,复算中间值并比对(在仓库目录内):
```bat
python verify_digest.py
```
- `[1]`–`[6]` 复算 Digest 中间值(BASE64/HA1/MD5(body)/HA2/response),`[2][3][4]` 全 OK 即算法还原正确,含 20244 空格反例;
- `[7]` 从 `STBRegister <Password>` **离线解出密码**(AES-128-ECB),并反向加密验证密钥。

---

## 📄 文档

- [`docs/完整本地登录技术文档.md`](docs/完整本地登录技术文档.md) — 认证流程、加解密、请求构造、每步来源溯源、踩坑、附录。
- [`docs/Full_Local_Login_Technical_Document_EN.md`](docs/Full_Local_Login_Technical_Document_EN.md) — 英文版。
- [`docs/可复现实验记录.md`](docs/可复现实验记录.md) — 以某次成功运行为基准,5 个实验每步带完整中间值,可逐位复算比对。
- [`reverse-engineering/`](reverse-engineering/README.md) — **反编译源码**(算法来源依据):`cmcc_auth/` 认证进程 + `epg/` 回看查询。⚠️ 专有软件反编译产物,仅研究用;公开仓库建议设为私有或移除此目录。

---

## 📄 License

[MIT](LICENSE) — 详见 [LICENSE](LICENSE)。

> 本工具涉及对第三方系统认证流程的研究。请遵守当地法律法规及运营商服务条款,仅用于个人授权用途。
