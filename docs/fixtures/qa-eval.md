# 问答评测集（最小集）

对照 `05-ai-qa-rules.md`「评测」一节。每行：问题 / 期望 status / 必须引用的文件 / 禁止出现的短语（多个用 `、` 分隔）。

运行方式：

- **检索回归（无需模型，每次 `mvnw test` 都跑）：** `RetrievalEvalTest` 读取本表。期望 `grounded` 的行，必须引用的文件须出现在达到阈值的检索结果中；备注含「检索无命中」的行，不得有任何片段达到阈值。其余 `insufficient` 行只在端到端评测中校验。
- **端到端评测（需要已配置 LLM）：** 设置 `LLM_*` 环境变量后执行 `mvnw test -Dtest=QaEvalTest`。逐行调用问答，校验 status、引用文件与禁止短语。未配置时该测试自动跳过。

草稿文档不得被引用：真实知识库目前没有草稿，此项由集成测试 `ApiIntegrationTest.draftContentIsNeverRetrieved` 用临时知识库覆盖。

## 用例

| 问题 | 期望 status | 必须引用的文件 | 禁止出现的短语 | 备注 |
|------|-------------|----------------|----------------|------|
| 申请韩国签证对照片有什么要求？ | grounded | korea-visa-application-documents.md | | |
| 韩国签证要求护照剩余有效期多久？ | grounded | korea-visa-application-documents.md | | |
| 住民票需要多久以内签发的？ | grounded | korea-visa-application-documents.md | | |
| 在职证明需要写哪些内容？ | grounded | korea-visa-application-documents.md | | |
| 这个应用是做什么的？ | grounded | what-is-this-app.md | | |
| 怎么提问才能得到可靠的答案？ | grounded | how-to-ask.md | | |
| 韩国签证申请费是多少钱？ | insufficient | | 日元、韩元 | 知识库明确未写费用 |
| 火星的首都是哪里？ | insufficient | | | 库外，检索无命中 |
| 公司年假一共有几天？ | insufficient | | | 库外，但与已有文档有字面重合（「公司」「天」），由模型判定 |
