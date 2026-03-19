# 🤖 Self-Healing Sci-Agent 
### 基于大模型自愈机制的自动化科研仿真智能体

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-blue.svg)](https://github.com/langchain4j/langchain4j)
[![MATLAB](https://img.shields.io/badge/Simulation-MATLAB-darkblue.svg)](https://www.mathworks.com/products/matlab.html)

**Self-Healing Sci-Agent** 是一款面向科研仿真场景的智能化助手。它深度集成了 **RAG（检索增强生成）** 与 **Agent 反射机制**，核心解决“科研文献理论到仿真代码落地”的转化难题。

**🚀 项目核心亮点：** 具备 **代码自愈（Self-Healing）** 能力。当生成的仿真代码在本地运行报错时，系统能实时捕获并分析 `stderr` 堆栈日志，驱动 LLM 自动修正逻辑并重新执行，直至任务成功。

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

### 1. 目录树结构 (Project Structure)

```text
consultant/
├── 📂 src/main/resources/
│   └── 📂 content/
│       ├── 📄 qkd_protocol_v1.pdf       # 核心科研文献 (PDF)
│       ├── 📄 qkd_protocol_v2.pdf       # 仿真环境参考 (PDF)
│       └── 📝 file_mapping_index.txt    # 【关键】文献与本地代码映射索引说明
└── ...
### 2. 文献与本地代码映射索引说明
# 文献文件名            | 本地代码存储路径（挂载点）             
qkd_sns_protocol.pdf   | E:/Matlab_Workspace/SNS_Project/      
tf_qkd_theory.pdf      | E:/Matlab_Workspace/TF_QKD_Library/   
...
