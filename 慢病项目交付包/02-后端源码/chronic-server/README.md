# chronic-server · 慢病管理小程序后端

配套 `patient-miniprogram` 的 Spring Boot 后端。按 `慢病小程序-前后端协作/接口契约-v1.md` 实现，**字段名与前端渲染完全一致**，前端只需关掉 mock 开关。

- Spring Boot 3.3.5 / JDK 21 / MySQL 8（本地也可用 H2 内存库跑）
- 统一响应体 `{code, msg, data}`，`code=0` 成功
- 鉴权：`Authorization: Bearer <token>`，除登录/注册/验证码/找回密码外全部拦截
- 种子数据由小程序 `utils/mock.js` 自动生成（`_tools/gen_seed.js`），共 24 张表

---

## 一、怎么跑

### 方式 A：连本地 MySQL（推荐，正式口径）

1. 先确认 MySQL 密码，改 `src/main/resources/application-mysql.yml` 的 `password`，或用环境变量：

```bash
MYSQL_PASSWORD=你的密码
```

2. 启动（会自动建 `chronic` 库、建表、灌数据，`createDatabaseIfNotExist=true`）：

```bash
java -jar target/chronic-server-1.0.0.jar
```

或开发模式：

```bash
mvn spring-boot:run
```

### 方式 B：H2 内存库（不用 MySQL 密码，30 秒验证）

```bash
java -jar target/chronic-server-1.0.0.jar --spring.profiles.active=h2
```

> 注意：`spring.sql.init.mode=always`，**每次启动都会重建表并回到种子状态**，演示用足够；要保留数据把它改成 `never`。

### 启动后

- 接口根路径：`http://127.0.0.1:8080/api`
- 测试账号：`13800136688` / `123456`

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800136688\",\"password\":\"123456\"}"
```

### 一次性自测全部接口

```bash
node _tools/smoke.js
```

会把 40 多个接口挨个打一遍，输出 `PASS / FAIL` 汇总。

---

## 二、接口清单（全部已实现）

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 认证 | POST | `/auth/login` | 手机号+密码，返回 token / userId / role / roles |
| | POST | `/auth/register` | 注册并自动登录 |
| | POST | `/auth/sms/code` | 开发环境固定返回 devCode=123456 |
| | POST | `/auth/password/forgot` | 重置密码 |
| | GET | `/auth/roles` | 角色列表 |
| | POST | `/auth/role/switch` | 切换角色（校验权限） |
| 患者 | GET | `/patient/profile` | 档案（含嵌套 doctor、diseases 数组） |
| | PUT | `/patient/profile` | 改档案（白名单字段） |
| | GET | `/patient/family` | 亲属列表 |
| | POST | `/patient/family/bind` | 绑定亲属 |
| | DELETE | `/patient/family/{id}` | 解绑 |
| 首页 | GET | `/home/summary` | 聚合：patient + risk + todayMetrics + todayTasks + quickActions |
| 监测 | GET | `/monitor/trends?type=&range=` | 趋势（labels/series/stats/target） |
| | GET | `/monitor/records?type=&pageNum=&pageSize=` | 记录分页 |
| | POST | `/monitor/records` | 录入一条测量 |
| | GET | `/monitor/disease-tabs` | 病种 Tab |
| 预警 | GET | `/alert/summary` | 高/中/低数量 |
| | GET | `/alert/list?level=&pageNum=` | 预警分页 |
| | PUT | `/alert/{id}/read` | 标记已读 |
| 用药 | GET | `/medication/list` | 用药清单 |
| | POST | `/medication/checkin` | 打卡（重复打卡返回 2001） |
| 计划 | GET | `/plan/today` | 今日任务 + 医生计划 |
| | PUT | `/plan/task/{id}/status` | done / skip |
| 食谱 | GET | `/recipe/list?tag=` | 分页 + 标签筛选 |
| | GET | `/recipe/{id}` | 详情 |
| 指导 | GET | `/guide/categories` | 分类 |
| | GET | `/guide/list?category=` | 内容列表 |
| 文章 | GET | `/article/list` | 分页 |
| | GET | `/article/{id}` | 正文 |
| 设备 | GET | `/device/list` | 已绑设备 |
| | POST | `/device/bind` | 绑定 |
| | DELETE | `/device/{id}` | 解绑 |
| 病历 | GET | `/emr/list` | 病历列表（`{total,list}`） |
| | POST | `/emr/upload` | 提交（需先传文件拿 url） |
| 问答 | POST | `/chat/send` | 提问，返回 answer + citations + safetyTip |
| | GET | `/chat/history?sessionId=` | 历史会话 |
| | GET | `/chat/quick-questions` | 快捷问题 |
| 远程医疗 | GET | `/telemedicine/doctors?dept=` | 医院/医生 |
| | POST | `/telemedicine/appointment` | 预约 |
| 公共 | POST | `/common/upload` | 文件上传（multipart，返回 url） |
| | GET | `/common/dict/{type}` | 字典 disease / relation / dept |

错误码：`0` 成功、`401` 未登录、`403` 无权限、`1001` 参数错、`1004` 资源不存在、`2001` 业务规则不允许、`5000` 服务端异常。

---

## 三、和前端联调

改小程序 `utils/request.js` 顶部两行即可：

```js
baseUrl: { dev: 'http://127.0.0.1:8080/api', prod: 'https://你的域名/api' },
useMock: false,
```

注意：小程序开发者工具需要勾选 **「不校验合法域名」**，真机必须用已备案的 HTTPS 域名。

手机和电脑联调时，`127.0.0.1` 要换成电脑的局域网 IP（`http://192.168.x.x:8080/api`）。

---

## 四、目录

```
慢病后端-springboot/
├─ pom.xml
├─ src/main/java/com/gzu/chronic/
│  ├─ Application.java
│  ├─ common/   R、Repo（行->camelCase + JSON 解析）、Views（字段整形）、P（参数工具）、异常
│  ├─ security/ TokenUtil（HMAC token）、AuthInterceptor
│  ├─ config/   WebConfig（拦截器/CORS/上传静态映射）
│  └─ controller/  Auth / Patient / Home / Monitor / Alert / Medication / Plan / Content / DeviceEmr / Consult / Common
├─ src/main/resources/
│  ├─ application.yml / application-mysql.yml / application-h2.yml
│  └─ db/schema.sql（24 表）+ db/data.sql（种子）
└─ _tools/
   ├─ gen_seed.js   从 mock.js 生成 data.sql
   └─ smoke.js      40+ 接口自测
```

## 五、已知边界（答辩时主动说，别被问倒）

1. **医生问答是规则应答**（关键词命中 + 指南引用），没接大模型；替换 `ConsultController#answer` 一个方法即可换成 LLM。
2. **token 是 HMAC 自签**，未做刷新令牌与多端登出；生产建议换 JWT + Redis 黑名单。
3. **每次启动重建表**（`sql.init.mode=always`），演示/联调方便，生产要改成 Flyway 增量迁移。
4. **文件上传落本地磁盘** `./uploads`，单机可用；集群要换 COS/OSS。
