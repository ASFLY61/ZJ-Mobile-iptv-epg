# 逆向工程参考(反编译源码)

本目录收录**反编译得到的关键源码**,用于说明 `full_local_login.py` 里各算法 / 参数的**来源与依据**。
这些文件从盒子内置的认证进程与 EPG 应用 APK 反编译(jadx)而来,**仅用于研究与教学**,请勿再分发或商用。

> 出于安全考虑,本目录内所有文件中的服务器域名、密钥等真实值均已替换为 `YOUR_*` 占位符。

---

## 来源

| 子目录 | 来源 | 反编译对象 |
|---|---|---|
| `cmcc_auth/` | 盒子内置 **CMCC_Auth** 认证进程(包名 `com.chinamobile.middleware.auth`) | 该 APK 的 `classes.dex` |
| `epg/` | **EPG 应用**(包名 `com.pukka.ydepg`) | 该 APK 的 `classes.dex` |

反编译工具:**jadx**(对 APK 的 `classes.dex` 反编译为 Java)。

---

## cmcc_auth/(认证进程)

| 文件 | 揭示了什么 |
|---|---|
| `AuthManagerImpl.java` | **核心**:HTTP Digest 认证(HA1 / HA2 / response 公式)、`ZJLogin` 零请求头、各端点 URL |
| `DataProcessing.java` | `decryptAes` **原生函数**绑定 —— 调用 `libdataprocessing.so` 解密 `<Password>` |
| `AESUtils.java` | AES 工具(用于 stbaccount 端点的密钥) |
| `AESUtil.java` | 另一套 AES 封装 |
| `QueryPresent.java` | stbaccount 查询 + 响应解密 |
| `QueryStbAccountResponse.java` | stbaccount 响应结构 |
| `HttpDigestTools.java` | Digest 头构造工具 |
| `Md5Util.java` | MD5 工具 |
| `Config.java` | 服务器 / 端点配置 |
| `UrlManager.java` | URL 构造 |
| `TokenManager.java` | UserToken 生命周期管理 |

## epg/(EPG 应用)

| 文件 | 揭示了什么 |
|---|---|
| `TVODProgramListPresenter.java` | **关键**:回看(catchup)节目单查询 —— `type=2` + `isFillProgram=1` + `sortType=STARTTIME:ASC` |
| `TVODPresenter.java` | 另一个回看 presenter(同样 `type=2`) |
| `QueryPlaybill.java` | 查询请求 bean(含 `type` / `isFillProgram` / `sortType` 等字段) |
| `QueryVODListBySubjectRequest.java` | `SortType.START_TIME_ASC = "STARTTIME:ASC"` 等常量 |
| `QueryChannel.java` | 频道查询字段(`channelIDs` / `contentType` / `isReturnAllMedia`) |
| `PlaybillLite.java` | 单条节目(开始/结束/名称/ID) |
| `PlaybillFilter.java` | 节目过滤字段 |
| `QueryPlaybillListResponse.java` | 节目单响应结构 |

---

## 关于 AES 密钥(`libdataprocessing.so`)

`DataProcessing.decryptAes` 是**原生函数**,实现在 ARM 二进制 `libdataprocessing.so` 里(不在 Java 层)。
`STBRegister` 的 `<Password>` 用 **AES-128-ECB + PKCS7** 加密,密钥为 16 字节,位于该 `.so` 的偏移 `0x7004`,
用"已知明文对"(某次抓到的密文 ↔ 已知明文密码)暴力滑窗定位得到。

> 出于安全考虑,**本仓库不直接收录该 `.so` 二进制**(它内嵌真实密钥)。
> 获取方式:盒子内 `/system/app/<EPG>/lib/arm/libdataprocessing.so`,
> 用 `objdump` / `radare2` 查看偏移 `0x7004` 处的 16 字节即为密钥(同一型号/区域盒子相同)。

---

## ⚠️ 法律与用途

- 本目录是**专有软件的反编译产物**,仅用于**个人研究、学习与互操作**,不构成对原软件的修改或再分发。
- 本项目与**中国移动 / 华为 / Pukka 无任何关联**;内容不保证准确、完整或最新。
- 使用者需**自行承担**使用本目录内容产生的一切法律风险,并遵守当地法律法规及原软件的许可协议。
- **若将本仓库公开发布,建议设为私有,或移除本目录(`reverse-engineering/`)。**
