/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: Log.java
 * 
 * 描述    : 提供工具运行日志初始化、状态记录、错误告警统计和日志文件输出能力。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-22  开发人员  统一参数校验及错误信息的日志输出格式。
 *     2026-07-25  开发人员  删除未被需求明确要求的阶段标题及输入配置一致性汇总输出。
 *     2026-07-25  开发人员  增加功能完成点进度输出，并屏蔽未被需求规定的详细生成器信息。
 *     2026-07-26  开发人员  同步004835格式的1018错误去重判断。
 *     2026-07-26  开发人员  同步1018统一文本的错误去重判断。
 *     2026-07-26  开发人员  补充运行日志版权信息中的©符号。
 *     2026-07-27  开发人员  按B-08任务修复LOG文件名大小写（log_改为Log_，符合001141需求）。
 *     2026-07-28  开发人员  按B-09任务修复进度关键字为[Process]、阶段标题输出、阶段完成信息格式，新增resetStage方法，totalStages改为7。
 *     2026-08-01  开发人员  阶段模板默认仅写日志，并按阶段缺口选择性补充控制台开始和完成提示。
 *     2026-08-25  开发人员  按问题报告单TR_PR_002/003/004（004878/004904）新增consistencyError方法，
 *                 输出无[Error]前缀、[错误码]后接全角冒号的一致性校验错误格式。
 *     2026-08-26  开发人员  按问题报告单TR_PR_021（004934）新增writeLineToLogFile与
 *                 failRunningStatusFileGeneration：所有日志写入通过checkError()检测底层
 *                 写入失败，失败时输出3014并立即终止（与文件系统工具对齐）。
 */

package com.nwpu.verify.layers.output;

/* 头文件 */
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @brief
 * 功能: 记录工具运行过程中的状态、告警、错误和验证摘要信息。
 *       日志内容覆盖如下：
 *       1. 工具名称、版本信息和版权信息；
 *       2. 工具运行过程中的状态信息或提示信息；
 *       3. 输入XML文件的CRC校验和XSD校验结果；
 *       4. 输入终端可加载文件和可加载文件的CRC校验结果；
 *       5. 输入可加载文件的配置一致性校验结果；
 *       6. 配置验证报告的生成结果。
 *       文件格式: TXT格式。
 *       文件命名: CPM_Gen_Verify_YYYYMMDD_HHMMSS.log。
 *       日志格式: [CPM_Gen_Verify][YYYY-MM-DD HH:MM:SS][Info/Warning/Error] 消息内容。
 */
public class Log {
    /* 全局变量 */
    private static final String TOOL_NAME = "CPM_Gen_Verify";
    private static final String TOOL_VERSION = "TLF42200105A-AF";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE_PREFIX = "CPM_Gen_Verify_";
    private static final String LOG_FILE_SUFFIX = ".log";
    public static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /* 日志文件名（在初始化时确定） */
    private static String logFileName = null;

    /* PR_42: 日志文件的绝对路径 */
    private static String logFileAbsolutePath = null;

    /* 使用静态的PrintWriter，避免频繁打开关闭文件 */
    private static PrintWriter logWriter = null;

    /* 标记日志是否已初始化（参数校验通过后才初始化） */
    private static boolean initialized = false;

    /* 统计错误和警告数量 */
    private static int totalErrors = 0;
    private static int totalWarnings = 0;

    /* 当前阶段信息 */
    private static int currentStage = 0;
    private static int totalStages = 7; /* 001067/001135: 7个固定阶段 */

    /* 标记验证摘要是否已输出 */
    private static boolean summaryPrinted = false;

    /* 标记是否应该禁止输出日志文件（用于特定错误码场景） */
    private static boolean suppressLogFile = false;

    /* B-07/006329: 保存原始控制台输出流，绕过 VerboseOutputStream 过滤，
     * 确保 Verification Status / Total Success / Total Fails 等汇总信息同时输出到 CMD 和 LOG。 */
    private static PrintStream consoleOut = System.out;

    /* 保存日志文件对象引用，用于需要时删除 */
    private static File logFileRef = null;

    /* 运行期告警去重：避免同一(告警码+消息内容)重复打印 */
    private static final Set<String> emittedWarningKeys = new HashSet<>();

