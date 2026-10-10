/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: Application.java
 * 描述    : 负责参数处理、校验流程编排和结果输出
 *
 * 修改历史:
 *     2026-06-07  统一翻新文件注释
 *     2026-07-22  开发人员  按错误场景控制校验结束后的延迟退出和结果输出。
 *     2026-07-22  开发人员  调整参数、XML、BIN及输出目录校验的错误码和处理流程。
 *     2026-07-22  开发人员  调整辅助命令的大小写识别、参数数量及非法组合校验。
 *     2026-07-22  开发人员  清理已删除的中断处理及需求冲突相关历史逻辑。
 *     2026-07-22  开发人员  完善工具标识、启动界面和用户输入参数打印。
 *     2026-07-23  开发人员  调整XML解析阶段输出及解析错误原因处理。
 *     2026-07-23  开发人员  完善XML文件发现、命名校验和BIN文件目录校验。
 *     2026-07-23  开发人员  完善XML正确性校验错误分类和校验完成处理。
 *     2026-07-24  开发人员  清理已删除的旧XSD及1020、1021相关输出逻辑。
 *     2026-07-24  开发人员  修正必要元素缺失错误后在XML正确性校验阶段结束运行。
 *     2026-07-25  开发人员  按功能完成点输出10%、30%、50%、60%、70%、80%和100%进度。
 *     2026-07-25  开发人员  清理未被需求规定的阶段标题及生成器冗余输出。
 *     2026-07-26  开发人员  按004835将1018改为输出实际错误XML的完整路径。
 *     2026-07-26  开发人员  统一1018正确性校验失败文本，并避免参数校验阶段解析XML触发1017。
 *     2026-07-27  开发人员  规范报告生成失败输出并在XML正确性校验完成后停止错误流程。
 *     2026-07-28  开发人员  更新2017处理并将2014范围校验移入XML正确性阶段。
 *     2026-07-27  开发人员  修正001156字段不匹配错误信息格式及Actual/Expected赋值方向。
 *     2026-07-27  开发人员  按006329需求输出符合性校验结果汇总（Verification Status/Total Success/Total Fails）。
 *     2026-07-27  开发人员  按B-08任务传递sessionTimestamp/sessionDateTimeStr/commandLineStr给ReportGenerator，统一CSV与LOG时间戳。
 *     2026-07-28  开发人员  按B-09任务实现7个固定英文阶段名、阶段标题、结束信息，移除多余的Output Processing阶段。
 *     2026-07-30  开发人员  按B-05修正BIN CRC阶段调用和大端CRC字段读取策略。
 *     2026-08-01  开发人员  保留专用校验提示并补齐各阶段缺失的控制台开始和完成信息。
 *     2026-08-01  开发人员  按001050为报告失败字段补充XML来源文件名和行号。
 *     2026-08-20  开发人员  按问题报告单TR_PR_001（004815）将一致性校验开始提示改为需求规定文本。
 *     2026-08-25  开发人员  按问题报告单TR_PR_001~004将004789一致性校验完成提示改为需求规定文本，
 *                 替换通用阶段完成消息。
 */
package com.nwpu.verify;

