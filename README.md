# knowledge-app

基于 AI 的问答知识应用。知识以 Markdown 文件为唯一事实来源，程序代码由这些文档驱动生成。

## 当前阶段

**第二步（已实现第一版）：** 按 `docs/` 生成的程序位于 `backend/` 与 `frontend/`，里程碑状态见 [docs/09-implementation-plan.md](docs/09-implementation-plan.md)。行为变更仍须先改 `docs/` 再改代码。

## 文档入口

从 [docs/00-index.md](docs/00-index.md) 按顺序阅读。

## 仓库约定

| 路径 | 用途 |
|------|------|
| `docs/` | 产品需求、规则、架构、实现计划 |
| `knowledge/` | 面向最终用户的知识库 Markdown |
| `backend/` | Spring Boot 后端：知识读取、Lucene 索引、检索、问答 API |
| `frontend/` | React + Vite 前端：问答页、知识库页 |
| `prototype/` | 对照 `07-ui-ux.md` 的静态页，不是正式前端 |
| `AGENTS.md` | 给编码助手的执行约束 |

## 运行

前置条件：JDK 21。Maven 与 Node.js 不需要预先安装：后端使用 Maven Wrapper，生产构建会把 Node.js 下载到 `frontend/node/`（已 gitignore）。

### 1. 配置

```powershell
Copy-Item .env.example .env
```

填写 `.env` 中的 `LLM_PROVIDER`、`LLM_MODEL`、`LLM_API_KEY`、`LLM_BASE_URL`（说明见 `.env.example`）。不填写也能启动：知识浏览、索引与检索可用，问答会提示未配置 LLM。

### 2. 构建并以单个 Java 进程运行

```powershell
cd backend
.\mvnw.cmd -Pprod package
cd ..
java -jar backend\target\knowledge-app.jar
```

打开 <http://localhost:8080>。端口可用环境变量 `PORT` 修改。请在仓库根目录启动，以便自动找到 `knowledge/` 与 `.env`。

### 3. 开发模式（前后端分别启动）

```powershell
# 终端 1：后端（http://localhost:8080）
cd backend
.\mvnw.cmd spring-boot:run

# 终端 2：前端（http://localhost:5173，/api 代理到 8080）
cd frontend
npm install
npm run dev
```

本机没有 Node.js 时，可先执行一次 `backend\mvnw.cmd -Pprod package`，再把 `frontend\node` 加入 PATH 后使用 `npm`。

## 测试

```powershell
cd backend
.\mvnw.cmd test            # 后端单元、集成与检索回归测试

cd ..\frontend
npm test                   # 前端组件测试
npm run typecheck
```

端到端问答评测需要真实模型：配置 `LLM_*` 环境变量后执行 `.\mvnw.cmd test -Dtest=QaEvalTest`，用例见 [docs/fixtures/qa-eval.md](docs/fixtures/qa-eval.md)。

## 维护知识

1. 按 [docs/04-knowledge-model.md](docs/04-knowledge-model.md) 在 `knowledge/` 新建或修改 `.md`。
2. 需要出现在目录时，更新 `knowledge/_index.md`。
3. 在页面上点「重建索引」（或重启应用）。索引过期时页面顶部会提示。

## 本机说明

当前 PATH 中未检测到 `git`。安装 Git 后即可按里程碑提交。