    /* 运行期错误去重：仅用于少量"摘要类/兜底类"错误，避免同一错误重复打印 */
    private static final Set<String> emittedErrorKeys = new HashSet<>();

    /* PR_14: 追踪所有记录的错误码，用于区分"可记录错误"和"致命错误" */
    /* 可记录错误：2007-2014, 3001-3005 - 这些错误应该被记录到日志但不应该触发1018 */
    private static final Set<Integer> recordedErrorCodes = new HashSet<>();

    // 参数校验阶段尚未初始化日志文件时，暂存错误/告警文本。
    private static final List<String> pendingLogMessages = new ArrayList<>();

    /**
     * initialize
     *
     * @brief
     * 功能: 初始化日志文件，仅在参数校验通过后创建日志文件并写入文件头信息。
     *       处理规则如下：
     *       1. 已初始化时直接返回成功；
     *       2. 新一轮运行前清理告警、错误和错误码去重状态；
     *       3. 根据输出目录和时间戳生成日志文件名；
     *       4. 创建UTF-8编码日志写入器并注册关闭钩子。
     * 
     * @param[in]  outputLogPath: 日志文件输出路径。
     * @param[in]  timestamp: 时间戳字符串，格式为yyyyMMdd_HHmmss，用于生成文件名。
     * @return
     * boolean: 初始化成功返回true，初始化失败返回false。
     * @implements 
     * @scope external
     * @errno:
     * IOException: 日志文件创建或写入器初始化失败。
     */
    public static synchronized boolean initialize(String outputLogPath, String timestamp) {
        if (initialized) {
            return true; /* 已经初始化过，返回成功 */
        }

        /* 新一轮运行前清理去重状态 */
        emittedWarningKeys.clear();
        emittedErrorKeys.clear();
        summaryPrinted = false;
        // 保留参数阶段已记录的错误码，使1014/1015等错误进入运行日志和退出矩阵。

        try {
            /* 使用用户指定的日志目录 */
            File logDir = new File(outputLogPath);

            /* 生成日志文件名 */
            /* 001141: 文件名格式为 CPM_Gen_Verify_Log_YYYYMMDD_HHmmss.log（大写L） */
            logFileName = LOG_FILE_PREFIX + "Log_" + timestamp + LOG_FILE_SUFFIX;

            /* 在指定目录下创建日志文件 */
            File logFile = new File(logDir, logFileName);
            logFileRef = logFile;

            /* PR_42: 保存日志文件的规范路径（去除冗余的.\\等） */
            try {
                logFileAbsolutePath = logFile.getCanonicalPath();
            } catch (IOException e) {
                logFileAbsolutePath = logFile.getAbsolutePath();
            }

            /* 005197/004934：日志文件也按Windows工具路径约束检查最终目标路径。
             * 某些JVM/文件系统组合支持创建超长路径，不能只依赖FileOutputStream是否抛异常；
             * 超过工具允许范围时应按日志文件生成失败处理，交由Application输出3014。 */
            if (logFile.getPath().length() > 260) {
                throw new IOException("Runtime status log file path exceeds maximum length (260 characters): "
                        + logFile.getPath().length());
            }

            /* 创建PrintWriter，指定UTF-8编码 */
            logWriter = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), "UTF-8")));

            initialized = true;

            /* 输出文件头部信息 */
            writeHeader();

            /* 将日志初始化前已经输出到CMD的异常文本补写到LOG。 */
            for (String pendingMessage : pendingLogMessages) {
                logWriter.println(pendingMessage);
            }
            logWriter.flush();
            /* TR_PR_021/004934: 文件头或补写内容写入失败（如磁盘满）视为运行状态文件生成失败，
             * 返回false由调用方按3014处理。 */
            if (logWriter.checkError()) {
                logWriter.close();
                logWriter = null;
                logFileRef = null;
                logFileAbsolutePath = null;
                initialized = false;
                return false;
            }
            if (logFileRef == null || !logFileRef.isFile() || logFileRef.length() == 0) {
                logWriter.close();
                logWriter = null;
                logFileAbsolutePath = null;
                initialized = false;
                return false;
            }
            pendingLogMessages.clear();

            /* 添加JVM关闭钩子来确保日志文件被正确关闭 */
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (logWriter != null && !suppressLogFile) {
                    /* 输出验证摘要 */
                    writeValidationSummary();
                    logWriter.close();
                }
            }));

            return true; /* 初始化成功 */

        } catch (Exception e) {
            /* 005197/004934：文件创建、初始化或文件头写入发生任何异常时，
             * 保留可能已经创建的残留文件，但释放写入器状态，由Application统一输出3014。
             * 这里不能把异常继续抛给业务校验层，否则可能被误归类为其他错误。 */
            if (logWriter != null) {
                logWriter.close();
                logWriter = null;
            }
            initialized = false;
            return false; /* 初始化失败 */
        }
    }

    /**
     * getLogFileAbsolutePath
     *
     * @brief
     * 功能: 获取当前日志文件的绝对路径。
     * 
     * @return
     * String: 日志文件绝对路径，未初始化时返回null。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static String getLogFileAbsolutePath() {
        return logFileAbsolutePath;
    }

    /**
     * close
     *
     * @brief
     * 功能: 显式关闭日志文件，在程序提前退出时确保日志内容被刷新并释放去重状态。
     * 
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static synchronized void close() {
        if (logWriter != null) {
            File finalizedLogFile = logFileRef;
            if (!suppressLogFile) {
                /* 004864：只有在所有业务处理结束、即将完成最终落盘时才写入成功状态。
                 * 控制台成功提示在文件关闭并通过完整性检查后再输出。 */
                writeLineToLogFile(formatLogMessage(LogType.INFO_LOWER, null,
                        "succeeded to generate running status file."));
                writeValidationSummary();
            }
            logWriter.flush();
            /* TR_PR_021/004934: 最终落盘失败视为运行状态文件生成失败，输出3014并终止 */
            boolean writeFailed = logWriter.checkError();
            logWriter.close();
            logWriter = null;
            initialized = false;
            boolean finalFileValid = finalizedLogFile != null
                    && finalizedLogFile.isFile()
                    && finalizedLogFile.length() > 0;
            if (writeFailed || !finalFileValid) {
                failRunningStatusFileGeneration();
            }
            /* 004864：文件已关闭且最终存在、非空后，才向控制台报告生成成功。 */
            System.out.println(formatLogMessage(LogType.INFO_LOWER, null,
                    "succeeded to generate running status file."));
        }

        /* 释放去重状态，避免影响后续同JVM内重复运行 */
        emittedWarningKeys.clear();
        emittedErrorKeys.clear();
        // 保留参数阶段已记录的错误码，使1014/1015等错误进入运行日志和退出矩阵。
    }

    /**
     * setSuppressLogFile
     *
     * @brief
     * 功能: 设置是否禁止输出日志文件，用于特定错误码场景下删除已创建日志文件。
     *       suppress取值含义如下：
     *       true  禁止输出日志文件；
     *       false 正常输出日志文件。
     * 
     * @param[in]  suppress: 日志文件输出抑制标志。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void setSuppressLogFile(boolean suppress) {
        suppressLogFile = suppress;
    }

    /**
     * closeAndDeleteLogFile
     *
     * @brief
     * 功能: 关闭当前日志写入器并删除已创建的日志文件。
     * 
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static synchronized void closeAndDeleteLogFile() {
        suppressLogFile = true;
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
            logWriter = null;
            initialized = false;
        }
        emittedWarningKeys.clear();
        emittedErrorKeys.clear();
        recordedErrorCodes.clear();
        /* 删除日志文件 */
        if (logFileRef != null && logFileRef.exists()) {
            logFileRef.delete();
        }
    }

    /**
     * isInitialized
     *
     * @brief
     * 功能: 判断日志模块是否已经完成初始化。
     * 
     * @return
     * boolean: 已初始化返回true，未初始化返回false。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * hasErrors
     *
     * @brief
     * 功能: 判断当前运行过程中是否记录过错误码。
     * 
     * @return
     * boolean: 存在错误返回true，不存在错误返回false。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static boolean hasErrors() {
        return !recordedErrorCodes.isEmpty();
    }

    /**
     * hasOnlyRecordableErrors
     *
     * @brief
     * 功能: 检查当前记录的错误是否全部属于可记录错误。
     *       可记录错误范围如下：
     *       2007-2017 XML元素和属性相关错误；
     *       3001-3005 配置约束和验证相关错误。
     *       可记录错误应写入日志文件，不触发1018错误和日志删除。
     * 
     * @return
     * boolean: 全部为可记录错误返回true，存在致命错误或没有错误时返回false。
     * @implements CPMVerify-TOR-001096
     * @scope external
     * @errno:
     * N/A
     */
    public static boolean hasOnlyRecordableErrors() {
        if (recordedErrorCodes.isEmpty()) {
            return false; /* 没有错误，返回false */
        }
        
        for (Integer code : recordedErrorCodes) {
            if (code == null) continue;
            
            /* 检查是否是"可记录错误"（2007-2017 或 3001-3005） */
            boolean isRecordableError = (code >= 2007 && code <= 2017) || (code >= 3001 && code <= 3005);
            
            if (!isRecordableError) {
                /* 发现非"可记录错误"，返回false */
                return false;
            }
        }
        
        /* 所有错误都是"可记录错误" */
        return true;
    }

    /**
     * getRecordedErrorCodes
     *
     * @brief
     * 功能: 获取当前已记录错误码集合的副本。
     * 
     * @return
     * Set<Integer>: 已记录错误码集合的副本。
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static Set<Integer> getRecordedErrorCodes() {
        return new HashSet<>(recordedErrorCodes);
    }

    /**
     * writeHeader
     *
     * @brief
     * 功能: 向日志文件写入版权、工具名称、版本、创建时间和进度跟踪头部信息。
     * 
     * @return
     * N/A
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static void writeHeader() {
        if (logWriter != null) {
            logWriter.println(
                    "Copyright \u00A9 2025, Xi'an Aviation Computing Technology Research Institute, Aviation Industry Corporation of China. All rights reserved.");
            logWriter.println("Tool Name : " + TOOL_NAME + " Tool");
            logWriter.println("Tool Version : " + TOOL_VERSION);
            /* 001135: {Name} 表示运行工具的用户名 */
            logWriter.println("Created By : " + System.getProperty("user.name"));
            logWriter.println("Created Date: " + getCurrentTime());
            logWriter.println("------------------------------------------------------------");
            logWriter.println("[PROGRESS TRACKING]");
            logWriter.println("------------------------------------------------------------");
            logWriter.flush();
        }
    }

    /**
     * startStage
     *
     * @brief
     * 功能: 开始新的处理阶段，输出阶段标题并写入日志文件。
     * 
     * @param[in]  stageName: 阶段名称。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void startStage(String stageName) {
        startStage(stageName, false);
    }

    /**
     * startStage
     *
     * @brief
     * 功能: 执行 startStage 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static void startStage(String stageName, boolean outputGenericStartToConsole) {
        currentStage++;
        /* 001135: 输出阶段标题行 [ STAGE {Index} : {Stage Name} ] */
        String stageTitle = "[ STAGE " + currentStage + " : " + stageName + " ]";
        writeStageStartToLog(stageTitle, stageName);
        /* 阶段标题保留在CMD；仅在没有专用开始提示的阶段输出通用开始文本。 */
        System.out.println(stageTitle);
        if (outputGenericStartToConsole) {
            System.out.println(formatLogMessage(LogType.INFO, null,
                    String.format("Initiating %s.", stageName)));
        }
    }

    /**
     * resetStage
     *
     * @brief
     * 功能: 重置阶段计数器为0，供新一轮运行（从Stage 1开始）使用。
     *
     * @return
     * N/A
     * @implements
     * @scope external
     * @errno:
     * N/A
     */
    public static void recordCurrentStageStartInLog(String stageName) {
        String stageTitle = "[ STAGE " + currentStage + " : " + stageName + " ]";
        writeStageStartToLog(stageTitle, stageName);
    }

    /**
     * writeStageStartToLog
     *
     * @brief
     * 功能: 执行 writeStageStartToLog 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static synchronized void writeStageStartToLog(String stageTitle, String stageName) {
        if (logWriter != null) {
            writeLineToLogFile("------------------------------------------------------------");
            writeLineToLogFile(stageTitle);
            writeLineToLogFile("------------------------------------------------------------");
            writeLineToLogFile(formatLogMessage(LogType.INFO, null,
                    String.format("Initiating %s.", stageName)));
        }
    }

    /**
     * resetStage
     *
     * @brief
     * 功能: 执行 resetStage 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static void resetStage() {
        currentStage = 0;
    }

    /**
     * completeStage
     *
     * @brief
     * 功能: 记录当前阶段完成状态，并根据总阶段数输出进度百分比。
     * 
     * @param[in]  stageName: 阶段名称。
     * @param[in]  errorCount: 当前阶段错误数量。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    /**
     * 004788: 按需求规定的功能完成点输出进度。
     */
    public static void reportProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100.");
        }
        /* 005878/001135: 进度格式 [CPM_Gen_Verify][时间][Info][Process] {XX}%
         * [Info] 与 [Process] 之间无空格，直接构造完整日志行，避免 formatLogMessage 在消息前插入空格。 */
        String logMessage = String.format("[%s][%s][%s][Process] %d%%",
                TOOL_NAME,
                getCurrentTime(),
                LogType.INFO.getText(),
                progress);
        writeLog(logMessage);
    }

    /**
     * completeStage
     *
     * @brief
     * 功能: 执行 completeStage 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static void completeStage(String stageName, int errorCount) {
        completeStage(stageName, errorCount, false);
    }

    /**
     * completeStage
     *
     * @brief
     * 功能: 执行 completeStage 相关处理。
     *
     * @scope external
     * @errno:
     * N/A
     */
    public static void completeStage(String stageName, int errorCount,
            boolean outputGenericCompletionToConsole) {
        /* 001135: 带错误数的阶段完成明细仅写入运行状态记录文件。 */
        writeLogFileOnly(formatLogMessage(LogType.INFO, null,
                String.format("%s completed with %d errors.", stageName, errorCount)));
        String completionMessage = formatLogMessage(LogType.INFO, null,
                String.format("%s completed.", stageName));
        writeLogFileOnly(completionMessage);
        if (outputGenericCompletionToConsole) {
            System.out.println(completionMessage);
        }
        // 004788要求按功能完成点报告进度，由主流程显式调用reportProgress。
    }

    /**
     * writeValidationSummary
     *
     * @brief
     * 功能: 输出验证摘要，包括错误总数、告警总数和验证状态。
     * 
     * @return
     * N/A
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static void writeValidationSummary() {
        /* 防止重复输出 */
        if (summaryPrinted || logWriter == null) {
            return;
        }

        summaryPrinted = true;
        writeLineToLogFile("------------------------------------------------------------");
        writeLineToLogFile("[ VALIDATION SUMMARY ]");
        writeLineToLogFile("------------------------------------------------------------");
        writeLineToLogFile("Total Errors : " + totalErrors);
        writeLineToLogFile("Total Warnings : " + totalWarnings);
        writeLineToLogFile("Verification Status: " + (totalErrors > 0 ? "Failed" : "Passed"));
    }

    /**
     * printValidationSummary
     *
     * @brief
     * 功能: 手动输出验证摘要，用于需要提前写出摘要信息的场景。
     * 
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void printValidationSummary() {
        writeValidationSummary();
    }

    /**
     * @brief
     * 功能: 定义日志等级类型及其输出文本。
     */
    public enum LogType {
        INFO("Info"),
        INFO_LOWER("info"),
        WARNING("Warning"),
        ERROR("Error");

        private final String text;

        LogType(String text) {
            this.text = text;
        }

        /**
         * getText
         *
         * @brief
         * 功能: 获取日志等级对应的输出文本。
         * 
         * @return
         * String: 日志等级输出文本。
         * @implements 
         * @scope external
         * @errno:
         * N/A
         */
        public String getText() {
            return text;
        }
    }

    /**
     * info
     *
     * @brief
     * 功能: 按Info等级记录日志消息。
     * 
     * @param[in]  message: 日志消息内容。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void info(String message) {
        if (isSuppressedVerboseMessage(message)) {
            return;
        }
        String logMessage = formatLogMessage(LogType.INFO, null, message);
        writeLog(logMessage);
    }

    /**
     * 001178/001176: 这两条需求指定使用小写[info]，与通用[Info]格式不同。
     */
    public static void infoLower(String message) {
        String logMessage = formatLogMessage(LogType.INFO_LOWER, null, message);
        writeLog(logMessage);
    }

    /**
     * 006329: 符合性校验结果汇总专用输出。
     * 需求模板为 [{YYYY-MM-DD HH:MM:SS}][Info] {message}，
     * 不带 [CPM_Gen_Verify] 前缀，与通用 info() 不同。
     */
    public static void infoNoPrefix(String message) {
        String logMessage = String.format("[%s][%s] %s",
                getCurrentTime(),
                LogType.INFO.getText(),
                message);
        // B-07/006329: 直接写入原始控制台和日志文件，绕过 VerboseOutputStream 过滤，
        // 确保 Verification Status / Total Success / Total Fails 同时输出到 CMD 和 LOG。
        consoleOut.println(logMessage);
        consoleOut.flush();
        if (initialized) {
            writeLineToLogFile(logMessage);
        }
    }

    /**
     * B-07/006329: 保存原始控制台输出流引用。
     * 在 filterVerboseGeneratorOutput() 重定向 System.out 之前调用。
     */
    public static void setConsoleOut(PrintStream out) {
        consoleOut = out;
    }

    /**
     * isSuppressedVerboseMessage
     *
     * @brief
     * 功能: 执行 isSuppressedVerboseMessage 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isSuppressedVerboseMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("识别到二进制文件类型")
                || message.contains("[部署地址计算]");
    }

    /**
     * warning
     *
     * @brief
     * 功能: 按Warning等级记录日志消息，并对相同告警码和消息内容进行去重。
     * 
     * @param[in]  code: 告警码。
     * @param[in]  message: 告警消息内容。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void warning(Integer code, String message) {
        String key = String.valueOf(code) + "|" + String.valueOf(message);
        if (emittedWarningKeys.contains(key)) {
            return;
        }
        emittedWarningKeys.add(key);
        totalWarnings++;
        String logMessage = formatLogMessage(LogType.WARNING, code, message);
        if (!initialized) {
            pendingLogMessages.add(logMessage);
        }
        writeLog(logMessage);
    }

    /**
     * error
     *
     * @brief
     * 功能: 按Error等级记录日志消息，统计错误数量并维护错误码集合。
     *       去重规则如下：
     *       1. XML XSD校验失败摘要类错误按错误码和消息内容去重；
     *       2. XML重复属性类错误按错误码和消息内容去重；
     *       3. 非空错误码加入已记录错误码集合。
     * 
     * @param[in]  code: 错误码。
     * @param[in]  message: 错误消息内容。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void error(Integer code, String message) {
        /* 对少量"摘要类"错误做去重，避免重复打印（例如同一XML的XSD失败被不同路径重复报告） */
        /* 问题报告单4/004835: 1018消息格式改为 "Correctness verification for XML file Failed, File Path: [完整路径]。" */
        if (code != null && code.equals(com.nwpu.verify.entity.CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED)
                && message != null
                && message.startsWith("Correctness verification for XML file Failed, File Path: [")
                && message.endsWith("]。")) {
            String key = String.valueOf(code) + "|" + message;
            if (emittedErrorKeys.contains(key)) {
                return;
            }
            emittedErrorKeys.add(key);
        }

        /* 2012重复属性类错误也做去重（预处理/XSD/解析可能多路径触发） */
        if (code != null && code.equals(com.nwpu.verify.entity.CodeConstant.ERROR_XML_REDUNDANCY_ATTRIBUTES)
                && message != null) {
            String key = String.valueOf(code) + "|" + message;
            if (emittedErrorKeys.contains(key)) {
                return;
            }
            emittedErrorKeys.add(key);
        }

        /* PR_14: 记录错误码，用于后续判断是否只有"可记录错误" */
        if (code != null) {
            recordedErrorCodes.add(code);
        }

        totalErrors++;
        String logMessage = formatLogMessage(LogType.ERROR, code, message);
        if (!initialized) {
            pendingLogMessages.add(logMessage);
        }
        writeLog(logMessage);
    }

    /**
     * consistencyError
     *
     * @brief
     * 功能: 按需求004878/004904规定的一致性校验错误格式记录日志消息。
     *       格式如下（无[Error]前缀，[错误码]后接全角冒号）：
     *       [CPM_Gen_Verify][时间][错误码]：消息内容。
     *       与error()一致地统计错误数量并维护错误码集合。
     *
     * @param[in]  code: 错误码（3001/3003等一致性校验错误）。
     * @param[in]  message: 错误消息内容。
     * @return
     * N/A
     * @implements 004878/004904
     * @scope external
     * @errno:
     * N/A
     */
    public static void consistencyError(Integer code, String message) {
        /* PR_14: 记录错误码，用于后续判断是否只有"可记录错误" */
        if (code != null) {
            recordedErrorCodes.add(code);
        }

        totalErrors++;
        String logMessage = String.format("[%s][%s][%s]：%s",
                TOOL_NAME,
                getCurrentTime(),
                code,
                message);
        if (!initialized) {
            pendingLogMessages.add(logMessage);
        }
        writeLog(logMessage);
    }

    /**
     * getCurrentTime
     *
     * @brief
     * 功能: 获取按日志时间格式格式化后的当前时间。
     * 
     * @return
     * String: 当前时间字符串。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getCurrentTime() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    /**
     * formatLogMessage
     *
     * @brief
     * 功能: 根据日志等级、错误码和消息内容生成标准日志行。
     *       格式如下：
     *       code非空: [CPM_Gen_Verify][时间][等级][错误码] 消息内容；
     *       code为空: [CPM_Gen_Verify][时间][等级] 消息内容。
     * 
     * @param[in]  type: 日志等级类型。
     * @param[in]  code: 错误码或告警码，可为空。
     * @param[in]  message: 日志消息内容。
     * @return
     * String: 格式化后的日志消息。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private static String formatLogMessage(LogType type, Integer code, String message) {
        if (code != null) {
            return String.format("[%s][%s][%s][%s] %s",
                    TOOL_NAME,
                    getCurrentTime(),
                    type.getText(),
                    code,
                    message);
        }
        return String.format("[%s][%s][%s] %s",
                TOOL_NAME,
                getCurrentTime(),
                type.getText(),
                message);
    }

    /**
     * writeLog
     *
     * @brief
     * 功能: 将日志消息输出到控制台，并在日志初始化后写入日志文件。
     * 
     * @param[in]  message: 待输出的日志消息。
     * @return
     * N/A
     * @implements 
     * @scope internal
     * @errno:
     * Exception: 写入日志文件失败时捕获并输出错误信息。
     */
    private static synchronized void writeLogFileOnly(String message) {
        if (initialized) {
            writeLineToLogFile(message);
        }
    }

    /**
     * writeLog
     *
     * @brief
     * 功能: 执行 writeLog 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static synchronized void writeLog(String message) {
        /* 总是输出到控制台 */
        System.out.println(message);

        /* 只有在初始化后才写入日志文件（参数校验通过后） */
        if (initialized) {
            writeLineToLogFile(message);
        }
    }

    /**
     * writeLineToLogFile
     *
     * @brief
     * 功能: 将一行消息写入运行状态记录文件并检查底层写入结果（TR_PR_021/004934）。
     *       PrintWriter不抛出底层IO异常，磁盘满、文件被占用等写入失败只能通过
     *       checkError()发现；写入失败视为运行状态文件生成失败，输出3014并立即
     *       终止（与文件系统工具RuntimeStatusLogger的checkError方案对齐）。
     *
     * @param[in]  message: 待写入的日志消息。
     * @return
     * N/A
     * @implements 004934
     * @scope internal
     * @errno:
     * N/A
     */
    private static void writeLineToLogFile(String message) {
        if (logWriter == null) {
            return;
        }
        logWriter.println(message);
        logWriter.flush(); /* 确保及时写入 */
        /* TR_PR_021: PrintWriter吞掉底层写入异常，须用checkError()检测写入失败 */
        if (logWriter.checkError()) {
            failRunningStatusFileGeneration();
        }
    }

    /**
     * failRunningStatusFileGeneration
     *
     * @brief
     * 功能: 运行状态记录文件生成/写入失败时的统一处理（TR_PR_021）。
     *       按004934输出3014错误信息（日志文件已不可写，仅输出到控制台），
     *       并按3014错误码分类立即终止（与文件系统工具halt(1)行为对齐）。
     *
     * @return
     * N/A
     * @implements 004934
     * @scope internal
     * @errno:
     * N/A
     */
    private static void failRunningStatusFileGeneration() {
        /* B-07: 使用原始控制台流绕过VerboseOutputStream过滤，确保3014必达CMD */
        consoleOut.println(formatLogMessage(LogType.ERROR,
                com.nwpu.verify.entity.CodeConstant.ERROR_GENERATE_LOG_FAILED,
                "failed to generate running status file."));
        consoleOut.flush();
        Runtime.getRuntime().halt(1);
    }

    /**
     * logXmlValidation
     *
     * @brief
     * 功能: 记录输入XML文件的CRC校验和XSD校验结果。
     *       对应规范要求e: 工具对输入XML文件的CRC校验和XSD校验结果。
     * 
     * @param[in]  xmlFileName: XML文件名。
     * @param[in]  crcResult: CRC校验结果，十六进制格式。
     * @param[in]  xsdResult: XSD校验结果，如XSD验证通过或XSD验证失败。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logXmlValidation(String xmlFileName, String crcResult, String xsdResult) {
        info(String.format("CRC and XSD validation results for input XML file:\nFile: %s\nCRC Result: %s\nXSD Result: %s",
                xmlFileName, crcResult, xsdResult));
    }

    /**
     * logCPMTerminalBinaryValidation
     *
     * @brief
     * 功能: 记录输入终端可加载文件的CRC校验结果。
     *       对应规范要求f: 工具对输入RDCU终端可加载文件的CRC校验结果。
     * 
     * @param[in]  binaryFileName: 二进制文件名。
     * @param[in]  crcResult: CRC校验结果，十六进制格式。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logCPMTerminalBinaryValidation(String binaryFileName, String crcResult) {
        info(String.format("CRC validation result for input terminal loadable file:\nFile: %s\nResult: %s",
                binaryFileName, crcResult));
    }

    /**
     * logCPMBinaryValidation
     *
     * @brief
     * 功能: 记录输入可加载文件的CRC校验结果。
     *       对应规范要求g: 工具对输入RDCU可加载文件的CRC校验结果。
     * 
     * @param[in]  binaryFileName: 二进制文件名。
     * @param[in]  crcResult: CRC校验结果，十六进制格式。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logCPMBinaryValidation(String binaryFileName, String crcResult) {
        info(String.format("CRC validation result for input loadable file:\nFile: %s\nResult: %s",
                binaryFileName, crcResult));
    }

    /**
     * logConfigConsistencyCheck
     *
     * @brief
     * 功能: 记录输入可加载文件的配置一致性校验结果。
     *       对应规范要求h: 工具对输入RDCU可加载文件的配置一致性校验结果。
     * 
     * @param[in]  configFileName: 配置文件名称。
     * @param[in]  checkResult: 配置一致性校验结果，如配置一致或配置不一致。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logConfigConsistencyCheck(String configFileName, String checkResult) {
        info(String.format("Configuration consistency validation result for input loadable file:\nFile: %s\nResult: %s",
                configFileName, checkResult));
    }

    /**
     * logReportGeneration
     *
     * @brief
     * 功能: 记录配置验证报告的生成结果。
     *       对应规范要求i: 工具对RDCU配置验证报告的生成结果。
     * 
     * @param[in]  reportFileName: 报告文件名。
     * @param[in]  result: 报告生成结果，如成功生成验证报告。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logReportGeneration(String reportFileName, String result) {
        info(String.format("Report generation result:\nFile name: %s\nResult: %s",
                reportFileName, result));
    }

    /**
     * logRuntimeStatus
     *
     * @brief
     * 功能: 记录工具运行过程中的运行状态信息。
     *       对应规范要求d: 工具运行过程中的运行状态信息或提示信息。
     * 
     * @param[in]  status: 运行状态信息。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logRuntimeStatus(String status) {
        info("Runtime status: " + status);
    }

    /**
     * logWarningStatus
     *
     * @brief
     * 功能: 记录工具运行过程中的提示信息。
     *       对应规范要求d: 工具运行过程中的运行状态信息或提示信息。
     * 
     * @param[in]  warning: 提示信息。
     * @return
     * N/A
     * @implements 
     * @scope external
     * @errno:
     * N/A
     */
    public static void logWarningStatus(String warning) {
        info("Runtime information: " + warning);
    }
}
