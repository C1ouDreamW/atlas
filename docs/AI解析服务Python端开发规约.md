# Atlas AI解析服务 Python 端 (Worker) 开发规约

## 1. 架构定位
在解耦后的旁路异步架构下，Python 端作为完全独立、无状态的 Worker 节点运行，且**不需要提供任何 HTTP Web/REST API 接口**（无需 Flask/FastAPI）。整个程序的生命周期应该是一个连接 Redis 的常驻守护进程（死循环拉取）。
所有的交互（领任务、报状态、交结果）均通过读写规约好的 Redis Key 实现。

---

## 2. 核心配置与规约对齐（必须严格遵从）

### 2.1 Redis 数据结构约定
| 描述 | Redis 数据结构/指令 | 键名约定 (Key) | 数据/状态值示例 | 备注/TTL设置 |
| :--- | :--- | :--- | :--- | :--- |
| **异步任务队列** | `Stream` | `quiz:task:stream` | `{"taskId": "xxx", "fileUrl": "/.../a.pdf", "fileType": "pdf"}` | 流的最大长度通常由 Java 端限制 |
| **消费者组** | `Consumer Group` | `quiz-ai-workers` | `<无>` | 用于同组 Python 实例抢单，避免重复解析 |
| **任务状态** | `String` | `quiz:task_status:{taskId}` | `"PROCESSING"` / `"PARSED"` / `"FAILED"` | 写入状态时，务必携带TTL (建议至少1800秒) |
| **任务结果** | `String` | `quiz:task_result:{taskId}` | `[{干净的JSON数组}]` | 存放最终大模型输出，务必携带TTL (建议1800秒) |

### 2.2 状态流转标识约定 (Enum Enum 对齐 Java 端)
- `PENDING`：(Java 端写入) 任务已入队，待接单。
- `PROCESSING`：(**Python 端写入**) Worker 已接单并开始解析。
- `PARSED`：(**Python 端写入**) 正常完结，JSON 结果已生成并放入 result_key。
- `FAILED`：(**Python 端写入** / 或是 Java 看门狗强制写) 解析中途彻底崩溃或模型熔断。

---

## 3. 标准操作工作流与实现细节

Python Worker 的主逻辑应按照以下 5 个步骤构成一个完整的处理环路：

### 第 1 步：监听并领取任务 (XREADGROUP)
Python 端启动时，应先尝试创建消费组（忽略已被创建的报错），然后开启阻塞拉取。
- **操作**：使用 `XREADGROUP GROUP quiz-ai-workers worker_node_1 BLOCK 5000 STREAMS quiz:task:stream >`
- **解析消息内容**：从取到的 Message 中解析字典：
  - `taskId`: 后续更新状态的所有前缀 ID。
  - `fileUrl`: 需要去读取的文件路径。当前阶段通常是 Java 端落盘的**本地绝对路径**（如 `/data/upload/xxx.pdf`）或是将来拓展的 **MinIO/OSS URL**。
  - `fileType`: `pdf`, `docx`, `txt` 等。

### 第 2 步：声明接单心跳 (防死信误杀)
拿到任务后，务必在开始重度物理读取前更新生命状态，防止 Java 端看门狗（Watchdog，10分钟阈值）将任务判作超时。
- **操作**：执行 `SET quiz:task_status:{taskId} "PROCESSING" EX 600`。
- **注意**：如果遇到百页超大文档（预计超过10分钟），Python 应通过后台守护线程，定时续期这个状态的过期时间（TTL）。

### 第 3 步：获取文件并执行深度抽取
根据获取到的 `fileUrl` 指引去拿到真正的文件字节流（本地 `open(fileUrl)` 或通过 `requests.get(fileUrl)` 拉取 MinIO 流）。
- **操作**：
  ~~- .docx 推荐使用 `python-docx`。~~
  ~~- .pdf 推荐使用 `pdfplumber` 或 `PyMuPDF` 获取文本（必要时接入 OCR 图片文字穿透处理）。~~
  
  弃用传统识别方式，直接集成 MinerU API（ https://mineru.net/apiManage/docs ） 高质量转换成md文本。

### 第 4 步：调用大模型核心转换
将提取的文本输入给 DeepSeek / Kimi 等模型，使用严苛的 System Prompt 约束其提取试题。
- **操作与重试**：如果LLM返回网络超时，应当做局部 Exponential Backoff（指数退避）重试。
- **结果清洗**：大模型往往无法管住自己，可能会在 JSON 前后加上 \`\`\`json 的 Markdown 代码围栏。Python 必须使用正则或原生的 `.strip()` 将其剥除，并通过 `json.loads(text)` 严格验证其合不合法。**如果抛出 JSON 解析错误，必须进入 Retry 让大模型重写。**

### 第 5 步：数据返回与消息确认 (交接闭环)
一切顺利后，向 Redis 提交最后的结果，并清除队列中待确认的消息。
- **成功逻辑**：
  1. 将数据写入结果区：`SET quiz:task_result:{taskId} "{序列化后的字符串}" EX 1800`
  2. 更改为成功状态：`SET quiz:task_status:{taskId} "PARSED" EX 1800`
  3. **ACK 操作**：`XACK quiz:task:stream quiz-ai-workers <拉取时的Message_ID>`
- **失败兜底逻辑 (Try-catch 捕捉外层异常)**：
  如果抛出无法恢复的错误（文件损坏、大模型全挂），必须：
  1. 更改为失败状态：`SET quiz:task_status:{taskId} "FAILED" EX 1800`
  2. **强制 ACK 操作**：`XACK quiz:task:stream quiz-ai-workers <Message_ID>`。如果不 ACK，这条消息会被当作死信永远卡在 Pending 队列被无限轮询重试。

---

## 4. 交付的数据格式基准 (JSON Schema)
写回到 `quiz:task_result:{taskId}` 中的字符串，应当是由标准 `List<Dict>` 构成的有效 JSON。Java 端及前端依赖此结构直接渲染预览表单：

```json
[
  {
    "questionText": "题目题干主要内容的纯文本...",
    "type": 1, // 1:单选 2:多选 3:判断 4:简答
    "options": ["选项A的内容", "选项B的内容", "选项C的内容"], // 如果是判断或简答题，可为空数组
    "answer": "A", // 单选为 A/B/C，多选为 A,B,C 逗号分隔，判断为 0(错)/1(对)
    "analysis": "题目的解析和考点..."
  }
]
```
