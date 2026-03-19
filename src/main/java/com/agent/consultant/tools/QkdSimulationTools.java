package com.agent.consultant.tools;

import com.agent.consultant.service.ExperienceService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * QKD 仿真核心工具集，暴露给大模型用于文件读写、执行及数据库交互
 */
@Slf4j
@Component
public class QkdSimulationTools {

    @Value("${qkd.matlab.output-dir:D:/AutoMatlab/}")
    private String matlabOutputDir;

    @Value("${qkd.matlab.executable:matlab}")
    private String matlabExecutable;

    @Autowired
    private ExperienceService experienceService;

    /**
     * 读取指定目录下的全部历史参考代码
     */
    @Tool("读取指定目录下所有的 MATLAB 源码。当你从知识库中得知参考代码的物理目录时调用。")
    public String readReferenceCode(@P("参考源码本地绝对路径") String dirPath) {
        log.info("Agent 正在读取参考代码目录: {}", dirPath);
        if (dirPath == null || dirPath.trim().isEmpty()) return "错误：目录路径不能为空。";

        Path directory = Paths.get(dirPath.trim());
        if (!Files.exists(directory) || !Files.isDirectory(directory)) return "错误：无效目录 - " + dirPath;

        StringBuilder result = new StringBuilder();
        result.append("========== MATLAB 参考源码 ==========\n");
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> matlabFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".m"))
                    .collect(Collectors.toList());

            if (matlabFiles.isEmpty()) return "警告：该目录下未找到 .m 文件。";

            for (Path mFile : matlabFiles) {
                result.append("====== 文件: ").append(mFile.getFileName()).append(" ======\n");
                result.append(Files.readString(mFile, StandardCharsets.UTF_8)).append("\n\n");
            }
            return result.toString();
        } catch (IOException e) {
            log.error("读取目录失败", e);
            return "错误：读取失败 - " + e.getMessage();
        }
    }

    /**
     * 保存单文件（支持 Agent 模块化编程）
     */
    @Tool("保存单个 MATLAB 代码文件到本地硬盘。请多次调用以实现函数拆分。")
    public String saveMatlabFile(
            @P("文件名，必须包含.m后缀") String fileName,
            @P("该文件的完整 MATLAB 源码") String code) {
        if (fileName == null || !fileName.endsWith(".m") || code == null) {
            return "错误：文件名或代码非法。";
        }
        try {
            Path outputDir = Paths.get(matlabOutputDir);
            if (!Files.exists(outputDir)) Files.createDirectories(outputDir);

            Path scriptPath = outputDir.resolve(fileName.trim());
            Files.writeString(scriptPath, code, StandardCharsets.UTF_8);
            log.info("Agent 保存了文件: {}", scriptPath.toAbsolutePath());
            return "文件 " + fileName + " 已成功落盘。";
        } catch (IOException e) {
            log.error("文件保存异常", e);
            return "保存失败: " + e.getMessage();
        }
    }

    /**
     * 核心调度器：执行 MATLAB 并通过 Wrapper 捕获真实报错
     */
    @Tool("执行 MATLAB 主程序。若返回带有【执行失败】的日志，必须修改代码并重试。")
    public String executeMatlabScript(@P("主程序文件名(不含.m)") String mainScriptName) {
        log.info("Agent 请求执行主程序: {}", mainScriptName);
        try {
            Path outputDir = Paths.get(matlabOutputDir);

            // 1. 清理上一轮的状态标志
            Files.deleteIfExists(outputDir.resolve("success.log"));
            Files.deleteIfExists(outputDir.resolve("error.log"));

            // 2. 构建安全的执行包装器 (Wrapper) 脚本
            // 利用 try-catch 拦截 MATLAB 内部报错并输出到独立日志，避免 Java 进程阻塞
            String wrapperCode = String.format(
                    "try\n" +
                            "    run('%s');\n" +
                            "    fid = fopen('success.log', 'w'); fprintf(fid, 'OK'); fclose(fid);\n" +
                            "catch ME\n" +
                            "    fid = fopen('error.log', 'w'); fprintf(fid, '%%s\\n%%s', ME.identifier, ME.message); fclose(fid);\n" +
                            "    exit(1);\n" +
                            "end", mainScriptName.replace(".m", ""));

            Files.writeString(outputDir.resolve("wrapper.m"), wrapperCode, StandardCharsets.UTF_8);

            // 3. 发起进程调用
            ProcessBuilder pb = new ProcessBuilder(matlabExecutable, "-nosplash", "-nodesktop", "-r", "run('wrapper.m')");
            pb.directory(outputDir.toFile());
            Process process = pb.start();

            // 4. 轮询监控日志状态（最大等待60秒）
            int timeoutSeconds = 60;
            for (int i = 0; i < timeoutSeconds; i++) {
                if (Files.exists(outputDir.resolve("success.log"))) {
                    return "【执行成功！】图表已在屏幕上弹出。";
                }
                if (Files.exists(outputDir.resolve("error.log")) && !process.isAlive()) {
                    String errorMsg = Files.readString(outputDir.resolve("error.log"));
                    log.warn("捕获到 MATLAB 报错: {}", errorMsg);
                    return "【执行失败！】MATLAB 抛出以下错误，请分析错误并修改代码后重试：\n" + errorMsg;
                }
                Thread.sleep(1000);
            }
            return "【执行超时】MATLAB 运行超过60秒未响应。";
        } catch (Exception e) {
            log.error("MATLAB 进程启动异常", e);
            return "系统异常: " + e.getMessage();
        }
    }

    @Tool("当 MATLAB 报错时，调用此工具查询 MySQL 数据库获取修复建议。")
    public String queryBugFixExperience(@P("MATLAB 抛出的具体报错信息") String errorMessage) {
        log.info("查询 Bug 修复经验：{}", errorMessage);
        String advice = experienceService.getAdvice(errorMessage);
        if (advice == null || advice.trim().isEmpty()) {
            return "未查询到相关修复经验，请自主分析排查。";
        }
        return advice;
    }

    @Tool("当你成功修复了一个 Bug 之后，调用此工具将其录入数据库。")
    public String saveBugFixExperience(@P("报错特征关键字") String keyword, @P("你的修复代码手段") String solution) {
        log.info("记录修复经验 - 关键字：{}, 解决方案：{}", keyword, solution);
        if (keyword == null || keyword.trim().isEmpty() || solution == null || solution.trim().isEmpty()) {
            return "记录失败：关键字或解决方案不能为空。";
        }
        try {
            experienceService.record(keyword, solution);
            return "经验库已成功更新，下次遇到相同错误时将提供此解决方案。";
        } catch (Exception e) {
            log.error("记录经验失败", e);
            return "记录失败：" + e.getMessage();
        }
    }
}