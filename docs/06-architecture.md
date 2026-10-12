# 目标架构

本文约束第二步的模块划分，避免把逻辑堆在一个脚本里。仓库内 `prototype/index.html` 仅用于对照 `07-ui-ux.md`，不视为正式前端，也不替代下列模块。

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

| 方法与路径 | 用途 |
|------------|------|
| `GET /api/knowledge/toc` | `_index.md` 目录中已发布的条目，以及空知识库标识 |
| `GET /api/knowledge/docs` | 全部已发布文档（含未列入目录者） |
| `GET /api/knowledge/doc?path=` | 单篇已发布文档的元数据与 Markdown 正文（不含 front matter） |
| `POST /api/qa/ask` | 问答；请求含 `question` 与可选 `history` |
| `GET /api/qa/status` | LLM 是否已配置（不返回密钥或模型地址） |
| `GET /api/index/status` | 索引状态；含 `stale`：知识源相对当前索引是否已变化 |
| `POST /api/index/rebuild` | 手动全量重建 |

稳定错误码：`VALIDATION_ERROR`（400）、`INVALID_PATH`（400）、`DOC_NOT_FOUND`（404）、`NOT_FOUND`（404，未知 API 路径）、`LLM_NOT_CONFIGURED`（503）、`INDEX_NOT_READY`（503）、`LLM_ERROR`（502）、`LLM_INVALID_RESPONSE`（502）、`INDEX_REBUILD_FAILED`（500）、`INTERNAL_ERROR`（500）。

## 会话

- 会话历史只保存在浏览器内存；每次问答请求携带最近至多 10 轮（FR-08）。「新会话」清空历史。后端不保存会话。
- QueryPlanner 第一版为规则改写：问题含指代词（如「这个」「上一份」「它」）或过短时，把上一轮问题拼入检索查询。

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
| `LLM_BASE_URL` | 提供商 API 根地址，如 `https://api.openai.com/v1` 或本地 `http://localhost:11434/v1` |
| `INDEX_DIR` | 索引入库路径 |
| `QA_SHOW_RETRIEVAL` | 是否在问答响应中返回检索摘要（FR-07），默认 `true` |
| `RETRIEVAL_MIN_SCORE` | BM25 分数阈值，低于该值视为无依据，默认 `0.5` |

- `LLM_PROVIDER` 第一版支持 `openai-compatible`（OpenAI Chat Completions 兼容接口，可对接 OpenAI、DeepSeek、Ollama 等）。四个 `LLM_*` 变量全部填写才视为已配置。
- 后端启动时读取进程环境变量，以及工作目录或其上一级目录中的 `.env`（`KEY=VALUE` 格式）。
- 未设置 `KNOWLEDGE_ROOT` 时，依次尝试工作目录下的 `knowledge/` 与上一级的 `knowledge/`；未设置 `INDEX_DIR` 时，使用知识根目录同级的 `.local/index/`。

提供 `.env.example`，不提供填好密钥的 `.env`。

## 已选技术

- **前端：** TypeScript、React、Vite；React Router 负责页面路由，TanStack Query 负责 API 状态，Vitest 与 React Testing Library 负责测试。
- **后端：** Java 21、Spring Boot 3.5.16、Spring Web、Bean Validation、JUnit 5。
- **构建：** Maven Wrapper。开发时 Vite 代理 `/api` 到 Spring Boot；生产构建由 Maven 构建前端并将产物作为 Spring Boot 静态资源打包进可执行 JAR。
- **检索：** 第一版使用 Apache Lucene BM25；第二版再评估 embedding 与混合检索。
- **LLM：** 业务层定义独立提供商接口，不绑定唯一厂商；第一版不设置默认提供商，通过环境变量显式启用。
- **仓库目录：** `frontend/`、`backend/`、`knowledge/`、`docs/`、`prototype/`；运行时索引位于不入库的 `.local/`。
- **前端打包：** Maven `prod` profile 通过 `frontend-maven-plugin` 在 `frontend/node/` 下载项目私有的 Node.js（不入库）并执行构建；默认构建与后端测试不需要 Node.js。
- **中文分词：** Lucene `SmartChineseAnalyzer`，附加停用词表 `backend/src/main/resources/stopwords-zh.txt`（虚词、疑问词），避免「的」「是」之类的字让库外问题误命中。修改分析器或停用词时须提升索引 schema 版本，使旧索引在启动时自动重建。
- **Markdown 渲染：** 前端 `react-markdown` + `remark-gfm`，不渲染原始 HTML。
