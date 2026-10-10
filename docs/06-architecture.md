# 目标架构

第一版应用骨架尚未编写。仓库内 `prototype/index.html` 仅用于对照 `07-ui-ux.md`，不视为已选技术，也不替代下列模块。本文约束第二步的模块划分，避免把逻辑堆在一个脚本里。

## 形态

本机 Web 应用：

- **前端：** 问答页、知识浏览页。
- **后端：** 索引、检索、调用 LLM、返回结构化结果。
- **存储：** `knowledge/` 为源；索引可放在本地目录（如 `.local/index/`，gitignore）。

开发环境中前后端分别启动；生产构建将前端静态资源打包进后端可执行 JAR，最终以单个 Java 进程运行。

## 逻辑模块

```
UI  →  QA API  →  QueryPlanner  →  Retriever  →  Index
                         ↓
                    AnswerComposer (LLM)
                         ↓
                    GroundingCheck
                         ↓
                    Response JSON
```

| 模块 | 职责 | 不得做 |
|------|------|--------|
| QueryPlanner | 结合短历史改写成检索查询 | 不发明事实 |
| Index | 读 Markdown、切块、嵌入或关键词索引 | 不调用 LLM 生成知识 |
| Retriever | top-k 与阈值 | 不把 draft 纳入 |
| AnswerComposer | 按 `05-ai-qa-rules.md` 调用模型 | 不直接读全库 |
| GroundingCheck | 无 citation 则强制 insufficient | 不「美化」无依据答案 |

## 索引

- 第一版使用 Apache Lucene BM25 关键词检索，不使用 embedding；embedding 推迟到第二版。
- 索引默认位于 `.local/index/`，可用 `INDEX_DIR` 覆盖，且不得提交到 git。
- 首次启动缺少索引时自动全量构建；后续启动通过文件清单（相对路径、大小、修改时间）判断知识源是否变化，变化时全量重建，否则直接加载。
- 第一版提供 `POST /api/index/rebuild` 手动重建与 `GET /api/index/status` 查看状态。
- 重建先写入临时目录，成功后原子替换旧索引；失败时保留旧索引并返回可读错误。

## API 约定

- HTTP API 根路径为 `/api`。
- 错误响应至少包含 `code` 与 `message`，并使用恰当的 HTTP 状态码。
- 问答响应遵循 `05-ai-qa-rules.md` 的结构化 JSON。
- 第一版不内置默认 LLM 提供商；未配置 LLM 时，知识浏览、索引与检索可用，问答 API 返回配置缺失错误。

## 知识文件访问安全

- API 只接受相对于 `KNOWLEDGE_ROOT` 的知识路径，拒绝绝对路径、盘符与 `..` 路径段。
- 后端使用 `Path.resolve()`、规范化和 `toRealPath()` 得到真实路径，并验证其仍位于真实知识根目录内，防止路径穿越与符号链接逃逸。
- 只允许读取索引登记的 `published` Markdown 文件；不得提供任意本机文件读取接口。

## 配置

环境变量示例名（实现时保持稳定）：

| 变量 | 含义 |
|------|------|
| `KNOWLEDGE_ROOT` | 知识根目录 |
| `LLM_PROVIDER` | 提供商标识 |
| `LLM_MODEL` | 模型名 |
| `LLM_API_KEY` | 密钥 |
| `INDEX_DIR` | 索引入库路径 |

提供 `.env.example`，不提供填好密钥的 `.env`。

## 已选技术

- **前端：** TypeScript、React、Vite；React Router 负责页面路由，TanStack Query 负责 API 状态，Vitest 与 React Testing Library 负责测试。
- **后端：** Java 21、Spring Boot 3.5.16、Spring Web、Bean Validation、JUnit 5。
- **构建：** Maven Wrapper。开发时 Vite 代理 `/api` 到 Spring Boot；生产构建由 Maven 构建前端并将产物作为 Spring Boot 静态资源打包进可执行 JAR。
- **检索：** 第一版使用 Apache Lucene BM25；第二版再评估 embedding 与混合检索。
- **LLM：** 业务层定义独立提供商接口，不绑定唯一厂商；第一版不设置默认提供商，通过环境变量显式启用。
- **仓库目录：** `frontend/`、`backend/`、`knowledge/`、`docs/`、`prototype/`；运行时索引位于不入库的 `.local/`。
