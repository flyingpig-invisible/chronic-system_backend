# 慢病管理小程序 · 交付包（2026-11-17）

> 给队友：先看第 1 节「三分钟上手」，再按需看后面。
> 完整接口说明在 `01-文档/接口契约-v1.md`，字段有疑问以它为准。

---

## 1. 三分钟上手

### 后端（拿到就能跑，不用装 Maven）

```bash
cd 03-可执行jar
java -jar chronic-server-1.0.0.jar --spring.profiles.active=h2 --server.port=8080
```

- `--spring.profiles.active=h2`：用 H2 内存库，**不需要 MySQL 密码**，适合快速验证
- 接口根路径：`http://127.0.0.1:8080/api`
- 测试账号：`13800136688` / `123456`
- 自测全部接口：`node 02-后端源码/chronic-server/_tools/smoke.js` → 预期 `PASS 39 / FAIL 0`

### 用 MySQL 跑（正式口径）

```bash
# 改 application-mysql.yml 里的 password，或用环境变量
MYSQL_PASSWORD=你的密码 java -jar chronic-server-1.0.0.jar --server.port=8080
```

启动时自动建 `chronic` 库、建 24 张表、灌种子数据（种子来自小程序 `utils/mock.js`，所以数据和小程序演示时一模一样）。

### 改代码（要 Maven）

```bash
cd 02-后端源码/chronic-server
mvn clean package -DskipTests
```

---

## 2. 包里有什么

| 目录 | 内容 |
|---|---|
| `01-文档/接口契约-v1.md` | **最重要**。通用约定（鉴权/响应体/错误码/分页/ID 用字符串/单位）+ 40 个接口清单 + 完整 JSON 样例 |
| `01-文档/页面-接口映射与优先级.md` | 20 个页面各调哪些接口；P0/P1/P2 开发顺序 |
| `01-文档/样例数据.json` | 26 个接口的真实响应样例，可直接当 DTO / 单元测试夹具 |
| `02-后端源码/chronic-server/` | Spring Boot 3.3.5 / JDK 21，22 个 Java 文件 / 1472 行，24 张表 |
| `03-可执行jar/` | 打好的 fat jar（25MB），有 JDK 21 就能跑 |
| `04-前端已改动文件/` | 小程序里已经接上后端的那部分（见第 4 节），可直接覆盖到工程 |

---

## 3. 后端技术要点（改代码前先看这个）

- **不用 ORM**：`Repo` 封装 JdbcTemplate，查询结果统一把 `snake_case` 列名转 `lowerCamelCase`
- **`Views` 做字段整形**：`metric_key→key`、`record_value→value`、`time_text→time`、`isRead→read`、`description→desc`……所以返回的 JSON 和小程序 `mock.js` 的字段**完全一致**，前端渲染代码不用改
- **鉴权**：`Authorization: Bearer <token>`，token 是 HMAC-SHA256 自签（无第三方依赖），除登录/注册/验证码/找回密码外全部拦截，未登录返回 `{"code":401}`
- **统一响应**：`{code, msg, data}`，`code=0` 成功；错误码见契约文档
- **每次启动重建表**（`spring.sql.init.mode=always`），演示方便；要保留数据改成 `never`

接口一览见 `02-后端源码/chronic-server/README.md`。

---

## 4. 前端当前状态（重要，别以为全接完了）

### 已接后端（4 个页面 + 网络层，已验证）

| 文件 | 改动 |
|---|---|
| `utils/request.js` | 新增。统一请求层：`useMock` 开关、Bearer token、401 跳登录 |
| `utils/api.js` | 新增。40+ 接口函数，每个自带 mock 兜底 |
| `pages/login/login.js` | `api.login` → 存 token/uid → 跳角色页 |
| `pages/index/index.js` | `getHomeSummary` 一次取全；打卡 `updateTaskStatus`；下拉刷新走接口 |
| `pages/monitor/monitor.js` | `trendCache` 缓存 `getTrends`；手录血压 `addRecord` |
| `pages/mine/mine.js` | `onShow` 拉 `getProfile` + `getScoreDetail` |

**降级设计**：页面先用 `mock.js` 渲染，接口回来再覆盖，失败静默忽略 —— 所以**后端没启动时小程序照样能演示**，不会白屏。

### 还没接（16 个页面，仍是纯 mock）

`alert / guide / recipe / medication / plan / chat / article / device / emr / telemedicine / profile / role / register / forgot / bind-relative / role-switch`

它们的后端接口**全部已实现且自测通过**，只是前端没改成 `await api.xxx()`。每页改动量约 5～10 行，改法照抄 `pages/index/index.js` 的 `loadData()` 模式即可，对应接口查 `01-文档/页面-接口映射与优先级.md`。

### 联调配置

`utils/request.js` 里：

```js
baseUrl: { dev: 'http://127.0.0.1:8080/api', prod: 'https://你的域名/api' },
useMock: false,
```

- 微信开发者工具 → 详情 → 本地设置 → 勾「**不校验合法域名**」，否则 `http://127.0.0.1` 被拦
- 真机联调把 `127.0.0.1` 换成电脑局域网 IP（`http://192.168.x.x:8080/api`）
- 后端没起来时把 `useMock` 改回 `true`，纯本地演示

---

## 5. 已知边界（别当成 bug）

1. **医生问答是规则应答**：`ConsultController#answer` 用关键词命中 + 指南引用，没接大模型。换成 LLM 只改这一个方法。
2. **token 是 HMAC 自签**，没做刷新令牌 / 多端登出；生产建议换 JWT + Redis 黑名单。
3. **文件上传落本地** `./uploads`，单机可用，集群要换 COS/OSS。
4. **`/emr/list` 返回 `{total, list}`**，且每条是把预存 JSON 展开的对象，不是逐字段表结构。
5. **`/guide/categories` 返回 `[{name}]`、`/chat/quick-questions` 返回 `[{question}]`**（对象数组）。而小程序 `mock.js` 里这两处是纯字符串数组，所以接这两个页面时前端要取 `.name` / `.question`，或让后端改成返回字符串数组——**建议改后端**，一个 `List<String>` 的改动。

---

## 6. 待办

- [ ] 补 16 个页面接接口
- [ ] 用 MySQL 跑一遍验证（本包验证的是 H2，SQL 兼容但未实连）
- [ ] 医生问答接真实模型 + 换成可溯源的指南三元组
- [ ] 上线前把 `sql.init.mode` 改 `never`，改用 Flyway 迁移
