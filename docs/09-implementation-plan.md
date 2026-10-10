# 实现计划（第二步）

在 `docs/` 相对稳定后再执行。编码助手必须按里程碑提交，不要一次生成无法运行的大而全项目。

## 进入第二步的前提

- 本目录需求 ID 无未解释冲突
- `U-01`（模型提供商）已决策并写入 `03-requirements.md` / `06-architecture.md`
- 仓库可用 git（若本机仍无 git，先安装再初始化）

## 里程碑

### 静态界面对照（先于 M0）

- `prototype/index.html`：问答页与知识页，对照 `07-ui-ux.md`
- 不选定框架、不安运行时依赖、不实现检索与 LLM
- 状态：已完成（2026-10-04）

### M0 脚手架

- 选定 Web 技术栈并回写 `06-architecture.md`「已选技术」
- 可启动的空问答页与空知识页
- `.env.example`、`.gitignore`
- 读取 `KNOWLEDGE_ROOT` 列出 published 文件

### M1 知识浏览

- Markdown 渲染
- `_index.md` 侧栏
- 路径限制在知识根内

### M2 索引与检索

- 按 `04-knowledge-model.md` 切块
- 全量重建索引
- 给定问题返回 top-k（可先做关键词，嵌入随后替换）

### M3 问答闭环

- 实现 `05-ai-qa-rules.md` 的 JSON 输出
- GroundingCheck
- UI 展示 citation 与三种 status
- 最小评测集 `docs/fixtures/qa-eval.md`

### M4 打磨

- 多轮（FR-08）
- 错误横幅与空状态
- README 更新为可运行说明

## 文档同步

每完成一个里程碑，在本文件对应小节末尾加一行：`状态：已完成（日期）`。行为变更先改 docs 再改代码。
