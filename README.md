# 🤖 Self-Healing Sci-Agent 
### 基于大模型自愈机制的自动化科研仿真智能体
**Self-Healing Sci-Agent** 是一款专为科研仿真设计的智能助手。它不仅能通过 RAG（检索增强生成）深度解析学术文献，更能通过 **Agent 反射机制** 自动生成 MATLAB 仿真代码。
其核心亮点在于具备“代码自愈”能力——当生成的仿真代码运行出错时，系统能自动捕获报错日志并驱动 LLM 进行自主修复，直到任务成功。
---

## 🌟 核心特性 (Core Features)

* **📚 智能化知识检索 (RAG)：** 支持对 `content` 目录下 PDF 文献的深度索引。通过向量化技术，Agent 能精准定位协议细节，为代码生成提供理论支撑。
* **🛠️ 代码自愈闭环 (Self-Healing)：** * **自动执行：** 基于 Java `ProcessBuilder` 异步调度本地 MATLAB 引擎。
    * **异常捕获：** 实时监控标准错误。
    * **自主迭代：** 捕获报错后自动反馈至 LLM 进行代码重构与覆盖写入，实现闭环修复。
* **⚡ 实时交互体验：** 采用 **SSE (Server-Sent Events)** 技术，实现模型思考过程与代码生成的流式打字机效果。
* **🧠 经验沉淀系统：** 结合 Redis 多轮对话管理与 MySQL 经验池，将修复成功的案例持久化，提升后续同类问题的解决速度。

---

## 📂 项目结构与知识库配置

1：为了保证 RAG 模块能准确识别本地资源，项目采用了以下目录规范：

consultant/
├── 📂 src/main/resources/
│   └── 📂 content/
│       ├── 📄 qkd_protocol_v1.pdf        # 核心科研文献 (PDF)
│       ├── 📄 qkd_protocol_v2.pdf        # 仿真环境参考 (PDF)
│       └── 📝 file_mapping_index.txt     # 【关键】文献与本地代码映射索引说明 
└── ...
2：文献与本地代码映射索引说明
# 文献文件名           | 本地代码存储路径（挂载点）             
qkd_sns_protocol.pdf  | E:/Matlab_Workspace/SNS_Project/      
tf_qkd_theory.pdf     | E:/Matlab_Workspace/TF_QKD_Library/   
...
