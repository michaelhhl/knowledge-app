# knowledge-app

基于 AI 的问答知识应用。知识以 Markdown 文件为唯一事实来源，程序代码由这些文档驱动生成。

## 当前阶段

**第一步（进行中）：** 以 `docs/` 与 `knowledge/` 为准。允许对照界面的静态页 `prototype/index.html`，不安装运行时依赖、不写应用脚手架。

**第二步：** 严格按 `docs/` 生成程序。实现时以文档为准，不以猜测为准。

## 文档入口

从 [docs/00-index.md](docs/00-index.md) 按顺序阅读。

## 仓库约定

| 路径 | 用途 |
|------|------|
| `docs/` | 产品需求、规则、架构、实现计划 |
| `knowledge/` | 面向最终用户的知识库 Markdown |
| `AGENTS.md` | 给编码助手的执行约束 |
| `src/` 等 | 第二步才创建 |

## 本机说明

创建仓库时本机 PATH 中未检测到 `git`。安装 Git 后可在本目录执行 `git init`。
