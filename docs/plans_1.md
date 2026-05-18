# 📑 智能在线刷题平台 - 项目落地路径与架构规划

作为具有大厂经验的架构师和面试官，我为你量身定制了这份项目拆解与落地计划。这份计划不仅关注代码实现，更从 **“如何将技术点转化为面试亮点”** 的角度出发，帮助你打造一份无懈可击的实习简历项目。

---

## 阶段一：需求深化与架构设计 (Week 1)
**目标：** 确立项目的骨架，完成数据库表设计与 API 规范制定。

### 📋 Todo-List:
- [x] **数据库建模 (MySQL)：** 设计核心表结构结构，包括 `用户表 (user)`、`题库表 (question_bank)`、`试题表 (question)`、`错题本表 (wrong_question)`。
- [x] **API 规范设计：** 确定 RESTful 接口规范，定义通用的泛型响应体 `Result<T>`。
- [x] **技术选型确认：** 确保 Java 17 + Spring Boot 3.x 开发环境完备，安装并启动 MySQL 8.x 和 Redis。

### 💡 排坑指南 (Architect's Note):
> **⚠️ 坑点提示：** 表设计时容易忽略扩展性。例如试题的内容（题干、选项、解析）可能非常长，务必使用 `TEXT` 或 `VARCHAR(2000)` 以上的数据类型。此外，时间字段统一使用 `DATETIME` 或 `TIMESTAMP`，避免前后端传参出现时区不一致问题（建议配置 Jackson 的全局时间格式化）。

---

## 阶段二：基础环境与脚手架搭建 (Week 1)
**目标：** 搭建企业级应用骨架，规范代码书写，这是体现你代码素养的第一步。

### 📋 Todo-List:
- [x] 初始化 Spring Boot 工程，引入 MyBatis-Plus、Redis、JWT 等依赖。
- [x] 配置 Knif4j (Swagger) 生成在线接口文档。
- [x] 编写全局异常处理器 `@RestControllerAdvice` 和自定义业务异常类库。
- [x] 划分项目包结构（Controller, Service, Mapper, Entity, DTO, VO）。

### 💼 面试考点 / 简历亮点:
- **⭐ 企业级规范包装：** 面试官非常看重应届生的代码规范。这里可以提炼为：**“基于 DTO/VO 实现严格的防溢出分层架构，并封装全局异常处理与统一结果拦截器，提升了系统的健壮性并规范了团队协同开发。”**

### 💡 排坑指南 (Architect's Note):
> **⚠️ 坑点提示：** Spring Boot 3.x 已经从 `javax.*` 迁移到了 `jakarta.*`，在引入第三方依赖时（如 Servlet、校验框架 validation）极易发生包冲突报错。务必注意相关依赖的版本匹配。

---

## 阶段三：用户鉴权与基础业务开发 (Week 2)
**目标：** 完成无状态登录验证，打通最基本的增删改查闭环。

### 📋 Todo-List:
- [x] 实现基于 BCrypt 的用户密码加密存储与校验。
- [x] 实现 JWT 签发与解析逻辑，基于 Spring 的 `Interceptor` (拦截器) 实现接口的安全鉴权。
- [x] 完成题库（QuestionBank）及其关联试题的 CRUD（增删改查）接口。

### 💼 面试考点 / 简历亮点:
- **⭐ 分布式会话设计：** **“摒弃传统的 Session 机制，采用 JWT 实现无状态登录，解决了后续微服务化或多实例部署时的 Session 共享痛点。”**

### 💡 排坑指南 (Architect's Note):
> **⚠️ 坑点提示：** 直接使用 JWT 会遇到“Token 无法主动拉黑/失效”的问题。如果要在真正的大厂落地，通常会在 Redis 中缓存一份 JWT 的名单（黑白名单）结合双 Token (Access/Refresh) 机制。现阶段可以简单实现，但在面试时你要能说出这个优化方向。

---

## 阶段四：AI 智能题库导入模块（核心业务与壁垒） (Week 3)
**目标：** 攻克项目中含金量最高的功能——利用大模型解决非结构化数据清洗问题。

### 📋 Todo-List:
- [x] 集成 PDFBox/Apache POI 工具类，实现 PDF/Word 文件的纯文本抽取。
- [x] 实现文件上传端点，支持 .txt/.pdf/.docx 直接上传导入。
- [x] 编写并调试 LLM 提示词 (Prompt)，使模型稳定输出包含（题干、选项、答案、解析）的标准 JSON 数组格式。
- [x] 对接 DeepSeek 大模型的 REST API，处理 HTTP 请求与响应。
- [x] 使用 Spring 的 `@Async` 或配置线程池，实现文件的**异步解析**，防止前端长时间等待导致 HTTP 超时。
- [x] 批量解析完成后，结合 MyBatis-Plus 提供的 `saveBatch` 进行批量入库。

### 💼 面试考点 / 简历亮点:
- **⭐ 大模型在业务场景的落地：** **“为了解决传统正则解析文档容错率低（低于 40%）的痛点，引入 LLM 多模态清洗方案，通过精细化 Prompt 工程，将非结构化题库转换为结构化 JSON，解析成功率达 95% 以上。”**
- **⭐ 异步任务与线程池：** **“针对大模型接口耗时较长（10s-30s）的问题，采用 `@Async` 自定义线程池实现任务的异步非阻塞处理，前端轮询/WebSocket 获取结果，极大地提升了用户体验。”**

