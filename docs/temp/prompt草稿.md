你现在是一位资深的 Full-Stack 代码审计专家和 QA 测试专家。

我需要你帮我检查项目中【传入文件并解析导入题库】这一核心功能的潜在 Bug、边界漏洞和鲁棒性问题。

涉及到的核心代码文件Java端如下：
- @src/main/java/cn/heycloudream/quiz_backend/controller/AiImportController.java
- @src/main/java/cn/heycloudream/quiz_backend/service/impl/AiQuestionImportServiceImpl.java
- @src/main/java/cn/heycloudream/quiz_backend/service/ai/AiImportStreamConsumer.java
- @src/main/java/cn/heycloudream/quiz_backend/service/ai/ImportIdempotentService.java
- @src/main/java/cn/heycloudream/quiz_backend/service/ai/AiImportTaskStatusStore.java
- @src/main/java/cn/heycloudream/quiz_backend/service/ai/AiQuestionImportRedisStatusStore.java
- @src/main/java/cn/heycloudream/quiz_backend/service/ai/RedisStreamTaskDispatcher.java
- @src/main/java/cn/heycloudream/quiz_backend/service/file/FileStorageService.java
- @src/main/java/cn/heycloudream/quiz_backend/service/prompt/AiQuestionImportSystemPromptProvider.java
- src/main/java/cn/heycloudream/quiz_backend/util/DocumentParseUtils.java
- @src/main/java/cn/heycloudream/quiz_backend/util/LlmJsonPayloadSanitizer.java
- @src/main/java/cn/heycloudream/quiz_backend/util/TaskIdGenerator.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/AiImportSubmitVO.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/AiImportTaskMetaVO.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/AiImportTaskStatusVO.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/QuestionPreviewVO.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/AiImportStatusVO.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/AiImportTaskResultVO.java
- @src/main/java/cn/heycloudream/quiz_backend/vo/ai/AiImportTaskResultVO.java

涉及到的核心代码文件Python端如下：
- @transf-python/main.py
- @transf-python/utils.py
- @transf-python/config.py
- @transf-python/prompts.py
- @transf-python/models.py
- @transf-python/utils.py
- @transf-python/config.py
- @transf-python/prompts.py

请从以下几个维度，深度分析这些文件在协作时可能出现的 Bug，并给出具体的修复建议：

0. 【核心数据流和逻辑】非常重要：请检查核心数据流和逻辑是否正确，是否存在阻塞或死锁，是否符合docs文件夹内文档的设计逻辑（严格遵循docs/background.md中的流程A,以及docs/AI解析服务Python端开发规约.md中的代码规约），并给出具体的修复建议。

1. 【文件上传与读取漏洞】：
   - 是否限制了非法文件类型（如上传了 .exe 后缀的伪造 Excel）？Java端需要限制，Python端不需要限制。
   - 大文件上传（如 100MB 压缩包或几十万行的表格）是否会导致内存溢出（OOM）？
   - 是否存在文件流未关闭、临时文件未删除导致的内存泄漏？

2. 【解析与数据校验漏洞】：
   - 如果文件内容为空、格式错乱、或者缺少必要列（如少了“标准答案”列），代码是否会崩溃（抛出 NullPointerException / Undefined）？
   - 特殊字符、Emoji、超长文本、HTML 标签传入时，解析和清洗逻辑是否健全？
   - 是否存在潜在的 XSS、SQL 注入（通过文件内容注入）或 Excel 宏注射漏洞？

3. 【业务与数据库写入问题】：
   - 题库导入通常涉及多条数据，如果第 50 条数据校验失败，前面的 49 条是回滚还是部分写入？（事务处理是否完善）
   - 如果用户重复导入同一个文件，系统是否有去重或覆盖逻辑？高并发下会不会产生死锁或重复插入？

请帮我列出所有发现的隐患，按照【严重程度：高/中/低】排序，并为每个高/中危隐患提供对应的修改代码示例。