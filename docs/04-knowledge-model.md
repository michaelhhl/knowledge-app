# 知识模型（Markdown）

## 根目录

默认：`knowledge/`。

```
knowledge/
  _index.md          # 人工维护的阅读目录
  _schema.md         # 本目录格式的简短说明（给人看）
  *.md               # 知识篇
  <folder>/*.md      # 分类子目录，可嵌套一层或两层
```

## 一篇知识的结构

文件名：小写英文或拼音，用连字符，如 `onboarding-checklist.md`。不要用空格。

推荐 YAML front matter：

```yaml
---
id: kb-onboarding-checklist
title: 入职检查清单
tags: [hr, onboarding]
updated: 2026-10-04
status: published
---
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | 是 | 稳定 ID，重命名文件时尽量保持不变 |
| `title` | 是 | 展示标题；若缺省则用一级标题或文件名 |
| `tags` | 否 | 小写英文或中文短标签 |
| `updated` | 否 | `YYYY-MM-DD` |
| `status` | 否 | `published`（默认可检索）或 `draft`（不检索、不公开回答） |

正文：

- 使用 ATX 标题（`#` / `##` / `###`）。
- `##` 作为检索与引用的基本块边界。
- 列表、表格、代码块可用于步骤与命令；代码块不要当作「可执行依据」自动运行。
- 一篇只讲一个主题。跨主题拆文件，用相对链接互指。

## 索引 `_index.md`

给人类浏览，也给应用生成侧栏。格式：

```markdown
# 知识目录

- [入职检查清单](./onboarding-checklist.md)
```

未列入 `_index.md` 但 `status: published` 的文件 **仍可被检索**，只是不出现在精选目录。

## 切块（Chunk）规则

实现检索时按以下规则切块，便于测试：

1. 按 `##` 切分；若某节过长（超过约 1200 汉字或 800 英文词），再按 `###` 切。
2. 每个 chunk 元数据：`file`、`id`、`title`、`heading_path`、`tags`。
3. `status: draft` 的整篇跳过。
4. 忽略以下划线开头的元文件：`_index.md`、`_schema.md`。

## 引用回链

回答中的出处至少包含：

- `file`：相对于 `knowledge/` 的路径
- `heading`：最近的标题文本
- 可选：chunk 起始行号（实现有则提供）

## 禁止

- 在 Markdown 里内嵌密钥。
- 用 HTML 注释存放「给模型看、给人藏起来」的指令（提示词注入风险）。维护者指令写在 `docs/`，不写在知识正文里。
