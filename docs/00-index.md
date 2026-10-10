# 文档索引

本目录是产品与实现的唯一规格。第二步生成代码时按此顺序理解项目。

## 阅读顺序

| 序号 | 文件 | 回答的问题 |
|------|------|------------|
| 1 | [01-vision.md](01-vision.md) | 做什么、不做什么、成功标准 |
| 2 | [02-personas-and-scenarios.md](02-personas-and-scenarios.md) | 谁在用、典型场景 |
| 3 | [03-requirements.md](03-requirements.md) | 功能与非功能需求（可验收） |
| 4 | [04-knowledge-model.md](04-knowledge-model.md) | Markdown 如何组织、如何被检索 |
| 5 | [05-ai-qa-rules.md](05-ai-qa-rules.md) | 模型如何检索、回答、引用、拒答 |
| 6 | [06-architecture.md](06-architecture.md) | 目标技术结构（第二步才落地） |
| 7 | [07-ui-ux.md](07-ui-ux.md) | 界面与交互 |
| 8 | [08-data-and-privacy.md](08-data-and-privacy.md) | 数据、隐私、安全边界 |
| 9 | [09-implementation-plan.md](09-implementation-plan.md) | 如何从文档生成代码、里程碑 |
| 10 | [10-coding-standards.md](10-coding-standards.md) | 代码风格与仓库纪律 |

## 知识库样例

面向用户的内容在仓库根目录 [`../knowledge/`](../knowledge/)。规格见知识模型文档。

## 文档维护规则

- 用中文撰写，专有名词可保留英文。
- 需求用稳定 ID（如 `FR-01`），不要删 ID 后复用给别的含义。
- 过时条款标为「已废弃」并写替代 ID，不要静默删除历史 ID。
- 一次只改与当前决策相关的文档，避免空泛扩写。