import com.nwpu.verify.layers.parsing.XmlParser;
import com.nwpu.verify.layers.generation.BinaryFileDispatcher;
import com.nwpu.verify.layers.output.ReportGenerator;
import com.nwpu.verify.entity.Module;
import com.nwpu.verify.entity.ComparisonResult;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import com.sun.jna.Native;
import com.sun.jna.Library;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.Pointer;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.validation.ARINC653ConfigValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Application {
    private static final String TOOL_NAME = "CPM_Gen_Verify";
    private static final String CONFIGURATION_ID = "TLF42200105A";
    private static final String VERSION = "AF";
    private static final String TOOL_VERSION = CONFIGURATION_ID + "-" + VERSION;
    private static final String COPYRIGHT =
            "Copyright \u00A9 2025, Xi'an Aviation Computing Technology Research Institute, Aviation Industry Corporation of China. All rights reserved.";
    static final String[] REQUIRED_BIN_FILES = { "payloadMap_rom.bin", "cfgRd.bin" };

    // CPMVerify-TOR-001096: deferred errors
    private static volatile boolean deferErrorExit = false;
    private static volatile Integer deferredErrorCode = null;

    // B-08: 会话级共享变量，用于报告头部 Input File 行和 Created Date 行
    // 在主流程开始时赋值一次，供报告生成和 exitWithErrorCode 共用
    static String commandLineStr = "";
    static String sessionDateTimeStr = "";

    /**
     * isPotentialAuxiliaryCommand
     *
     * @brief
     * 功能: 执行 isPotentialAuxiliaryCommand 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static boolean isPotentialAuxiliaryCommand(String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].startsWith("-")) {
            return true;
        }
        if (args.length >= 4) {
            return false;
        }
        for (String arg : args) {
            if (isAuxiliaryCommand(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * isAuxiliaryCommand
     *
     * @brief
     * 功能: 执行 isAuxiliaryCommand 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isAuxiliaryCommand(String arg) {
        return "-v".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg) || "-le".equalsIgnoreCase(arg);
    }

    /**
     * normalizeGeneratorOption
     *
     * @brief
     * 功能: 执行 normalizeGeneratorOption 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static String normalizeGeneratorOption(String arg) {
        if (arg == null) {
            return null;
        }
        if ("-big".equalsIgnoreCase(arg) || "big".equalsIgnoreCase(arg)) {
            return "big";
        }
        if ("-little".equalsIgnoreCase(arg) || "little".equalsIgnoreCase(arg)) {
            return "little";
        }
        if ("-64".equalsIgnoreCase(arg) || "64".equalsIgnoreCase(arg)) {
            return "64";
        }
        if ("-32".equalsIgnoreCase(arg) || "32".equalsIgnoreCase(arg)) {
            return "32";
        }
        return null;
    }

    /**
     * isGeneratorOption
     *
     * @brief
     * 功能: 执行 isGeneratorOption 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static boolean isGeneratorOption(String arg) {
        return normalizeGeneratorOption(arg) != null;
    }

    /**
     * reportAuxiliaryParameterError
     *
     * @brief
     * 功能: 执行 reportAuxiliaryParameterError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static void reportAuxiliaryParameterError() {
        Log.error(CodeConstant.ERROR_AUXILIARY_PARAM_INVALID,
                "Input auxiliary parameters error ,except for -v,-h,-le.");
        exitWithErrorCode(CodeConstant.ERROR_AUXILIARY_PARAM_INVALID);
    }



    /**
     * logParameterCountError
     *
     * @brief
     * 功能: 执行 logParameterCountError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static void logParameterCountError() {
        Log.error(CodeConstant.ERROR_PARAMS_COUNT_INVALID,
                "input parameters number error ,please re-enter parameters");
    }

    /**
     * isRequiredBinFile
     *
     * @brief
     * 功能: 执行 isRequiredBinFile 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static boolean isRequiredBinFile(File file) {
        for (String requiredFile : REQUIRED_BIN_FILES) {
            if (requiredFile.equals(file.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * containsBinFile
     *
     * @brief
     * 功能: 执行 containsBinFile 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean containsBinFile(File folder, String expectedFileName) {
        File[] matches = folder.listFiles((dir, name) -> expectedFileName.equals(name));
        return matches != null && matches.length > 0;
    }

    /**
     * checkRequiredBinFiles
     *
     * @brief
     * 功能: 执行 checkRequiredBinFiles 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static void checkRequiredBinFiles(File binFolder) {
        for (String requiredFile : REQUIRED_BIN_FILES) {
            if (!containsBinFile(binFolder, requiredFile)) {
                String errorMsg = "Missing required file [payloadMap_rom.bin, cfgRd.bin]in "
                        + getCleanPath(binFolder)
                        + ". Please ensure all required files exist in the directory.";
                Log.error(CodeConstant.ERROR_BIN_REQUIRED_FILE_MISSING, errorMsg);
                exitWithErrorCode(CodeConstant.ERROR_BIN_REQUIRED_FILE_MISSING);
            }
        }
    }

    /**
     * isParameterValidationError
     *
     * @brief
     * 功能: 执行 isParameterValidationError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static boolean isParameterValidationError(int errorCode) {
        // 问题报告单2: 1022(打开BIN文件失败)归入参数检验阶段错误
        return (errorCode == 1005) || (errorCode >= 1007 && errorCode <= 1013) || errorCode == 1016
                || errorCode == CodeConstant.ERROR_OPEN_BIN_FILE_FAILED;
    }

    /**
     * shouldDeferError
     *
     * @brief
     * 功能: 执行 shouldDeferError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean shouldDeferError(int errorCode) {
        return (errorCode >= 1005 && errorCode <= 1015)
                || errorCode == 1016
                || errorCode == 1017
                || errorCode == 1018
                || errorCode == 1019
                || errorCode == 1022
                || (errorCode >= 2007 && errorCode <= 2017)
                || (errorCode >= 3001 && errorCode <= 3006)
                || (errorCode >= 3010 && errorCode <= 3012)
                || (errorCode >= 5001 && errorCode <= 5016);
    }

    /**
     * hasErrorCodeInRange
     *
     * @brief
     * 功能: 执行 hasErrorCodeInRange 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static boolean hasErrorCodeInRange(int minCode, int maxCode) {
        for (Integer code : Log.getRecordedErrorCodes()) {
            if (code != null && code >= minCode && code <= maxCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * 001096/005867：XML、CRC和一致性校验错误完成当前相关校验后停止。
     */
    static boolean hasStopAfterValidationError() {
        return hasErrorCodeInRange(2007, 2017)
                || hasErrorCodeInRange(3001, 3006)
                || hasErrorCodeInRange(3010, 3010)
                || hasErrorCodeInRange(5001, 5016)
                || (deferredErrorCode != null
                        && (deferredErrorCode == 1006
                                || deferredErrorCode == 1017
                                || deferredErrorCode == 1018
                                || deferredErrorCode == 1019
                                || deferredErrorCode == 1022));
    }
    /**
     * beginDeferredErrorCollection
     *
     * @brief
     * 功能: 执行 beginDeferredErrorCollection 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static void beginDeferredErrorCollection() {
        deferErrorExit = true;
        deferredErrorCode = null;
    }

    /**
     * getDeferredErrorCode
     *
     * @brief
     * 功能: 执行 getDeferredErrorCode 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static Integer getDeferredErrorCode() {
        return deferredErrorCode;
    }

    /**
     * finishDeferredError
     *
     * @brief
     * 功能: 执行 finishDeferredError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static void finishDeferredError(int errorCode) {
        deferErrorExit = false;
        deferredErrorCode = null;
        exitWithErrorCode(errorCode);
    }

    /**
     * 清理延迟错误状态。
     */
    static void clearDeferredErrorState() {
        deferErrorExit = false;
        deferredErrorCode = null;
    }



    /**
     * main
     *
     * @brief
     * 功能: 启动 CPM_Gen_Verify 应用并按阶段执行验证流程。
     *
     * @param args 命令行参数
     * @scope external
     * @errno:
     * N/A
     */
    public static void main(String[] args) {
        commandLineStr = String.join(" ", args);

        // 强制控制台使用实际编码，并过滤解析器和生成器的非项目输出。
        configureConsoleEncoding();
        filterXmlParserOutput();
        ARINC653ConfigValidator.setExecuteErrorsOnXsdFailure(true);
        ARINC653ConfigValidator.setExecuteWarningsOnXsdFailure(true);

        ParameterValidationModule parameterValidation = new ParameterValidationModule();
        // TDD-CPMGenVerify-001：输入参数校验。
        ParameterValidationResult parameters = parameterValidation.validateParameters(args);
        if (parameters.auxiliaryCommandHandled) {
            return;
        }

        try {
            // 阶段 1：参数校验及输入输出路径校验。
            // TDD-CPMGenVerify-009：运行状态记录文件的生成，由 validateInputFiles 内部调用 initialize 完成初始化。
            InputFileValidationResult inputFiles = parameterValidation.validateInputFiles(
                    parameters.inputXmlFile, parameters.inputPath,
                    parameters.outputVerifyReportFolder, parameters.outputLogPath);
            File xmlFile = inputFiles.xmlFile;
            String sessionTimestamp = inputFiles.sessionTimestamp;
            String sessionDateTimeStr = inputFiles.sessionDateTimeStr;
            List<String> validationErrors = new ArrayList<>();
            filterVerboseGeneratorOutput();

            // 参数校验完成后，进入后续阶段的延迟错误收集流程。
            beginDeferredErrorCollection();

            // 阶段 2：输入文件扫描和 XML 文件读取解析。
            List<File> binFiles = new InputFileModule().scanBinFiles(parameters.inputPath);
            XmlValidationModule xmlValidation = new XmlValidationModule();
            // TDD-CPMGenVerify-002：XML 文件的读取与解析。
            Module module = xmlValidation.parseModule(getCleanPath(xmlFile));
            if (hasErrorCodeInRange(1017, 1017)
                    || hasErrorCodeInRange(CodeConstant.ERROR_XML_READ_FAILED,
                            CodeConstant.ERROR_XML_READ_FAILED)) {
                new ReportGenerationModule().finishXmlValidationError(
                        parameters.outputVerifyReportFolder, sessionTimestamp,
                        sessionDateTimeStr, commandLineStr);
            }

            // -gen 模式只生成 BIN 文件，不执行后续校验。
            if (parameters.genMode && module != null) {
                generateBinFiles(module, parameters.inputPath, parameters.generatorType);
                Log.close();
                return;
            }

            // 阶段 3～5：XML 正确性、一致性和 CRC 校验。
            boolean xsdValidationPassed = true;
            try {
                // TDD-CPMGenVerify-003：XML 文件正确性校验。
                xsdValidationPassed = xmlValidation.validateXsdOnly(xmlFile, validationErrors);
                if (xsdValidationPassed) {
                    // TDD-CPMGenVerify-004：XML 文件的一致性校验。
                    xmlValidation.validateConsistencyRules(xmlFile, validationErrors);
                    // TDD-CPMGenVerify-005：XML 文件的 CRC 校验。
                    xmlValidation.validateXmlCrc(xmlFile, validationErrors, parameters.enableXmlCrcCheck);
                }
            } catch (Exception e) {
                Log.error(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED,
                        "Correctness verification for XML file Failed, File Path: ["
                                + xmlFile.getAbsolutePath() + "]。");
                validationErrors.add("Verification exception: " + e.getMessage());
                xsdValidationPassed = false;
                exitWithErrorCode(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED);
            }

            int errorCount = countNonWarningErrors(validationErrors);
            if (hasStopAfterValidationError()) {
                new ReportGenerationModule().finishXmlValidationError(
                        parameters.outputVerifyReportFolder, sessionTimestamp,
                        sessionDateTimeStr, commandLineStr);
            }

            if (!xsdValidationPassed) {
                exitWithErrorCode(CodeConstant.ERROR_XML_XSD_VALIDATION_FAILED);
            } else if (errorCount > 0 && !Log.hasOnlyRecordableErrors()) {
                Log.close();
                System.exit(1);
            }

            // 阶段 6：BIN 文件 CRC 校验。
            // TDD-CPMGenVerify-006：BIN 文件的 CRC 校验。
            new BinValidationModule().validateBinCrc(binFiles);

            // 阶段 7：配置数据符合性校验及验证报告生成。
            RuntimeStatusModule runtime = new RuntimeStatusModule();
            runtime.startStage("Configuration Data Consistency Verification");
            ConfigurationConsistencyModule consistency = new ConfigurationConsistencyModule();
            // TDD-CPMGenVerify-007：配置数据的符合性校验。
            ConsistencyValidationResult validationResult = consistency.checkBinConfigConsistency(
                    module, binFiles, xmlFile, parameters.generatorType,
                    parameters.outputVerifyReportFolder, sessionTimestamp);
            if (module != null) {
                ReportGenerationModule reportGeneration = new ReportGenerationModule();
                if (validationResult.hasBinFiles) {
                    // TDD-CPMGenVerify-008：CPM 配置验证报告的生成。
                    reportGeneration.generatorReport(validationResult,
                            parameters.outputVerifyReportFolder, sessionTimestamp,
                            sessionDateTimeStr, commandLineStr);
                } else {
                    reportGeneration.generatorEmptyReport(parameters.outputVerifyReportFolder,
                            sessionTimestamp, sessionDateTimeStr, commandLineStr);
                }
            }
            runtime.completeStage("Configuration Data Consistency Verification",
                    validationResult.stage7ErrorCount);
            runtime.reportProgress(80);

            // 工具结束处理：完成全部报告和日志输出后再处理 3011/3012。
            Log.info("Tool execution completed.");
            runtime.reportProgress(100);
            if (hasErrorCodeInRange(3011, 3012)) {
                clearDeferredErrorState();
                System.out.flush();
                runtime.close();
                System.exit(1);
            }
            runtime.close();
        } catch (OutOfMemoryError e) {
            String reason = formatXmlErrorMessage(e);
            String fileName = XmlParser.getCurrentParsingFileName();
            Log.error(CodeConstant.ERROR_XML_PARSE_FAILED,
                    "XML file " + (fileName != null ? fileName : "unknown")
                            + " parsing failed. the reason : " + reason + ".");
            exitWithErrorCode(CodeConstant.ERROR_XML_PARSE_FAILED);
        } catch (Throwable e) {
            System.err.println("Error occurred during processing: " + e.getMessage());
            e.printStackTrace();
            System.out.flush();
            Log.close();
            System.exit(1);
        }
    }
    /**
     * 统计消息列表中的非告警错误数量。
     * 告警码为4xxx，根据消息内容特征判断（与历史实现保持一致）。
     *
     * @param messages 校验过程中收集的消息列表
     * @return 非告警消息的数量
     */
    static int countNonWarningErrors(java.util.List<String> messages) {
        int count = 0;
        for (String msg : messages) {
            if (!isWarningMessage(msg)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断给定消息是否为告警（4xxx错误码对应的告警类型）。
     */
    private static boolean isWarningMessage(String msg) {
        // 4001: configured shared data not used
        if (msg.contains("configured shared data") && msg.contains("not used")) {
            return true;
        }
        // 4002: configured shared lib not used
        if (msg.contains("configured shared lib") && msg.contains("not used")) {
            return true;
        }
        // 4003: configured partition not scheduled
        if (msg.contains("configured partition") && msg.contains("not scheduled")) {
            return true;
        }
        // 4004: configured app not used
        if (msg.contains("configured app") && msg.contains("not used")) {
            return true;
        }
        // 4005: XML file CRC attribute missing or type error
        if (msg.contains("XML file") && msg.contains("CRC attribute missing or type error")) {
            return true;
        }
        // 4006: CRC validation has been disabled
        if (msg.contains("CRC validation has been disabled") ||
                msg.contains("Proceeding without integrity verification")) {
            return true;
        }
        // 4005: CRC attribute missing or type error (without "XML file" prefix)
        if (msg.contains("CRC attribute missing or type error")) {
            return true;
        }
        return false;
    }

    /**
     *根据错误码决定退出行为
     *
     *错误码分类：
     *- 1001-1018: 参数检查、XML解析失败、XSD校验未通过 -> 不输出log和report，立即终止
     *- 3006-3010: XML CRC校验失败、打开BIN文件失败、BIN CRC校验失败 -> 不输出log和report，立即终止
     *- 3011-3012: 配置数据符合性校验未通过、BIN文件验证未通过 -> 不输出log，但输出report，立即终止
     *- 3013-3014: 配置验证报告生成失败、运行状态记录文件失败 -> 立即终止
     *
     *@param errorCode                错误码
     *@param comparisonResults        比对结果列表（用于3011-3012错误时生成report）
     *@param outputVerifyReportFolder 报告输出文件夹
     *@param sessionTimestamp         会话时间戳
     */
    static void exitWithErrorCode(int errorCode, List<ComparisonResult> comparisonResults,
            String outputVerifyReportFolder, String sessionTimestamp) {

        if (deferErrorExit && shouldDeferError(errorCode)) {
            if (deferredErrorCode == null) {
                deferredErrorCode = errorCode;
            }
            return;
        }


        // 错误码1001-1019、1022: 参数检查、XML解析失败、XSD校验未通过、打开BIN文件失败
        // 不输出log和report，立即终止
        if ((errorCode >= 1001 && errorCode <= 1019)
                || errorCode == CodeConstant.ERROR_OPEN_BIN_FILE_FAILED) {
            System.out.flush();
            Log.closeAndDeleteLogFile();
            System.exit(1);
        }

        // 错误码3006-3010: XML CRC校验失败、打开BIN文件失败、BIN CRC校验失败
        // 不输出log和report，立即终止
        if (errorCode >= 3006 && errorCode <= 3010) {
            System.out.flush();
            Log.closeAndDeleteLogFile();
            System.exit(1);
        }

        // 错误码3011-3012: 配置数据符合性校验未通过、BIN文件验证未通过
        // 不输出log，但输出report后立即终止
        // 001065: 3011-3012 完成全部生成、比对和报告处理后再退出，保留报告和日志。
        if (errorCode >= 3011 && errorCode <= 3012) {
            // 先生成report（如果有比对结果）
            if (comparisonResults != null && !comparisonResults.isEmpty() &&
                    outputVerifyReportFolder != null && sessionTimestamp != null) {
                try {
                    String timestampedReportFile = generateTimestampedReportFileName(outputVerifyReportFolder,
                            sessionTimestamp);
                    ReportGenerator reportGenerator = new ReportGenerator();

                    // 计算统计信息
                    int totalSuccess = 0;
                    int totalFails = 0;
                    for (ComparisonResult result : comparisonResults) {
                        if ("Success".equals(result.getfVerificationResult())) {
                            totalSuccess++;
                        } else {
                            totalFails++;
                        }
                    }

                    // Bug#3: 006329 在 3012 路径中也输出到 CMD/LOG，
                    // 与正常路径格式一致（不带 [CPM_Gen_Verify] 前缀）
                    String verificationStatus3012 = (totalFails == 0) ? "Passed" : "Failed";
                    Log.infoNoPrefix("Verification Status : " + verificationStatus3012);
                    Log.infoNoPrefix("Total Success : " + totalSuccess);
                    Log.infoNoPrefix("Total Fails : " + totalFails);

                    // PR_34: 根据实际的 totalFails 动态判断验证是否通过
                    boolean validationPassed = (totalFails == 0);

                    // Bug#2/Bug#4: 第3参数应与正常路径一致，传入BIN文件名列表字符串，
                    // 而非 "Validation Passed/Failed"。从 comparisonResults 提取唯一 BIN 文件名。
                    java.util.LinkedHashSet<String> binNameSet = new java.util.LinkedHashSet<>();
                    for (ComparisonResult result : comparisonResults) {
                        String binName = result.getfBinaryFileName();
                        if (binName != null && !binName.isEmpty()) {
                            binNameSet.add(binName);
                        }
                    }
                    String binFileNamesStr = binNameSet.isEmpty()
                            ? "" : String.join(", ", binNameSet);

                    reportGenerator.generatorReport(comparisonResults, timestampedReportFile,
                            binFileNamesStr, totalSuccess, totalFails, validationPassed,
                            sessionTimestamp, sessionDateTimeStr, commandLineStr);

                    // 输出report生成成功信息到控制台（不写入log文件）
                    Log.infoLower("succeeded to generate report file.");
                } catch (IOException e) {
                    Log.error(CodeConstant.ERROR_GENERATE_REPORT_FAILED, "failed to generate report file.");
                    exitWithErrorCode(CodeConstant.ERROR_GENERATE_REPORT_FAILED);
                }
            }
            // Bug#5: 001065 要求保留日志文件，不应调用 closeAndDeleteLogFile()。
            // B-07/006329: 刷新 System.out 确保 VerboseOutputStream 缓冲的汇总信息（Verification Status 等）输出到 CMD。
            System.out.flush();
            Log.close();
            System.exit(1);
        }

        // 错误码3013-3014: 配置验证报告生成失败、运行状态记录文件失败
        // 立即终止（不删除已有的log文件）
        if (errorCode >= 3013 && errorCode <= 3014) {
            System.out.flush();
            Log.close();
            System.exit(1);
        }

        // 其他错误码：正常关闭log后退出
        System.out.flush();
        Log.close();
        System.exit(1);
    }

    /**
     *简化版退出方法（用于不需要生成report的场景）
     *改为public以便其他类调用（如ARINC653ConfigValidator遇到1017/1019错误时立即终止）
     *
     *@param errorCode 错误码
     */
    public static void exitWithErrorCode(int errorCode) {
        exitWithErrorCode(errorCode, null, null, null);
    }

    /**
     * generateBinFiles
     *
     * @brief
     * 功能: -gen模式下，根据XML配置生成BIN文件并写入指定目录。
     *       使用与BinCRCValidator一致的CRC-32算法，确保生成的BIN文件能通过CRC校验。
     *       cfgRd.bin          由OsConfigGenerator生成，需替换末尾4字节CRC；
     *       payloadMap_rom.bin 由CpmDeployInfoGenerator生成，需追加4字节CRC。
     *
     * @param[in]  module: XML解析得到的模块对象。
     * @param[in]  outputDir: BIN文件输出目录路径。
     * @param[in]  generatorType: 生成器类型（64big, 64little, 32big, 32little）。
     * @return
     * N/A
     */
    static void generateBinFiles(Module module, String outputDir, String generatorType) {
        System.out.println("========================================");
        System.out.println("  BIN File Generation Mode (-gen)");
        System.out.println("  Generator: " + generatorType);
        System.out.println("  Output: " + outputDir);
        System.out.println("========================================");

        BinaryFileDispatcher.setGeneratorType(generatorType);

        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            boolean created = outputDirFile.mkdirs();
            if (!created && !outputDirFile.isDirectory()) {
                System.err.println("[FAIL] Failed to create output directory: "
                        + outputDirFile.getAbsolutePath());
                return;
            }
        }

        String[] binFileNames = {"cfgRd.bin", "payloadMap_rom.bin"};
        int successCount = 0;

        for (String binFileName : binFileNames) {
            File dummyFile = new File(binFileName);
            BinaryFileDispatcher.GenerationResult result = BinaryFileDispatcher
                    .dispatchGeneration(dummyFile, module);

            if (!result.isSuccess()) {
                System.err.println("[FAIL] Failed to generate: " + binFileName + " - " + result.getErrorMessage());
                continue;
            }

            byte[] generatedBin = result.getGeneratedData();

            // cfgRd.bin生成器已写入CRC32（使用CRC32Utils），需替换为BinCRCValidator算法的CRC
            // payloadMap_rom.bin生成器未写入CRC32，需追加BinCRCValidator算法的CRC
            byte[] dataWithoutCRC;
            if ("cfgrd.bin".equalsIgnoreCase(binFileName)) {
                // cfgRd.bin: 去除生成器写入的4字节CRC
                dataWithoutCRC = new byte[generatedBin.length - 4];
                System.arraycopy(generatedBin, 0, dataWithoutCRC, 0, dataWithoutCRC.length);
            } else {
                // payloadMap_rom.bin: 生成器未写入CRC，全部作为数据
                dataWithoutCRC = generatedBin;
            }

            // 使用与BinCRCValidator一致的CRC-32算法
            long crc32 = calculateCRC32ForBin(dataWithoutCRC);
            byte[] crcBytes = new byte[4];
            crcBytes[0] = (byte) ((crc32 >> 24) & 0xFF);
            crcBytes[1] = (byte) ((crc32 >> 16) & 0xFF);
            crcBytes[2] = (byte) ((crc32 >> 8) & 0xFF);
            crcBytes[3] = (byte) (crc32 & 0xFF);

            // 组合数据 + CRC
            byte[] fullBin = new byte[dataWithoutCRC.length + 4];
            System.arraycopy(dataWithoutCRC, 0, fullBin, 0, dataWithoutCRC.length);
            System.arraycopy(crcBytes, 0, fullBin, dataWithoutCRC.length, 4);

            System.out.println("[INFO] " + binFileName + " CRC32: 0x" + String.format("%08X", crc32));

            File outputFile = new File(outputDirFile, binFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(fullBin);
                System.out.println("[OK] Generated: " + outputFile.getAbsolutePath() +
                        " (" + fullBin.length + " bytes)");
                successCount++;
            } catch (IOException e) {
                System.err.println("[FAIL] Failed to write: " + outputFile.getAbsolutePath() +
                        " - " + e.getMessage());
            }
        }

        System.out.println("========================================");
        System.out.println("  Generation complete: " + successCount + "/" + binFileNames.length + " files");
        System.out.println("========================================");
    }

    /**
     * calculateCRC32ForBin
     *
     * @brief
     * 功能: 使用与BinCRCValidator完全一致的CRC-32算法计算校验值（TR_PR_001）。
     *       算法参数：POLY=0x04C11DB7, INIT=0xFFFFFFFF, RefIn=FALSE, RefOut=FALSE, XOROUT=0。
     *
     * @param[in]  data: 待计算CRC的数据（不含CRC字段）。
     * @return
     * long: CRC32无符号整数值。
     */
    private static long calculateCRC32ForBin(byte[] data) {
        final long POLY = 0x04C11DB7L;
        final long INIT = 0xFFFFFFFFL;
        final long XOROUT = 0x00000000L;
        long crc = INIT;

        for (byte b : data) {
            // RefIn=FALSE：不反转输入字节，按最高有效位方向处理。
            crc ^= ((long) (b & 0xFF)) << 24;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80000000L) != 0) {
                    crc = (crc << 1) ^ POLY;
                } else {
                    crc = crc << 1;
                }
                crc &= 0xFFFFFFFFL;
            }
        }

        return (crc ^ XOROUT) & 0xFFFFFFFFL;
    }

    /**
     * resolveConsoleEncoding
     *
     * @brief
     * 功能: 执行 resolveConsoleEncoding 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String resolveConsoleEncoding() {
        // 在 JDK 18+ 默认 UTF-8 环境下，优先使用控制台相关编码属性，最后才回退到 file.encoding
        String encoding = System.getProperty("stdout.encoding");
        if (encoding != null && !encoding.isEmpty()) {
            return encoding;
        }
        encoding = System.getProperty("sun.stdout.encoding");
        if (encoding != null && !encoding.isEmpty()) {
            return encoding;
        }
        encoding = System.getProperty("sun.jnu.encoding");
        if (encoding != null && !encoding.isEmpty()) {
            return encoding;
        }
        encoding = System.getProperty("file.encoding");
        if (encoding != null && !encoding.isEmpty()) {
            return encoding;
        }
        return Charset.defaultCharset().name();
    }

    /**
     *强制System.out/err使用控制台实际编码，自动适配GBK和UTF-8控制台。
     *通过调用Windows API获取当前控制台代码页，避免在不同环境下乱码。
     */
    static void configureConsoleEncoding() {
        try {
            String charsetName = detectConsoleEncoding();

            // 使用检测到的编码重新包装标准输出/错误流
            PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, charsetName);
            PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err), true, charsetName);
            System.setOut(out);
            System.setErr(err);
        } catch (Exception e) {
            // 如果失败则保持JVM默认编码，避免影响后续流程
        }
    }

    /**
     *检测当前Windows控制台的实际编码
     *
     *@return 编码名称，如"UTF-8"或"GBK"
     */
    private static String detectConsoleEncoding() {
        try {
            // 调用Windows API获取控制台输出代码页
            int codePage = WinKernel32.INSTANCE.GetConsoleOutputCP();

            // 根据代码页返回对应的Java编码名称
            switch (codePage) {
                case 65001: // UTF-8
                    return "UTF-8";
                case 936: // GBK (简体中文)
                    return "GBK";
                case 950: // Big5 (繁体中文)
                    return "Big5";
                case 437: // US
                    return "Cp437";
                case 850: // Western European
                    return "Cp850";
                default:
                    // 其他代码页尝试使用Cp前缀
                    return "Cp" + codePage;
            }
        } catch (Exception e) {
            // 如果API调用失败（非Windows或其他异常），回退到UTF-8
            return "UTF-8";
        }
    }

    /**
     *过滤XML解析器的默认输出
     *XML解析器会输出 [Warning] 和 [Fatal Error] 格式的消息，这不是项目定义的格式，需要过滤掉
     */
    /**
     * 临时屏蔽生成器直接输出的详细地址/段大小调试信息。
     * 标准工具日志、阶段信息、错误和进度信息仍正常显示。
     */
    static void filterVerboseGeneratorOutput() {
        final PrintStream originalOut = System.out;
        // B-07/006329: 保存原始控制台输出流到 Log，使 infoNoPrefix 能绕过过滤直接输出到 CMD。
        Log.setConsoleOut(originalOut);
        final String charsetName = detectConsoleEncoding();
        try {
            class VerboseOutputStream extends java.io.OutputStream {
                private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream(256);

                /**
                 * write
                 *
                 * @brief
                 * 功能: 执行 write 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void write(int b) throws IOException {
                    buffer.write(b);
                    if (b == '\n') {
                        flushLines(false);
                    }
                }

                /**
                 * write
                 *
                 * @brief
                 * 功能: 执行 write 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    buffer.write(b, off, len);
                    for (int i = off; i < off + len; i++) {
                        if (b[i] == (byte) '\n') {
                            flushLines(false);
                            break;
                        }
                    }
                }

                /**
                 * flushLines
                 *
                 * @brief
                 * 功能: 执行 flushLines 相关处理。
                 *
                 * @scope internal
                 * @errno:
                 * N/A
                 */
                private void flushLines(boolean force) throws IOException {
                    byte[] data = buffer.toByteArray();
                    int start = 0;
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] == (byte) '\n') {
                            writeLine(data, start, i - start + 1);
                            start = i + 1;
                        }
                    }
                    buffer.reset();
                    if (start < data.length) {
                        buffer.write(data, start, data.length - start);
                    } else if (force) {
                        buffer.reset();
                    }
                }

                /**
                 * writeLine
                 *
                 * @brief
                 * 功能: 执行 writeLine 相关处理。
                 *
                 * @scope internal
                 * @errno:
                 * N/A
                 */
                private void writeLine(byte[] data, int offset, int length) throws IOException {
                    String line = new String(data, offset, length,
                            java.nio.charset.Charset.forName(charsetName)).trim();
                    // B-07/006329: 放行 infoNoPrefix 生成的时间戳格式行（如 [2026-07-29 14:15:46][Info] ...），
                    // 使 Verification Status / Total Success / Total Fails 汇总信息同时输出到 CMD 和 LOG。
                    if ((line.startsWith("[CPM_Gen_Verify]") || line.startsWith("[ STAGE")
                            || line.startsWith("文件名:") || line.startsWith("校验结果:")
                            || line.startsWith("生成结果:")
                            || (line.startsWith("[20") && line.indexOf("][Info]") > 0))
                            && !line.contains("[部署地址计算]")) {
                        originalOut.write(data, offset, length);
                    }
                }

                /**
                 * flush
                 *
                 * @brief
                 * 功能: 执行 flush 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void flush() throws IOException {
                    flushLines(true);
                    originalOut.flush();
                }
            }

            System.setOut(new PrintStream(new VerboseOutputStream(), true, charsetName));
        } catch (Exception e) {
            // 过滤器初始化失败时保留原控制台输出。
        }
    }
    /**
     * filterXmlParserOutput
     *
     * @brief
     * 功能: 执行 filterXmlParserOutput 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static void filterXmlParserOutput() {
        try {
            final PrintStream originalErr = System.err;
            final String charsetName = detectConsoleEncoding();

            class LineFilteringOutputStream extends java.io.OutputStream {
                private final java.io.OutputStream delegate;
                private final java.nio.charset.Charset charset;
                private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream(256);

                LineFilteringOutputStream(java.io.OutputStream delegate, java.nio.charset.Charset charset) {
                    this.delegate = delegate;
                    this.charset = charset;
                }

                /**
                 * write
                 *
                 * @brief
                 * 功能: 执行 write 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void write(int b) throws IOException {
                    buffer.write(b);
                    if (b == '\n') {
                        flushBufferLines();
                    }
                }

                /**
                 * write
                 *
                 * @brief
                 * 功能: 执行 write 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    buffer.write(b, off, len);
                    // 如果包含换行则尝试按行刷新
                    if (indexOfNewline(b, off, len) >= 0) {
                        flushBufferLines();
                    }
                }

                /**
                 * indexOfNewline
                 *
                 * @brief
                 * 功能: 执行 indexOfNewline 相关处理。
                 *
                 * @scope internal
                 * @errno:
                 * N/A
                 */
                private int indexOfNewline(byte[] b, int off, int len) {
                    int end = off + len;
                    for (int i = off; i < end; i++) {
                        if (b[i] == (byte) '\n') {
                            return i;
                        }
                    }
                    return -1;
                }

                /**
                 * flushBufferLines
                 *
                 * @brief
                 * 功能: 执行 flushBufferLines 相关处理。
                 *
                 * @scope internal
                 * @errno:
                 * N/A
                 */
                private void flushBufferLines() throws IOException {
                    byte[] data = buffer.toByteArray();
                    int start = 0;
                    for (int i = 0; i < data.length; i++) {
                        if (data[i] == (byte) '\n') {
                            int lineLen = i - start + 1;
                            String line = new String(data, start, lineLen, charset);
                            String trimmed = line.trim();
                            if (!(trimmed.startsWith("[Warning]") || trimmed.startsWith("[Fatal Error]"))) {
                                delegate.write(data, start, lineLen);
                            }
                            start = i + 1;
                        }
                    }
                    buffer.reset();
                    if (start < data.length) {
                        buffer.write(data, start, data.length - start);
                    }
                }

                /**
                 * flush
                 *
                 * @brief
                 * 功能: 执行 flush 相关处理。
                 *
                 * @scope external
                 * @errno:
                 * N/A
                 */
                @Override
                public void flush() throws IOException {
                    // flush时把剩余内容也按当前内容过滤一次
                    if (buffer.size() > 0) {
                        String line = buffer.toString(charset.name());
                        String trimmed = line.trim();
                        if (!(trimmed.startsWith("[Warning]") || trimmed.startsWith("[Fatal Error]"))) {
                            delegate.write(buffer.toByteArray());
                        }
                        buffer.reset();
                    }
                    delegate.flush();
                }
            }

            java.io.OutputStream filteringStream = new LineFilteringOutputStream(originalErr,
                    java.nio.charset.Charset.forName(charsetName));
            PrintStream filteredErr = new PrintStream(filteringStream, true, charsetName);
            System.setErr(filteredErr);
        } catch (Exception e) {
            // 如果设置失败，保持原样
        }
    }

    /**
     * isDuplicateAttributeError
     *
     * @brief
     * 功能: 执行 isDuplicateAttributeError 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static boolean isDuplicateAttributeError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null) {
                if (msg.contains("already specified") || msg.contains("was already specified")
                        || msg.contains("Duplicate attribute") || msg.contains("Attribute") && msg.contains("duplicate")
                        || msg.contains("重复")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * extractElementAndAttributeFromDuplicateMsg
     *
     * @brief
     * 功能: 执行 extractElementAndAttributeFromDuplicateMsg 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String[] extractElementAndAttributeFromDuplicateMsg(String msg) {
        if (msg == null) {
            return new String[] { "unknown", "unknown" };
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Attribute \\\"([^\\\"]+)\\\".*element \\\"([^\\\"]+)\\\"",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(msg);
        if (m.find()) {
            return new String[] { m.group(2), m.group(1) };
        }
        // 兜底：只提取Attribute
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("Attribute \\\"([^\\\"]+)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(msg);
        if (m2.find()) {
            return new String[] { "unknown", m2.group(1) };
        }
        return new String[] { "unknown", "unknown" };
    }

    /**
     *Minimal Kernel32 mapping for WriteConsoleW/GetStdHandle/GetConsoleOutputCP.
     */
    private interface WinKernel32 extends Library {
        WinKernel32 INSTANCE = Native.load("kernel32", WinKernel32.class);
        int STD_OUTPUT_HANDLE = -11;

        WinNT.HANDLE GetStdHandle(int nStdHandle);

        boolean WriteConsoleW(WinNT.HANDLE hConsoleOutput, char[] lpBuffer, int nNumberOfCharsToWrite,
                IntByReference lpNumberOfCharsWritten, Pointer lpReserved);

        /**
         *获取当前控制台输出代码页
         *
         *@return 代码页编号，如 65001(UTF-8), 936(GBK), 437(US)等
         */
        int GetConsoleOutputCP();
    }

    /**
     *直接调用 Windows WriteConsoleW 输出 UTF-16 字符，绕过 OEM 936 缺字问题。
     */
    private static boolean writeConsoleWide(String text) {
        try {
            WinNT.HANDLE hConsole = WinKernel32.INSTANCE.GetStdHandle(WinKernel32.STD_OUTPUT_HANDLE);
            if (hConsole == null || WinNT.INVALID_HANDLE_VALUE.equals(hConsole)) {
                return false;
            }
            IntByReference written = new IntByReference();
            boolean ok = WinKernel32.INSTANCE.WriteConsoleW(hConsole, text.toCharArray(), text.length(), written,
                    Pointer.NULL);
            return ok && written.getValue() == text.length();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     *PR_01: 打印版权和版本信息
     *在运行辅助命令之前打印版权和版本信息
     *使用UTF-8编码输出©符号，配合启动脚本设置的代码页65001
     */
    static void printCopyrightAndVersion() {
        try {
            String copyrightLine = getInfoPrefix() + COPYRIGHT + "\n";

            // 优先用 WriteConsoleW 输出宽字符，绕过 OEM 936 缺字问题
            String wideContent = getStartupVersionLine() + "\n" + copyrightLine;
            if (writeConsoleWide(wideContent)) {
                return;
            }

            // 回退：按控制台编码写字节
            String charsetName = resolveConsoleEncoding();
            if (charsetName.isEmpty()) {
                charsetName = "GBK";
            }

            @SuppressWarnings("resource")
            java.io.FileOutputStream fos = new java.io.FileOutputStream(java.io.FileDescriptor.out);
            fos.write((getStartupVersionLine() + "\n").getBytes(charsetName));
            fos.write(copyrightLine.getBytes(charsetName));
            fos.flush();
        } catch (Exception e) {
            // 如果失败，回退到普通输出
            System.out.println(getStartupVersionLine());
            System.out.println(getInfoPrefix() + COPYRIGHT);
        }
    }

    /**
     * getStartupVersionLine
     *
     * @brief
     * 功能: 执行 getStartupVersionLine 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getStartupVersionLine() {
        return TOOL_NAME + " Tool " + TOOL_VERSION;
    }

    /**
     * getInfoPrefix
     *
     * @brief
     * 功能: 执行 getInfoPrefix 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getInfoPrefix() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "[" + TOOL_NAME + "][" + timestamp + "][Info] ";
    }

    /**
     * getParameterInfoPrefix
     *
     * @brief
     * 功能: 执行 getParameterInfoPrefix 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    private static String getParameterInfoPrefix() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "[" + timestamp + "][Info] ";
    }

    /**
     * CPMVerify-TOR-001570/004786: 打印用户输入参数信息
     * 在配置验证命令执行前打印用户输入的参数
     */
    static void printUserInputParameters(String[] args) {
        System.out.println(getParameterInfoPrefix() + "Parameter 1:" + args[0]);
        System.out.println(getParameterInfoPrefix() + "Parameter 2:" + args[1]);
        System.out.println(getParameterInfoPrefix() + "Parameter 3:" + args[2]);
        System.out.println(getParameterInfoPrefix() + "Parameter 4:" + args[3]);
    }

    /**
     *打印工具版本信息
     *格式:CPM_Gen_Verify CI-V
     *其中：
     *- CI: 构型标识 (TLF42200105A)
     *- V: 版本号 (AA表示首次发布版本，AB表示第二版本，以此类推)
     */
    static void printVersion() {
        // String timestamp =
        // LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd
        // HH:mm:ss"));
        // String logPrefix = "[CPM_Gen_Verify][" + timestamp + "][Info] ";
        String configurationId = "TLF42200105A"; // 构型标识
        String version = "AF"; // 版本号

        String versionInfo = String.format("CPM_Gen_Verify %s-%s", configurationId, version);
        System.out.println(versionInfo);
    }

    /**
     *打印工具能够检查的所有错误类型
     *格式: 序号 [Error/Warning][Code]: ErrorMessage
     */
    static void printErrorTypes() {
        // 参数和文件路径相关错误
        System.out.println("1  [Error][1001]: Input auxiliary parameters error ,except for -v,-h,-le.");
        System.out.println("2  [Error][1002]: Invalid parameters. Use the -h command for more help.");
        System.out.println("3  [Error][1003]: Input parameters number error ,please re-enter parameters.");
        System.out.println(
                "4  [Error][1005]: File path invalid: '{input_xml_cfg_file } '. File does not exist.");
        System.out.println(
                "5  [Error][1006]: Missing required file  {input_xml_file }in {input_xml_cfg_folder}. Please ensure all required files exist in the directory.");
        System.out.println("6  [Error][1007]: input BIN file path(2nd parameter) error ,please re-enter parameters.");
        System.out.println("7  [Error][1008]: Missing required file [payloadMap_rom.bin, cfgRd.bin]in {input_bin_cfg_folder}. Please ensure all required files exist in the directory.");
        System.out.println(
                "8  [Error][1009]: Folderpath{output_verify_report_folder} invalid: Directory does not exist.");
        System.out
                .println("9  [Error][1010]: Write permission denied for folder path: '{output_verify_report_folder}'.");
        System.out.println(
                "10 [Error][1011]: Output running status file path(4nd parameter) error ,please re-enter parameters.");
        System.out.println("11 [Error][1012]: Folder path invalid: '{output_log_folder}'. Directory does not exist.");
        System.out.println("12 [Error][1013]: Write permission denied for folder path: '{output_log_folder}'.");
        System.out.println(
                "13 [Error][1014]: Redundant XML file '{fileName}' detected.The sibling directory of the CPM_XX_Module.xml needs to be checked and removed.");
        System.out.println(
                "14 [Error][1015]: Redundant BIN file '{fileName}'found in<innut_bin_cfg_folder>'.Please remove it or ensure the directory only contains the required files [payloadMap_rom.bin、cfgRd.bin].");
        System.out.println(
                "15 [Error][1016]: XML file{file name} does not match name rules,rules:CPM_XX_Module.xml,the 1st X means left and right ,value for L or R,the 2nd X means number,value for 0-9.");
        System.out.println("16 [Error][1017]: XML file {fileName} parsing failed. the reason : {reason}.");
        System.out.println("17 [Error][1018]: Correctness verification for XML file Failed, File Path: [{full_file_path}]。");
        System.out.println("18 [Error][1019]: XML file {fileName} read failed.");
        System.out.println("19 [Error][1022]: failed to open Bin File. the folder path of the file: {input_bin_cfg_folder }.");

        // XML元素和属性相关错误 (2007-2017)
        System.out.println(
                "20 [Error][2007]: [File: {fileName}, Line: {lineNumber}] undefined XML element {element name }.");
        System.out.println(
                "21 [Error][2008]: [File: {fileName}, Line: {lineNumber}] undefined XML element attribute {attribute name}.");
        System.out.println(
                "22 [Error][2009]: [File: {fileName}, Line: {lineNumber}] element {element name} occurs {actualCount} time(s) less than minimum {minOccurs} time(s).");
        System.out.println(
                "23 [Error][2010]: [File: {fileName}, Line: {lineNumber}] element {element name} occurs {actualCount} time(s) greater than {maxOccurs} time(s).");
        System.out.println(
                "24 [Error][2011]: [File: {fileName}, Line: {lineNumber}] XML element  {element name } attribute  {attribute name}missing.");
        System.out.println(
                "25 [Error][2012]: [File: {fileName}, Line: {lineNumber}] XML element   {element name }  attribute   {attribute name}  redundancy .");
        System.out.println(
                "26 [Error][2013]: [File: {fileName}, Line: {lineNumber}] XML element   {element name }   attribute   {attribute name}  type error.");
        System.out.println(
                "27 [Error][2014]: [File: {fileName}, Line: {lineNumber}] element {element name }  attribute  {attribute name}   is out of range,range is [{min}，{max}].");
        System.out.println(
                "28 [Error][2015]: [File: {fileName}, Line: {lineNumber}] Required element '{elementName}' is missing in parent element '{parentElement}'.");
        System.out.println(
                "29 [Error][2016]: [File: {fileName}, Line: {lineNumber}] Length of {invalidValue} is not allowed for attribute {attributeName} in element {elementName}. Length range: {minLength} to {maxLength}.");
        System.out.println(
                "30 [Error][2017]: [File: {fileName}, Line: {lineNumber}] Value {attributeValue} is not allowed for attribute {attributeName} in element {elementName}. Valid values: {enumValue, enumValue}.");

        // 配置约束和验证相关错误 (3001-3006, 3010-3014)
        System.out.println(
                "31 [Error][3001]: [File: {fileName}, Line: {lineNumber}] the element  {element name }    attribute   {attribute name}   value {number} is not unique.");
        System.out.println(
                "32 [Error][3002]:  [File: {fileName}, Line: {lineNumber}] the element  {element name }  attribute  {attribute name}    value {number} of reference relation is error.");
        System.out.println(
                "33 [Error][3003]: [File: {fileName}, Line: {lineNumber}] the element  {element name }   address{value} configuration not page-aligned.");
        System.out.println(
                "34 [Error][3004]:  [File: {fileName}, Line: {lineNumber}] address{value}ut of range,the range is [(min)，(max)].");
        System.out.println(
                "35 [Error][3005]: [File: {fileName}, Line: {lineNumber}] the two address intervals with exclusion relation configuration is error.");
        System.out.println(
                "36 [Error][3006]: XML file {fileName} failed CRC validation. CRC32: 0x[original_crc]; Computed CRC32: 0x[computed_crc]. File Path: {full_file_path}.");
        System.out.println(
                "37 [Error][3010]: BIN file failed CRC validation. CRC32: 0x[original_crc]; Computed CRC32: 0x[computed_crc]. File Path: {full_file_path}  .");
        System.out.println(
                "38 [Error][3011]: Field [field_name] verification failed: Offset = [location] Actual Value = \"binary_value\" Expected Value = \"computed_value\".");
        System.out.println(
                "39 [Error][3012]: Bin file validation failed. File Path: {full_file_path}.");
        System.out.println("40 [Error][3013]: failed to generate report file.");
        System.out.println("41 [Error][3014]: failed to generate running status file.");

        // 警告码 (4001-4006)
        System.out.println(
                "42 [Warning][4001]: [File: {fileName}, Line: {lineNumber}] configured shared data {name} not used.");
        System.out.println(
                "43 [Warning][4002]: [File: {fileName}, Line: {lineNumber}] configured shared lib  {name}    not used.");
        System.out.println(
                "44 [Warning][4003]: [File: {fileName}, Line: {lineNumber}] configured partition  {name}   not scheduled.");
        System.out.println(
                "45 [Warning][4004]: [File: {fileName}, Line: {lineNumber}] configured app {name}   not used.");
        System.out.println("46 [Warning][4005]: XML file {fileName} CRC attribute missing or type error.");
        System.out.println(
                "47 [Warning][4006]: CRC validation has been disabled for the input XML files. Proceeding without integrity verification. File Path: {full_file_path}。");

        // 一致性和配置约束错误 (5001-5016)
        System.out.println("48 [Error][5001]: XML files failed consistency validation.");
        System.out.println("49 [Error][5002]: The RequiredMemorySize value {Attribute Value} of the partition {Partition name} is less than the sum {Calculated Value} of the application and shared libraries [excluding text,rodata] referenced by it.");
        System.out.println("50 [Error][5003]: The port direction {Attribute Value} of port {Port name} does not match the port protocol type {Attribute Value}.");
        System.out.println("51 [Error][5004]: The VirtualAddress of the shared data and partitionSpaceSize are both not configured; the operating system cannot automatically allocate a virtual address.");
        System.out.println("52 [Error][5005]: The virtual address of shared data {shareData name} is not within the range [{start address},{end address}].");
        System.out.println("53 [Error][5006]: The size attribute value {Attribute Value} of kernelMemoryRegion is less than the sum {SumNum} of segments under CoreOSDescription/MemorySize.");
        System.out.println("54 [Error][5007]: The size attribute value {Attribute Value} of portRegion is less than the sum {SumNum} of the space required by the ports configured in all partitions and virtual partitions.");
        System.out.println("55 [Error][5008]: The size attribute value {Attribute Value} of libRegion is less than the sum {SumNum} of the space required by all shared libraries.");
        System.out.println("56 [Error][5009]: The size attribute value {Attribute Value} of HmLogRegion is less than the sum {SumNum} of the space required by all health monitoring logs.");
        System.out.println("57 [Error][5010]: In Online, partition {partition name} must have the Base Address attribute configured.");
        System.out.println("58 [Error][5011]: In Online, the range [{start address},{start address + size}] of partition {partition name} is not within the range [{start address},{start address + size}] of {kernelRegion name}.");
        System.out.println("59 [Error][5012]: When Payloads//PartitionPayload/@Online is true, the element HardwareConfiguration/kernelRegion must be configured.");
        System.out.println("60 [Error][5013]: The name of the system shared library {shared library name} is incorrectly configured; it must be configured as \"pos\".");
        System.out.println("61 [Error][5014]: The system shared library is not unique.");
        System.out.println("62 [Error][5015]: Partition {Partition name} cannot be configured under multiple Core elements.");
        System.out.println("63 [Error][5016]: The number of elements under Ports/QueuingPort and Ports/SamplingPort exceeds 5120.");
    }

    /**
     *打印工具帮助信息
     */
    static void printHelp() {
        System.out.println("The syntax of this command is:\n" + //
                "a）Configuration Verification Commands\n" + //
                "java -jar CPM_Gen_Verify.jar <input_xml_cfg_file> <input_bin_cfg_folder> <output_verify_report_folder> <output_log_ folder> [options]\n"
                + //
                "Parameter List: \n" + //
                "1)<input_xml_cfg_folder>: Required. Points to the XML format configuration file generated by GPT, consisting of the file path and file name.\n"
                + //
                "2)<input_bin_cfg_folder>: Required. A loadable file that points to the CPM_Gen output, consisting of a file path.\n"
                + //
                "3)<output_verify_report_folder>: Required. Specifies the folder path where the output CPM configuration verification report will be saved. \n"
                + //
                "4)<output_log_folder>: Required. Specifies the folder path where the runtime status log files will be saved. \n"
                + //
                "5)[options]: Optional. Includes: -nc to disable CRC checksum verification for input XML files.\n" + //
                "b)Accessibility Commands\n" + //
                "java –jar CPM_Gen_Verify.jar <command>\n" + //
                "Command include:\n" + //
                "1)-v: Prints the current version of the tool in the console window;\n" + //
                "2)-le: Prints all error types that the tool can check in the console window;\n" + //
                "3)-h: Prints all available options, input format, and a brief description of the functions of the options in the console window.");
    }

    /**
     *检查是否存在冗余的 XML 文件
     *只允许存在主 XML 文件和其通过 xi:include 引用的文件
     *
     *@param mainXmlFile 主 XML 文件
     */
    static void checkRedundantXmlFiles(File mainXmlFile) {
        try {
            File xmlFolder = mainXmlFile.getParentFile();
            if (xmlFolder == null) {
                return;
            }

            // 1. 构建合法文件集合（主文件 + 引用的文件）
            Set<String> validFiles = new HashSet<>();
            validFiles.add(mainXmlFile.getName()); // 添加主文件

            // 2. 解析主 XML 文件，提取所有 xi:include 的 href 属性
            Set<String> referencedFiles = extractXIncludeReferences(mainXmlFile);
            if (referencedFiles == null) {
                // 参数校验阶段只检查目录结构；XML语法错误统一在XML解析阶段报告1017。
                return;
            }
            validFiles.addAll(referencedFiles);

            // 3. 扫描目录中的所有 XML 文件
            File[] allFiles = xmlFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (allFiles == null) {
                return;
            }

            // 4. 检查是否存在冗余文件
            for (File file : allFiles) {
                String fileName = file.getName();
                if (!validFiles.contains(fileName)) {
                    String errorMsg = "Redundant XML file '" + fileName + "' detected. " +
                            "The sibling directory of the " + mainXmlFile.getName() +
                            " needs to be checked and removed.";
                    Log.error(CodeConstant.ERROR_REDUNDANT_XML_FILE, errorMsg);
                    exitWithErrorCode(CodeConstant.ERROR_REDUNDANT_XML_FILE);
                }
            }

        } catch (Exception e) {
            Log.warning(CodeConstant.WARNING_XML_CRC_ATTRIBUTE_MISSING,
                    "无法检查冗余 XML 文件: " + e.getMessage());
        }
    }

    /**
     *提取 XML 文件中所有 xi:include 元素的 href 属性值
     *递归提取主文件及其引用文件的所有 XInclude 引用
     *
     *@param xmlFile XML 文件
     *@return 引用的文件名集合
     */
    private static Set<String> extractXIncludeReferences(File xmlFile) {
        Set<String> references = new HashSet<>();

        try {
            // 使用支持环境变量/重复属性容错的解析器，避免单个2012导致提前退出
            Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

            // 查找所有 xi:include 元素（XInclude 命名空间）
            // 命名空间 URI: http://www.w3.org/2001/XInclude
            NodeList includeNodes = doc.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "include");

            for (int i = 0; i < includeNodes.getLength(); i++) {
                Element includeElement = (Element) includeNodes.item(i);
                String href = includeElement.getAttribute("href");

                if (!href.trim().isEmpty()) {
                    // 解析环境变量
                    String resolvedHref = XmlParser.resolveEnvVariables(href);
                    // 提取文件名（去掉路径部分）
                    String fileName = new File(resolvedHref).getName();
                    references.add(fileName);
                }
            }

            // 递归处理引用的文件
            File parentFolder = xmlFile.getParentFile();
            for (String refFileName : new ArrayList<>(references)) {
                // 首先尝试在parentFolder下找
                File refFile = new File(parentFolder, refFileName);
                if (refFile.exists()) {
                    Set<String> nestedRefs = extractXIncludeReferences(refFile);
                    references.addAll(nestedRefs);
                }
            }

        } catch (Throwable e) {
            // 参数校验阶段只尝试提取引用文件；解析错误留给XML解析阶段统一输出1017。
            return null;
        }

        return references;
    }

    /**
     *检查XML文件夹中是否存在必需的文件
     *1. 检查 CPM_XX_module.xml 主文件是否存在（已在调用前检查）
     *2. 检查主文件引用的所有 XML 文件是否存在
     *3. 检查基础必需文件是否存在（如 coreOS.xml）
     *
     *@param mainXmlFile 主 XML 文件（CPM_XX_module.xml）
     *@param xmlFolder   XML文件所在文件夹
     */
    static void checkRequiredFiles(File mainXmlFile, File xmlFolder) {
        // 1. 检查主 module 文件是否存在（通常已在调用前检查，这里再次确认）
        if (!mainXmlFile.exists()) {
            String errorMsg = String.format(
                    "Missing required file %s in %s. Please ensure all required files exist in the directory.",
                    mainXmlFile.getName(),
                    getCleanPath(xmlFolder));
            Log.error(CodeConstant.ERROR_XML_FILE_MISSING, errorMsg);
            // 错误码1006: 不输出log和report
            exitWithErrorCode(CodeConstant.ERROR_XML_FILE_MISSING);
        }

        // 2. 提取主文件引用的所有 XML 文件（通过 xi:include 元素），返回完整路径
        Map<String, File> referencedFilesMap = extractXIncludeReferencesWithFullPath(mainXmlFile);
        if (referencedFilesMap == null) {
            // 主XML无法解析时不在参数校验阶段重复报告1017或误报缺失引用文件。
            return;
        }

        // 3. 检查所有引用的文件是否存在
        for (Map.Entry<String, File> entry : referencedFilesMap.entrySet()) {
            String originalHref = entry.getKey();
            File resolvedFile = entry.getValue();
            if (!resolvedFile.exists()) {
                String errorMsg = String.format(
                        "Missing required file %s in %s. Please ensure all required files exist in the directory.",
                        originalHref,
                        getCleanPath(resolvedFile.getParentFile()));
                Log.error(CodeConstant.ERROR_XML_FILE_MISSING, errorMsg);
                // 错误码1006: 不输出log和report
                exitWithErrorCode(CodeConstant.ERROR_XML_FILE_MISSING);
            }
        }
    }

    /**
     *提取 XML 文件中所有 xi:include 元素的 href 属性值，返回解析后的完整文件路径
     *支持环境变量格式 $(VAR_NAME)
     *
     *@param xmlFile XML 文件
     *@return 原始href -> 解析后的File对象的映射
     */
    private static Map<String, File> extractXIncludeReferencesWithFullPath(File xmlFile) {
        Map<String, File> references = new java.util.LinkedHashMap<>();

        try {
            // 使用支持环境变量/重复属性容错的解析器，避免单个2012导致提前退出
            Document doc = XmlParser.parseXmlWithEnvSupport(xmlFile, false);

            NodeList includeNodes = doc.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "include");

            for (int i = 0; i < includeNodes.getLength(); i++) {
                Element includeElement = (Element) includeNodes.item(i);
                String href = includeElement.getAttribute("href");

                if (!href.trim().isEmpty()) {
                    // 解析环境变量
                    String resolvedHref = XmlParser.resolveEnvVariables(href);
                    File resolvedFile;
                    // 如果解析后的路径是绝对路径，直接使用；否则相对于xmlFile父目录
                    if (new File(resolvedHref).isAbsolute()) {
                        resolvedFile = new File(resolvedHref);
                    } else {
                        resolvedFile = new File(xmlFile.getParentFile(), resolvedHref);
                    }
                    references.put(href, resolvedFile);

                    // 递归处理引用的文件
                    if (resolvedFile.exists()) {
                        Map<String, File> nestedRefs = extractXIncludeReferencesWithFullPath(resolvedFile);
                        references.putAll(nestedRefs);
                    }
                }
            }

        } catch (Throwable e) {
            // 参数校验阶段只尝试提取引用文件；解析错误留给XML解析阶段统一输出1017。
            return null;
        }

        return references;
    }

    /**
     *生成带时间戳的报告文件名
     *例如: 在文件夹 ./reports 中生成 CPM_Gen_Verify_Report_20251017_120530.csv
     *
     *@param reportFolderPath 报告文件夹路径
     *@param timestamp        时间戳字符串（格式：yyyyMMdd_HHmmss）
     *@return 带时间戳的完整文件路径
     */
    static String generateTimestampedReportFileName(String reportFolderPath, String timestamp) {
        // 构建新文件名: CPM_Gen_Verify_Report_时间戳.csv
        String newFileName = "CPM_Gen_Verify_Report_" + timestamp + ".csv";

        // 拼接文件夹路径和文件名，使用getCanonicalPath去除冗余的.\等
        File reportFolder = new File(reportFolderPath);
        File reportFile = new File(reportFolder, newFileName);
        try {
            return reportFile.getCanonicalPath();
        } catch (IOException e) {
            return reportFile.getAbsolutePath();
        }
    }

    /**
     *获取文件的规范路径（去除冗余的.\等）
     *
     *@param file 文件对象
     *@return 规范路径，如果获取失败则返回绝对路径
     */
    static String getCleanPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     *判断异常是否为文件读取错误（如权限被拒绝、文件不存在等）
     */
    private static boolean isFileReadError(Throwable e) {
        // 检查异常类型
        if (e instanceof java.io.FileNotFoundException
                || e instanceof java.security.AccessControlException
                || e instanceof java.nio.file.AccessDeniedException) {
            return true;
        }

        // 检查完整的异常链（递归检查所有cause）
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.io.IOException
                    || cause instanceof java.io.FileNotFoundException
                    || cause instanceof java.security.AccessControlException
                    || cause instanceof java.nio.file.AccessDeniedException) {
                return true;
            }

            // 检查cause的消息
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                String lowerCauseMessage = causeMessage.toLowerCase();
                if (lowerCauseMessage.contains("拒绝访问")
                        || lowerCauseMessage.contains("access denied")
                        || lowerCauseMessage.contains("permission denied")
                        || lowerCauseMessage.contains("denied")
                        || lowerCauseMessage.contains("no such file")
                        || lowerCauseMessage.contains("cannot find")) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        // 检查异常消息中是否包含文件读取相关的关键词
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("拒绝访问")
                    || lowerMessage.contains("access denied")
                    || lowerMessage.contains("permission denied")
                    || lowerMessage.contains("denied")
                    || lowerMessage.contains("permission")
                    || lowerMessage.contains("no such file")
                    || lowerMessage.contains("cannot find")) {
                return true;
            }

            // PR_03: 如果异常消息看起来像是文件路径（只包含路径，没有其他错误描述）
            // 这通常意味着底层的文件访问错误被包装了，异常消息只剩下文件路径
            // 例如："E:\CPM_verify\CPMs_631\CPM_L1\APP_ATC_INSTANCE.xml"
            if (message.matches(".*[A-Za-z]:\\\\.*\\.xml.*") // Windows路径格式
                    || message.matches(".*/.*\\.xml.*")) { // Unix路径格式
                // 检查消息是否基本只包含路径（去除空格后长度接近）
                String trimmedMessage = message.trim();
                if (!trimmedMessage.contains(" ") ||
                        trimmedMessage.split("\\s+").length <= 2) {
                    // 消息中几乎没有空格或只有很少单词，很可能只是文件路径
                    return true;
                }
            }
        }

        return false;
    }

    /**
     *将英文错误信息翻译成中文
     */
    private static String translateErrorMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String lower = message.toLowerCase();

        // 1. XML File Encoding Format Error（编码相关错误）
        // 检测编码格式错误，包括无效的编码名称（如UTF-16 LE）和UTF-8编码异常
        if (lower.contains("invalid byte") || lower.contains("malformed")) {
            if (lower.contains("utf")) {
                return "XML File Encoding Format Error";
            }
            return "XML File Encoding Format Error";
        }
        // 检测无效的编码名称（如 "Invalid encoding name"）
        if (lower.contains("encoding") && (lower.contains("invalid") || lower.contains("unsupported"))) {
            return "XML File Encoding Format Error";
        }

        // 2. Incorrect Start Tag Format（起始标签格式错误）
        if (lower.contains("element type")
                && (lower.contains("must be followed") || lower.contains("not terminated"))) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("content of elements must consist")) {
            return "Incorrect Start Tag Format";
        }
        if (lower.contains("markup") && lower.contains("preceding the root element")) {
            return "Incorrect Start Tag Format";
        }

        // 5. Incorrect closing tag name（结束标签格式错误）
        if (lower.contains("end-tag") || (lower.contains("closing tag") && lower.contains("does not match"))) {
            return "Incorrect closing tag name";
        }

        // 3. Incorrect XML header format（表头/声明错误）
        // 必须优先于根元素检测，因为XML头部错误也可能触发"root element"错误消息
        if (lower.contains("xml declaration") || lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        // 检测"根元素之前的标记"相关错误 - 这实际上是指XML声明部分
        if (lower.contains("pseudo attribute name")) {
            return "Incorrect XML header format";
        }
        // 检测处理指令相关错误（XML声明本身就是一个处理指令）
        if (lower.contains("processing instruction")) {
            // 如果明确提到"target"，可能是注释格式问题
            if (lower.contains("target") && lower.contains("matching")) {
                return "Incorrect XML comment format";
            }
            // 其他处理指令错误通常是XML头部问题
            return "Incorrect XML header format";
        }
        // 检测"content is not allowed in prolog"等前导内容错误
        if (lower.contains("content") && lower.contains("prolog")) {
            return "Incorrect XML header format";
        }
        // 检测"markup must be well-formed"相关错误
        if (lower.contains("markup") && lower.contains("well-formed")) {
            return "Incorrect XML header format";
        }

        // 4. Incorrect XML comment format（注释格式错误）
        if (lower.contains("comment") && (lower.contains("not well-formed") || lower.contains("invalid"))) {
            return "Incorrect XML comment format";
        }
        if (lower.contains("comments") && lower.contains("not permitted")) {
            return "Incorrect XML comment format";
        }

        // 内存错误（归入7种reason之一：Root element parsing exception）
        if (lower.contains("out of memory") || lower.contains("java heap space")) {
            return "Root element parsing exception";
        }

        // 6. Root element parsing exception（根元素错误）
        // 检测真正的根元素错误（如根元素外有内容）
        if (lower.contains("root element") || lower.contains("document element")
                || lower.contains("trailing section")) {
            return "Root element parsing exception";
        }

        // 7. Attribute Definition Exception（属性错误）
        if (lower.contains("attribute")
                && (lower.contains("invalid") || lower.contains("duplicate") || lower.contains("malformed")
                        || lower.contains("open quote is expected"))) {
            return "Attribute Definition Exception";
        }

        // 其他常见错误（归入7种reason之一：Root element parsing exception）
        if (lower.contains("premature end of file") || lower.contains("unexpected end of file")) {
            return "Root element parsing exception";
        }
        if (lower.contains("not well-formed")) {
            return "Root element parsing exception";
        }

        // 未匹配的场景统一归入7种reason之一，保证reason取值始终为需求表中的7种之一
        return "Root element parsing exception";
    }

    /**
     *处理XML解析异常消息，将不友好的错误信息转换为更易理解的提示
     *使用Throwable而非Exception以便处理OutOfMemoryError等Error类型
     */
    static String formatXmlErrorMessage(Throwable e) {
        String message = e.getMessage() != null ? e.getMessage().trim() : "";

        // 去除末尾的点和空格
        if (message != null && !message.isEmpty()) {
            message = message.replaceAll("[.\\s]+$", "");
        }

        if (message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }

        // 翻译错误信息
        return translateErrorMessage(message);
    }

    /**
     *验证输入参数格式
     *检查参数是否为空、路径格式是否正确等
     *
     *@param inputXmlFile             XML文件路径
     *@param inputPath                输入路径
     *@param outputVerifyReportFolder 输出报告文件夹路径
     *@param outputLogPath            输出日志路径
     *@return true 如果参数格式有效，false 否则
     */
    static boolean validateParameters(String inputXmlFile, String inputPath,
            String outputVerifyReportFolder, String outputLogPath) {
        // 检查参数是否为空
        if (inputXmlFile == null || inputXmlFile.trim().isEmpty()) {
            System.err.println("Error: Input XML file path cannot be empty");
            return false;
        }

        if (inputPath == null || inputPath.trim().isEmpty()) {
            System.err.println("Error: Input path cannot be empty");
            return false;
        }

        if (outputVerifyReportFolder == null || outputVerifyReportFolder.trim().isEmpty()) {
            System.err.println("Error: Output report folder path cannot be empty");
            return false;
        }

        if (outputLogPath == null || outputLogPath.trim().isEmpty()) {
            System.err.println("Error: Output log path cannot be empty");
            return false;
        }

        // 检查参数是否包含Windows文件路径中非法的特殊字符
        // Windows文件名禁止字符: < > : " / \ | ? *
        // 其中 : \ / 在路径中是合法的（盘符和路径分隔符），所以不检测
        // 注意：第4个参数(outputLogPath)的特殊字符检查在后续单独处理，报错码为1011
        String[] firstThreeParams = { inputXmlFile, inputPath, outputVerifyReportFolder };
        String[] invalidChars = { "*", "?", "<", ">", "|", "\"" };
        for (String param : firstThreeParams) {
            for (String invalidChar : invalidChars) {
                if (param.contains(invalidChar)) {
                    return false;
                }
            }
        }

        // 检查输出报告文件夹路径格式（不应该包含文件扩展名）
        String reportPathLower = outputVerifyReportFolder.toLowerCase();
        String[] invalidExtensions = { ".csv", ".txt", ".xml", ".xlsx", ".xls", ".bin", ".dat", ".json" };
        for (String ext : invalidExtensions) {
            if (reportPathLower.endsWith(ext)) {
                System.err.println("Error: Output report path should be a folder, not a file");
                return false;
            }
        }

        return true;
    }

    /**
     * validateXmlFileName
     *
     * @brief
     * 功能: 执行 validateXmlFileName 相关处理。
     *
     * @scope internal
     * @errno:
     * N/A
     */
    static boolean validateXmlFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        // 002080：CPM、L/R、Module均按需求使用固定大小写。
        String pattern = "^CPM_[LR][0-9]_Module\\.xml$";
        return fileName.matches(pattern);
    }
}
