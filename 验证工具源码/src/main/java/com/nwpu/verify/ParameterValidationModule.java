/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: ParameterValidationModule.java
 * 描述    : 负责命令行参数和输入输出路径校验。
 */
package com.nwpu.verify;

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.layers.output.Log;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;

/**
 * 参数校验功能，对应设计文档中的 {@code validateParameters()} 接口。
 */
final class ParameterValidationModule {
    /** 必须单独执行的辅助命令。 */
    private static final String VERSION_COMMAND = "-v";
    private static final String ERROR_LIST_COMMAND = "-le";
    private static final String HELP_COMMAND = "-h";

    /**
     * TDD-CPMGenVerify-001：输入参数校验。
     *
     * 校验命令行参数并返回参数阶段结果。
     *
     * @param args 命令行参数
     * @return 参数解析和校验结果
     */
    ParameterValidationResult validateParameters(String[] args) {
        ParameterValidationResult result = new ParameterValidationResult();
        if (Application.isPotentialAuxiliaryCommand(args)) {
            if (args.length == 1) {
                String command = args[0].toLowerCase(java.util.Locale.ROOT);
                switch (command) {
                    case VERSION_COMMAND:
                        Application.printVersion();
                        result.auxiliaryCommandHandled = true;
                        return result;
                    case ERROR_LIST_COMMAND:
                        Application.printErrorTypes();
                        result.auxiliaryCommandHandled = true;
                        return result;
                    case HELP_COMMAND:
                        Application.printHelp();
                        result.auxiliaryCommandHandled = true;
                        return result;
                    default:
                        break;
                }
            }

            Application.reportAuxiliaryParameterError();
            return result;
        }

        // 1002和1003分别对应参数格式错误、参数数量/可选参数错误。
        result.parameterCountError = args.length < 4;

        if (args.length >= 4) {
            result.inputXmlFile = args[0];
            result.inputPath = args[1];
            result.outputVerifyReportFolder = args[2];
            result.outputLogPath = args[3];

            if (!validateParameters(result.inputXmlFile, result.inputPath,
                    result.outputVerifyReportFolder, result.outputLogPath)) {
                result.parameterFormatError = true;
            }
        } else if (args.length > 0) {
            result.parameterFormatError = !validateParameters(
                    args[0],
                    args.length > 1 ? args[1] : "",
                    args.length > 2 ? args[2] : "",
                    "");
        }

        // 解析可选参数。即使必选参数格式错误，也继续检查可选参数。
        String endianType = "big";
        String osBitType = "64";
        boolean ncSeen = false;
        boolean endianSeen = false;
        boolean bitSeen = false;

        if (args.length > 4) {
            for (int i = 4; i < args.length; i++) {
                if ("-nc".equals(args[i])) {
                    if (ncSeen) {
                        result.parameterFormatError = true;
                        result.parameterCountError = true;
                    }
                    result.enableXmlCrcCheck = false;
                    ncSeen = true;
                } else if ("-gen".equals(args[i])) {
                    result.genMode = true;
                } else if (Application.isGeneratorOption(args[i])) {
                    String normalizedOption = Application.normalizeGeneratorOption(args[i]);
                    if ("big".equals(normalizedOption) || "little".equals(normalizedOption)) {
                        if (endianSeen) {
                            result.parameterFormatError = true;
                            result.parameterCountError = true;
                        }
                        endianType = normalizedOption;
                        endianSeen = true;
                    } else {
                        if (bitSeen) {
                            result.parameterFormatError = true;
                            result.parameterCountError = true;
                        }
                        osBitType = normalizedOption;
                        bitSeen = true;
                    }
                } else {
                    result.parameterFormatError = true;
                    result.parameterCountError = true;
                }
            }
        }

        if (result.parameterFormatError) {
            Log.error(CodeConstant.ERROR_INVALID_PARAMETERS,
                    "Invalid parameters. Use the -h command for more help.");
        }
        if (result.parameterCountError) {
            Application.logParameterCountError();
        }
        if (result.parameterFormatError || result.parameterCountError) {
            Application.exitWithErrorCode(result.parameterFormatError
                    ? CodeConstant.ERROR_INVALID_PARAMETERS
                    : CodeConstant.ERROR_PARAMS_COUNT_INVALID);
        }

        result.generatorType = osBitType + endianType;

        if (args.length >= 4) {
            Application.printCopyrightAndVersion();
            Application.printUserInputParameters(args);
            Log.resetStage();
            Log.startStage("Parameter Verification");
            Log.info("Initiating parameter validation.");
        }
        return result;
    }

    /**
     * 文档接口：校验四个必选输入参数的格式。
     *
     * @param inputXmlFile XML 文件路径
     * @param inputPath BIN 文件目录
     * @param outputVerifyReportFolder 报告目录
     * @param outputLogPath 日志目录
     * @return 参数格式是否正确
     */
    boolean validateParameters(String inputXmlFile, String inputPath,
            String outputVerifyReportFolder, String outputLogPath) {
        return Application.validateParameters(inputXmlFile, inputPath,
                outputVerifyReportFolder, outputLogPath);
    }

