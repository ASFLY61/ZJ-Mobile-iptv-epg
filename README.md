# ZJ-Mobile-iptv-epg

> 浙江移动本地节目单获取工具:只需填 **STBID**(机顶盒序列号)——**EDS 入口已内置默认值**,在本地 Python 完成整套认证并拉取电视**节目单**
> 认证链路与密码解密算法均从盒子固件 / EPG 反编译精确还原。

- **UserID / 密码** 自动从 `STBRegister` 响应读取 / 解密
- **VSP 端口** 自动探测(7176–7200)
- **同区域换盒子只改 `STBID`**(EDS 入口不变)

---

## 特性

- **纯本地、自包含**:整个文件夹拷到任意 Windows 电脑即可运行
- **只需 STBID**:EDS 入口已内置,UserID 与密码全自动解出
- **回看模式(默认)**:今天全天 + 过去 6 天全天(复刻盒子回看)
- **live 模式**:今天(自当前节目起)+ 明天
- **批量导出**:一次登录拉全部 222 频道,导出 JSON / CSV / Excel
- **端口自动探测**:在 7176–7200 间自动找通

---

## 快速开始

**前提**:一台能访问目标 VSP 服务器的 Windows PC + Python 3.8+。

```bat
:: ① 首次: 双击 安装依赖.bat   ->  建 .venv, 装 pycryptodome + openpyxl (均可选)
:: ② 之后: 双击 运行纯本地登录.bat  ->  默认中央一套 + 今天(回看模式)
```

命令行(仓库目录内):
```bat
python full_local_login.py
```

> 首次运行会**提示填入 STBID**(EDS 已内置,一般不用改),填入后自动存到 `local_config.json`,后续直接跑。

---

## 用法

参数**顺序无关**,按类型自动识别:`4位7176-7200`=端口 / `YYYY-MM-DD`=日期 / 其它数字=频道 / `all` / `live` / `week` / `catchup`。

```bat
python full_local_login.py 42329916 2026-09-19      # 频道 + 日期(默认回看)
python full_local_login.py live 42329858             # live 模式
python full_local_login.py 42329858 week             # 周回看: 今天 + 过去6天
python full_local_login.py all 2026-09-19            # 全部222频道, 导出 CSV/Excel
python full_local_login.py catchup 42329858          # 列今天节目(带 playbillID)
python full_local_login.py catchup 42329858 <playbillID>   # 取该节目回看地址(TVOD, 可直接播)
python full_local_login.py catchup 42329858 2026-09-18 urls  # 一键批量: 当天全部已播节目 -> JSON+M3U
```

- 输出按 `频道_日期` 命名存到仓库目录:`playbill_<频道>_<日期>.json/.txt`;批量另出 `playbill_all_<日期>.json/.csv/.xlsx`(CSV 带 UTF-8 BOM,Excel 双击不乱码)。
- `42329858`=中央一套、`42329916`=黑龙江卫视、`42329207`=陕西卫视;完整 222 台见 `channel_ids.json`(含 mediaID)。

---

## 配置

仓库是**模板**:**只需填 `STBID`**(EDS 入口已内置默认值),其余自动推导。

| 占位符 | 含义 | 获取方式 |
|---|---|---|
| `YOUR_STBID_HERE` | 盒子序列号(**每盒必改**) | 盒子**背面标签**;或 `adb shell getprop ro.serialno` |
| `BASE`(已内置) | 认证入口 EDS | **已内置默认值**(浙江移动,同区域通用);若登录失败(区域不同)按下方方法获取 |
| `YOUR_VSP_IP` | VSP 服务器 IP | **可留占位符** —— 307 重定向自动推导 |
| `YOUR_PASSWORD_HERE` | 设备密码(兜底) | **可留占位符** —— 自动从 STBRegister 解密 |
| `YOUR_USERNAME_HERE` | 订购用户 ID(兜底) | **可留占位符** —— 自动从 STBRegister 读取 |
| `AES_KEY`(内置默认) | 解密 `<Password>` 的密钥 | 内置默认,换型号/区域才改 |

**若内置 EDS 不适用(登录失败),按以下方法获取正确的 EDS 入口,三选一:**

1. **`extract_base.py` 自动提取(有抓包文件时,推荐)**:`python extract_base.py capture.har`(支持 HAR/PCAP/代理日志),直接输出可粘贴的 `BASE`/`VSP_HOST`。
2. **盒子配置(免抓包)**:EPG 应用 `config.json` 的 `edsUrl` 字段就是 `BASE`。
3. **抓包**:盒子登录的第一个请求 `POST http://<EDS>:8082/EDS/STBRegister`,取它的 `host:port`。

> `BASE` 填对后,`VSP_HOST` 基本不用管——307 重定向会自动带出。

---

## 工作原理(5 步)

```
STBRegister → Digest Challenge → Digest Auth → ZJLogin → QueryPlaybill
   注册            取挑战            换 UserToken    建会话       拉节目单
```

| 步 | 端点 | 关键点 | 成功标志 |
|---|---|---|---|
| 0 | `POST /EDS/STBRegister` | 307 跳 VSP | `RsltCode=0` |
| 1 | `POST /EDS/pub/authentication_initial_challenge_v1` | 空 body | 返回 `WWW-Authenticate` |
| 2 | `POST /EDS/pub/authentication_initial` | Digest(MD5, auth-int) | `StatusCode=0` → UserToken |
| 3 | `POST /VSP/V3/ZJLogin` | **零请求头** + 当前端口 | `retCode=000000000` → JSESSIONID |
| 4 | `POST /VSP/V3/QueryPlaybillListStcProps` | 带 Cookie + token | `retCode=000000000` → 节目单 |

Digest 算法(反编译 `AuthManagerImpl.java` 还原):
```
HA1 = MD5(username:realm:BASE64(password))
HA2 = MD5("POST:/EDS/pub/authentication_initial:" + MD5(body))
response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
```

---

## 项目结构

```
.
├── full_local_login.py      # 主程序: 认证 + 拉节目单(端到端)
├── verify_digest.py         # 离线自检: 复算 Digest 中间值 + 解密码(不发网络请求)
├── probe_days.py            # 诊断: 实测两种模式的日期范围(需联网)
├── extract_base.py          # 从抓包文件自动提取 BASE / VSP_HOST(零依赖)
├── channel_ids.json         # 222 频道表(换区域替换此文件)
├── epg_channels.json        # 174 频道: 当天实测有节目数据的子集
├── 安装依赖.bat / 运行纯本地登录.bat
├── docs/                    # 技术文档(中/英)
└── reverse-engineering/     # 反编译源码(算法依据, 仅研究用)
```

---

## 文档

- [`docs/完整本地登录技术文档.md`](docs/完整本地登录技术文档.md) — 认证流程、加解密、请求构造、来源溯源、踩坑
- [`reverse-engineering/`](reverse-engineering/README.md) — 反编译源码(算法依据)。⚠️ 专有软件反编译产物,仅研究用;公开仓库建议设为私有或移除此目录

---

## 免责声明

- 本项目**仅供个人学习与研究**,用于访问**你自己设备、你自己订购**的服务。
- 使用者需自行承担使用本工具产生的一切后果,作者不对任何滥用负责。

## License

[MIT](LICENSE)
