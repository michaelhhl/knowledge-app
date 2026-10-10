# 目标架构

第一版应用骨架尚未编写。仓库内 `prototype/index.html` 仅用于对照 `07-ui-ux.md`，不视为已选技术，也不替代下列模块。本文约束第二步的模块划分，避免把逻辑堆在一个脚本里。

## 形态

本机 Web 应用：

- **前端：** 问答页、知识浏览页。
- **后端：** 索引、检索、调用 LLM、返回结构化结果。
- **存储：** `knowledge/` 为源；索引可放在本地目录（如 `.local/index/`，gitignore）。

具体框架在实现第一步脚手架时选定，并回写本文「已选技术」一节。候选（未锁定）：TypeScript Web 栈（例如 Next.js 或独立 Vite + 轻量后端）。

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

- 源文件变更后需重建。第一版允许全量重建。
- 嵌入向量若使用云 API，只发送 chunk 文本，不发送密钥文件。
- 可同时保留关键词检索（如全文）作为嵌入失败时的回退（P1）。

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

（第二步开始时填写。未填写前不要假设某一框架已存在。）