### 💡 排坑指南 (Architect's Note):
> **⚠️ 坑点提示：** 
> 1. 大模型 API 有时会“胡说八道”或返回不合法的 JSON 形态（比如多返回了 markdown 的 ````json` 标签）。你需要编写强壮的 JSON 格式化工具进行字符串清洗。
> 2. PDF 文件可能过大，触发 JVM 的 OOM，或者超出了大模型的 Token 限制。建议对文件做大小限制或分块提问（Chunking）。

---

## 阶段五：高并发刷题与错题本模块（性能优化） (Week 4)
**目标：** 解决期末周突发性的高并发访问问题，这也是简历进阶包装的重点。

### 📋 Todo-List:
- [x] 编写在线刷题功能接口：`GET /api/v1/practice/banks/{bankId}/questions` 获取题库刷题列表，`POST /api/v1/practice/banks/{bankId}/questions/{questionId}/submit` 提交单题答案并判分。
- [x] 建立错题本闭环：`GET /api/v1/wrong-questions` 分页查看错题，`DELETE /api/v1/wrong-questions/{id}` 移除错题，`GET /api/v1/wrong-questions/practice` 按错题本重刷。
- [x] 复用 Redis 热点题库缓存：公开题库刷题列表复用 `QuestionBankHotDetailService` 的 Cache-Aside 缓存；私有题库做归属校验后直接查 DB。
- [x] 补充阶段五 Service 单元测试：`PracticeServiceImplTest`、`WrongQuestionServiceImplTest` 覆盖拉题、判分、错题记录、复活与移除等核心路径。
- [ ] 补齐阶段五 Controller/集成测试，并完成 JMeter/Apifox 压测报告。

### ✅ 当前实现说明:
- **刷题模式拉题**：`PracticeQuestionVO` 故意不返回 `answerJson` 与 `analysis`，防止用户提交前直接看到答案。公开题库允许登录用户刷题并复用热点缓存；私有题库必须是题库所有者。
- **提交答案判分**：`AnswerSubmitDTO.userAnswer` 接收答案列表。`SINGLE` / `JUDGE` 比较首个答案并忽略大小写；`MULTI` 排序后全量比较。判分响应 `AnswerSubmitResultVO` 返回 `correct`、`needsManualGrading`、标准答案与解析。
- **主观题边界**：主观题或未知题型暂不自动判分，返回 `needsManualGrading=true` 且 `correct=null`，不会自动加入错题本。
- **错题自动收集**：客观题答错后调用 `WrongQuestionService.recordWrong()`。首次做错执行 INSERT；已存在则递增 `wrong_count`；已移除的错题再次做错会将 `is_deleted` 复活为 0 并递增次数，避免同一用户同一试题重复 INSERT。
- **错题本重刷**：错题分页与重刷列表均不暴露答案。前端从 `GET /api/v1/wrong-questions/practice` 获取错题刷题列表后，仍复用刷题提交接口完成判分。
- **当前限制**：错题分页的 `bankId` 过滤在 Service 层基于当前页记录做 Java 过滤，数据量增大后应改为自定义 SQL/JOIN，以获得更准确的全量分页与 `total`。

### 💼 面试考点 / 简历亮点:
- **⭐ 刷题读路径与缓存复用：** **“刷题模式需要高频读取整套公开题库，因此公开题库拉题直接复用热点题库详情的 Redis Cache-Aside 链路，在不重复建设缓存体系的前提下降低 MySQL 读压力；私有题库保持强归属校验，避免越权访问。”**
- **⭐ 错题本幂等记录设计：** **“错题本表通过 `(user_id, question_id)` 唯一约束支撑业务幂等：首次做错插入，重复做错只递增次数，用户移除后再次做错执行复活更新，避免重复行导致统计和重刷列表失真。”**
- **⭐ 判分结果边界清晰：** **“客观题由服务端自动判分并触发错题收集，主观题明确标记为需人工判分，不为了功能完整性伪造 AI/规则判分，保证当前能力边界真实可靠。”**

### 💡 排坑指南 (Architect's Note):
> **⚠️ 坑点提示：**
> 1. **缓存一致性：** 管理员修改了题库，Redis 里的缓存要怎么更新？（经典的 Cache Aside 策略：先更新数据库，再删除缓存）。面试官最爱抓着这个问。
> 2. **缓存穿透/击穿：** 当前热点公开题库详情已使用手动 Cache-Aside、空值占位和互斥锁保护；新增缓存场景时应沿用该模式，不要混入 Spring Cache。
> 3. **分页过滤准确性：** 错题本按 `bankId` 过滤当前在 Java 层完成，数据量上来后应下沉到 SQL，避免分页后过滤导致某些页记录偏少。

---

## 阶段六：测试、压测与部署上线 (Week 5)
**目标：** 补足工程化的最后一步，将项目跑在云端。

### 📋 Todo-List:
- [ ] 编写主要 Controller 与 Service 的单元测试（阶段五 Service 核心路径已有 Mockito 测试，仍需补 Controller/集成测试）。
- [ ] 使用 JMeter/Apifox 对核心接口（有无 Redis 缓存）进行压力测试，并保存对比截图留作简历素材。
- [ ] 编写 Dockerfile。将应用打包为 Docker 镜像，通过 Docker-Compose 部署数据库、Redis 和后端服务到云服务器。

### 💼 面试考点 / 简历亮点:
- **⭐ 闭环的工程化能力：** **“使用 Docker 容器化技术实现了项目的统一部署。使用 JMeter 进行全链路压测，成功找出性能瓶颈并用 Redis 消除，体现了数据驱动性能调优的意识。”**

### 💡 排坑指南 (Architect's Note):
> **⚠️ 坑点提示：** 对于 1核2G/2核4G 的低配学生机，Spring Boot + MySQL + Redis 很容易吃满内存导致某个容器挂掉（OOM Killed）。注意在 Docker 中限制好应用的堆内存（`-Xms256m -Xmx512m`）。