    /**
     * 校验输入文件、输出目录，并初始化日志阶段。
     *
     * @param inputXmlFile XML 配置文件路径
     * @param inputPath BIN 文件目录路径
     * @param outputVerifyReportFolder 验证报告输出目录路径
     * @param outputLogPath 运行状态日志输出目录路径
     * @return 输入输出文件校验结果
     */
    InputFileValidationResult validateInputFiles(String inputXmlFile, String inputPath,
            String outputVerifyReportFolder, String outputLogPath) {
        Application.beginDeferredErrorCollection();

        File xmlFile = new File(inputXmlFile);

        if (!xmlFile.exists() || !xmlFile.isFile()) {
            String errorMsg = "File path invalid: '" + inputXmlFile + " '. File does not exist.";
            Log.error(CodeConstant.ERROR_XML_FOLDER_NOT_EXIST, errorMsg);
            Application.exitWithErrorCode(CodeConstant.ERROR_XML_FOLDER_NOT_EXIST);
        }

        if (xmlFile.exists() && !xmlFile.canRead()) {
            String errorMsg = String.format(
                    "XML file %s failed CRC validation. CRC32: 0x[]; Computed CRC32: 0x[]. File Path: %s",
                    xmlFile.getName(), Application.getCleanPath(xmlFile));
            Log.error(CodeConstant.ERROR_XML_CRC_VALIDATION_FAILED, errorMsg);
            Application.exitWithErrorCode(CodeConstant.ERROR_XML_CRC_VALIDATION_FAILED);
        }

        Log.info("XML file found: " + Application.getCleanPath(xmlFile));

        String xmlFileName = xmlFile.getName();
        if (!Application.validateXmlFileName(xmlFileName)) {
            String errorMsg = "XML file " + xmlFileName
                    + " does not match name rules,rules:CPM_XX_Module.xml,the 1st X means left and right ,value for L or R,the 2nd X means number,value for 0-9.";
            Log.error(CodeConstant.ERROR_XML_FILENAME_INVALID_FORMAT, errorMsg);
            Application.exitWithErrorCode(CodeConstant.ERROR_XML_FILENAME_INVALID_FORMAT);
        } else {
            Log.info("XML Module file name format validation passed: " + xmlFileName);
        }

        File binFolder = new File(inputPath);
        if (!binFolder.exists() || !binFolder.isDirectory()) {
            String errorMsg = "input BIN file path(2nd parameter) error ,please re-enter parameters.";
            Log.error(CodeConstant.ERROR_BIN_FILE_PATH_INVALID, errorMsg);
            Application.exitWithErrorCode(CodeConstant.ERROR_BIN_FILE_PATH_INVALID);
        }

        Log.info("BIN file path is valid: " + Application.getCleanPath(binFolder));
        Application.checkRequiredBinFiles(binFolder);
        File[] binFiles = binFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".bin"));
        if (binFiles != null && binFiles.length > 0) {
            HashSet<String> requiredFileSet = new HashSet<>(Arrays.asList(Application.REQUIRED_BIN_FILES));
            for (File binFile : binFiles) {
                if (!requiredFileSet.contains(binFile.getName())) {
                    String errorMsg = String.format(
                            "Redundant BIN file '%s' found in '%s'. Please remove it or ensure the directory only contains the required files [payloadMap_rom.bin, cfgRd.bin].",
                            binFile.getName(), Application.getCleanPath(binFolder));
                    Log.error(CodeConstant.ERROR_REDUNDANT_BIN_FILE, errorMsg);
                    Application.exitWithErrorCode(CodeConstant.ERROR_REDUNDANT_BIN_FILE);
                }
            }
            Log.info("BIN folder check passed, contains only required files: "
                    + Application.getCleanPath(binFolder));
        }

        for (String requiredBinName : Application.REQUIRED_BIN_FILES) {
            File requiredBinFile = new File(binFolder, requiredBinName);
            boolean binOpenFailed = false;
            try (java.io.RandomAccessFile binRaf = new java.io.RandomAccessFile(requiredBinFile, "r")) {
                if (binRaf.length() < 4) {
                    binOpenFailed = true;
                }
            } catch (IOException e) {
                binOpenFailed = true;
            }
            if (binOpenFailed) {
                String errorMsg = "failed to open Bin File . the folder path of the file: "
                        + Application.getCleanPath(binFolder) + " .";
                Log.error(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED, errorMsg);
                Application.exitWithErrorCode(CodeConstant.ERROR_OPEN_BIN_FILE_FAILED);
            }
        }

        File reportFolder = new File(outputVerifyReportFolder);
        if (!reportFolder.exists() || !reportFolder.isDirectory()) {
            String errorMsg = "Folder path" + outputVerifyReportFolder
                    + " invalid: Directory does not exist.";
            Log.error(CodeConstant.ERROR_REPORT_FOLDER_NOT_EXIST, errorMsg);
            Application.exitWithErrorCode(CodeConstant.ERROR_REPORT_FOLDER_NOT_EXIST);
        }

        checkFolderWritable(reportFolder, outputVerifyReportFolder,
                CodeConstant.ERROR_REPORT_FOLDER_NOT_WRITABLE,
                "Write permission denied for folder path: '");
        Log.info("Report output folder is valid: " + Application.getCleanPath(reportFolder));

        File logFolder = validateLogFolder(outputLogPath);

        Integer deferredErrorCode = Application.getDeferredErrorCode();
        if (deferredErrorCode != null && Application.isParameterValidationError(deferredErrorCode)) {
            Application.finishDeferredError(deferredErrorCode);
        }

        LocalDateTime sessionStartDateTime = LocalDateTime.now();
        String sessionTimestamp = sessionStartDateTime.format(Log.FILE_TIME_FORMATTER);
        String sessionDateTimeStr = sessionStartDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Application.sessionDateTimeStr = sessionDateTimeStr;

        // TDD-CPMGenVerify-009：运行状态记录文件的生成，此处为 initialize 的实际调用位置。
        boolean logInitSuccess = new ReportGenerationModule().initialize(outputLogPath,
                sessionTimestamp);
        if (logInitSuccess) {
            Log.recordCurrentStageStartInLog("Parameter Verification");
            Log.info("Parameter validation completed.");
            Log.completeStage("Parameter Verification", 0);
            Log.reportProgress(10);
        } else {
            Log.error(CodeConstant.ERROR_GENERATE_LOG_FAILED, "failed to generate running status file.");
            new ReportGenerationModule().generateFallbackReportAfterLogFailure(
                    outputVerifyReportFolder, sessionTimestamp, sessionDateTimeStr, Application.commandLineStr);
            Application.clearDeferredErrorState();
            System.out.flush();
            Log.close();
            System.exit(1);
        }

        return new InputFileValidationResult(xmlFile, binFolder, reportFolder, logFolder,
                sessionTimestamp, sessionDateTimeStr);
    }

    /**
     * 校验运行状态日志目录的格式、存在性和写权限。
     *
     * @param outputLogPath 运行状态日志目录路径
     * @return 校验通过的日志目录
     */
    private File validateLogFolder(String outputLogPath) {
        char[] invalidChars = { '"', '|', '*', '?', '<', '>' };
        for (char c : invalidChars) {
            if (outputLogPath.indexOf(c) >= 0) {
                reportLogPathError();
            }
        }

        String logPathLower = outputLogPath.toLowerCase();
        String[] invalidExtensions = { ".log", ".txt", ".xml", ".xlsx", ".xls", ".bin", ".dat", ".json" };
        for (String extension : invalidExtensions) {
            if (logPathLower.endsWith(extension)) {
                reportLogPathError();
            }
        }

        Log.info("Running status log folder path format is valid: " + outputLogPath);
        File logFolder = new File(outputLogPath);
        if (!logFolder.exists() || !logFolder.isDirectory()) {
            String errorMsg = "Folder path invalid: '" + outputLogPath + "'. Directory does not exist.";
            Log.error(CodeConstant.ERROR_LOG_FOLDER_NOT_EXIST, errorMsg);
            Application.exitWithErrorCode(CodeConstant.ERROR_LOG_FOLDER_NOT_EXIST);
        }

        Log.info("Running status log folder path is valid: " + Application.getCleanPath(logFolder));
        checkFolderWritable(logFolder, outputLogPath,
                CodeConstant.ERROR_LOG_FOLDER_NOT_WRITABLE,
                "Write permission denied for folder path: '");
        Log.info("Running status log folder write permission check passed: "
                + Application.getCleanPath(logFolder));
        return logFolder;
    }

    /**
     * 检查目录写权限。
     *
     * @param folder 待检查目录
     * @param displayPath 错误消息使用的路径
     * @param errorCode 校验失败错误码
     * @param messagePrefix 错误消息前缀
     */
    private void checkFolderWritable(File folder, String displayPath, int errorCode, String messagePrefix) {
        try {
            File tempFile = File.createTempFile("test_", ".tmp", folder);
            if (!tempFile.delete()) {
                throw new IOException("Failed to delete temporary file: " + tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            String errorMsg = messagePrefix + displayPath + "'.";
            Log.error(errorCode, errorMsg);
            Application.exitWithErrorCode(errorCode);
        }
    }

    /**
     * 输出日志路径格式错误并结束参数校验。
     */
    private void reportLogPathError() {
        String errorMsg = "output running status file path(4nd parameter) error ,please re-enter parameters.";
        Log.error(CodeConstant.ERROR_LOG_PATH_INVALID, errorMsg);
        Application.exitWithErrorCode(CodeConstant.ERROR_LOG_PATH_INVALID);
    }
}
