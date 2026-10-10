/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 * 
 * 文件名称: ReportGenerator.java
 * 
 * 描述    : 根据二进制比对结果生成CSV格式验证报告。
 * 
 * 修改历史:
 *     2026-06-07  开发人员  翻新文件注释，统一注释格式。
 *     2026-07-27  开发人员  按B-07任务修复CSV报告列顺序、Actual/Expected赋值方向、新增Line列，并统一统计判断为"Success"。
 *     2026-07-27  开发人员  按B-08任务新增Input File行、使用统一会话时间戳，并调整generatorReport/exportToCSV方法签名。
 *     2026-08-01  开发人员  按001050输出字段比对失败对应的XML文件名和行号。
 */

package com.nwpu.verify.layers.output;

/* 头文件 */
import com.nwpu.verify.entity.ComparisonResult;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @brief
 * 功能: 生成二进制验证报告，并负责报告路径校验、CSV转义、报告头尾信息输出。
 */
public class ReportGenerator {

    /**
     * getCleanPath
     *
     * @brief
     * 功能: 获取文件的规范路径，去除路径中的冗余表示。
     * 
     * @param[in]  file: 待处理的文件对象。
     * @return
     * String: 文件规范路径；规范路径获取失败时返回绝对路径。
     * @implements 
     * @scope internal
     * @errno:
     * IOException: 获取规范路径失败时捕获并返回绝对路径。
     */
    private static String getCleanPath(java.io.File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /* 全局变量 */
    private static final String TOOL_NAME = "CPM_Gen_Verify";
    private static final String TOOL_VERSION = "TLF42200105A-AF";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * generatorReport
     *
     * @brief
     * 功能: 根据字段比对结果和统计信息生成验证报告。
     *       报告生成流程如下：
     *       1. 接收比对结果列表和报告输出路径；
     *       2. 使用BIN文件名称写入报告头部；
     *       3. 输出成功字段数、失败字段数和整体验证状态；
     *       4. 调用CSV导出流程写入报告文件。
     * 
     * @param[in]  comparisonResults: 比对结果列表。
     * @param[in]  filePath: 报告文件路径。
     * @param[in]  binFileName: BIN文件名称，用于报告头部。
     * @param[in]  totalSuccess: 验证成功的字段数。
     * @param[in]  totalFails: 验证失败的字段数。
     * @param[in]  validationPassed: 整体验证是否通过。
     * @param[in]  sessionTimestamp: 会话时间戳（格式 yyyyMMdd_HHmmss），用于文件名对齐。
     * @param[in]  sessionDateTime: 会话日期时间（格式 yyyy-MM-dd HH:mm:ss），用于报告头部Created Date。
     * @param[in]  commandLine: 命令行字符串，用于报告头部Input File行。
     * @return
     * N/A
     * @implements
     * @scope external
     * @errno:
     * IOException: 报告文件路径校验、目录创建或文件写入失败。
     */
    public void generatorReport(List<ComparisonResult> comparisonResults, String filePath,
            String binFileName, int totalSuccess, int totalFails, boolean validationPassed,
            String sessionTimestamp, String sessionDateTime, String commandLine) throws IOException {
        exportToCSV(comparisonResults, filePath, binFileName, totalSuccess, totalFails, validationPassed,
                sessionTimestamp, sessionDateTime, commandLine);
    }

    /**
     * generatorReport
     *
     * @brief
     * 功能: 兼容旧接口，根据比对结果自动统计成功和失败数量后生成验证报告。
     *       统计规则如下：
     *       1. fVerificationResult等于PASS时计入成功数量；
     *       2. 其余结果计入失败数量；
     *       3. 失败数量为0时整体验证状态为通过；
     *       4. 未提供BIN文件名时使用Unknown写入报告头部。
     * 
     * @param[in]  comparisonResults: 比对结果列表。
     * @param[in]  filePath: 报告文件路径。
     * @param[in]  sessionTimestamp: 会话时间戳（格式 yyyyMMdd_HHmmss），用于文件名对齐。
     * @param[in]  sessionDateTime: 会话日期时间（格式 yyyy-MM-dd HH:mm:ss），用于报告头部Created Date。
     * @param[in]  commandLine: 命令行字符串，用于报告头部Input File行。
     * @return
     * N/A
     * @implements
     * @scope external
     * @errno:
     * IOException: 报告文件路径校验、目录创建或文件写入失败。
     */
    public void generatorReport(List<ComparisonResult> comparisonResults, String filePath,
            String sessionTimestamp, String sessionDateTime, String commandLine) throws IOException {
        /* 计算统计信息 */
        int totalSuccess = 0;
        int totalFails = 0;
        for (ComparisonResult result : comparisonResults) {
            /* B07-3: 统一使用 "Success" 判断，与 BinComparator 中设置的值保持一致 */
            if ("Success".equals(result.getfVerificationResult())) {
                totalSuccess++;
            } else {
                totalFails++;
            }
        }
        boolean validationPassed = (totalFails == 0);
        exportToCSV(comparisonResults, filePath, "Unknown", totalSuccess, totalFails, validationPassed,
                sessionTimestamp, sessionDateTime, commandLine);
    }

    /**
     * validateFilePath
     *
     * @brief
     * 功能: 验证报告文件路径的有效性。
     *       检查场景如下：
     *       1. 文件路径长度超过Windows MAX_PATH限制；
     *       2. 文件名包含Windows非法字符；
     *       3. 文件扩展名不是.csv；
     *       4. 路径指向目录而不是文件。
     *       文件路径为空或null的情况由Application.java中的1009错误码处理。
     * 
     * @param[in]  filePath: 待验证的报告文件路径。
     * @return
     * N/A
     * @implements PR_35
     * @scope internal
     * @errno:
     * IOException: 报告文件路径无效。
     */
    private void validateFilePath(String filePath) throws IOException {
        /* 文件路径为空或null的情况由Application.java中的1009错误码处理 */
        /* 这里不再检查，直接返回（让后续代码自然抛出异常） */
        if (filePath == null || filePath.trim().isEmpty()) {
            return; /* 交给后续 new File() 等操作自然失败 */
        }

        /* 场景1: 文件路径过长 (Windows MAX_PATH = 260) */
        if (filePath.length() > 260) {
            throw new IOException("Report file path exceeds maximum length (260 characters): " + filePath.length());
        }

        /* 场景3: 检查文件名是否包含非法字符 (Windows: < > : " | ? *) */
        java.io.File file = new java.io.File(filePath);
        String fileName = file.getName();
        String[] illegalChars = { "<", ">", ":", "\"", "|", "?", "*" };
        for (String illegalChar : illegalChars) {
            if (fileName.contains(illegalChar)) {
                throw new IOException("Report file name contains illegal character '" + illegalChar + "': " + fileName);
            }
        }

        /* 场景4: 检查文件扩展名 */
        if (!filePath.toLowerCase().endsWith(".csv")) {
            throw new IOException("Report file must have .csv extension: " + filePath);
        }

        /* 场景5: 检查路径是否指向一个目录而不是文件 */
        if (file.exists() && file.isDirectory()) {
            throw new IOException("Report path points to a directory, not a file: " + filePath);
        }
    }

    /**
     * exportToCSV
     *
     * @brief
     * 功能: 将字段比对结果导出为CSV格式报告。
     *       CSV列定义如下：
     *       Offset 字段偏移量；
     *       Field Name 字段名称；
     *       Actual Value 实际值；
     *       Expected Value 期望值；
     *       Validation Result 验证结果。
     *       导出流程包括路径校验、父目录创建、可写性检查、BOM写入、头部信息写入、数据行写入和尾部统计写入。
     * 
     * @param[in]  comparisonResults: 比对结果列表。
     * @param[in]  filePath: 报告文件路径。
     * @param[in]  binFileName: BIN文件名称，用于报告头部。
     * @param[in]  totalSuccess: 验证成功的字段数。
     * @param[in]  totalFails: 验证失败的字段数。
     * @param[in]  validationPassed: 整体验证是否通过。
     * @param[in]  sessionTimestamp: 会话时间戳（格式 yyyyMMdd_HHmmss），用于文件名对齐。
     * @param[in]  sessionDateTime: 会话日期时间（格式 yyyy-MM-dd HH:mm:ss），用于报告头部Created Date。
     * @param[in]  commandLine: 命令行字符串，用于报告头部Input File行。
     * @return
     * N/A
     * @implements
     * @scope internal
     * @errno:
     * IOException: 路径校验、目录创建或CSV文件写入失败。
     */
    private void exportToCSV(List<ComparisonResult> comparisonResults, String filePath,
            String binFileName, int totalSuccess, int totalFails, boolean validationPassed,
            String sessionTimestamp, String sessionDateTime, String commandLine) throws IOException {
        /* PR_35: 验证文件路径 */
        validateFilePath(filePath);

        /* PR_35: 确保父目录存在 */
        java.io.File reportFile = new java.io.File(filePath);
        java.io.File parentDir = reportFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create report directory: " + getCleanPath(parentDir));
            }
        }

        /* PR_35: 验证目录是否可写 */
        if (parentDir != null && !parentDir.canWrite()) {
            throw new IOException("Report directory is not writable: " + getCleanPath(parentDir));
        }

        /* PR_35: 检查文件是否已存在且不可写 */
        if (reportFile.exists() && !reportFile.canWrite()) {
            throw new IOException("Report file exists and is not writable: " + filePath);
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            /* 写入BOM标记，使Excel能正确识别UTF-8编码 */
            writer.write('\uFEFF');

            /* 写入报告头部信息 */
            writeReportHeader(writer, binFileName, sessionDateTime, commandLine);
            writer.newLine();

            /* 写入标题行 */
            /* 001050: 列顺序为 Offset, Field Name, Expected Value, Actual Value, Verification Result, Line */
            String[] headers = { "Offset", "Field Name", "Expected Value", "Actual Value", "Verification Result", "Line" };
            writer.write(String.join(",", headers));
            writer.newLine();

            /* 写入数据行 */
            for (ComparisonResult result : comparisonResults) {
                String offset = escapeCSV(result.getfOffset() != null ? result.getfOffset() : "");
                String fieldName = escapeCSV(result.getfBinaryFieldName() != null ? result.getfBinaryFieldName() : "");
                /* 001050: Expected Value = 工具计算的基准值（fBinaryFieldValue），Actual Value = BIN文件读取的实际值（fActualValue） */
                String expectedValue = escapeCSV(
                        result.getfBinaryFieldValue() != null ? result.getfBinaryFieldValue() : "");
                String actualValue = escapeCSV(
                        result.getfActualValue() != null ? result.getfActualValue() : "");
                String verificationResult = escapeCSV(
                        result.getfVerificationResult() != null ? result.getfVerificationResult() : "");
                /* 001050: 仅失败项填写来源；非XML属性来源由上游明确设置为0。 */
                String line = "";
                if ("Fail".equals(result.getfVerificationResult())) {
                    line = escapeCSV(result.getfLine() != null && !result.getfLine().trim().isEmpty()
                            ? result.getfLine() : "0");
                }

                writer.write(String.format("%s,%s,%s,%s,%s,%s",
                        offset, fieldName, expectedValue, actualValue, verificationResult, line));
                writer.newLine();
            }

            /* 写入报告尾部统计信息 */
            writer.newLine();
            writeReportFooter(writer, totalSuccess, totalFails, validationPassed);
            writer.flush();
        }

        /* 005177/006381：写入过程未抛异常不代表报告已经生成成功。
         * 关闭输出流后再次确认目标是非空普通文件，避免把空文件或被外部删除的文件
         * 错误报告为生成成功；失败由上层统一转换为3013。 */
        java.io.File generatedReport = new java.io.File(filePath);
        if (!generatedReport.isFile() || generatedReport.length() == 0) {
            throw new IOException("Report file was not generated or is empty: " + filePath);
        }
    }

    /**
     * writeReportHeader
     *
     * @brief
     * 功能: 写入验证报告头部信息，包括工具名称、版本、创建时间、输入文件和验证文件名称。
     *       处理规则如下：
     *       1. Created Date使用统一会话日期时间，保证CSV和LOG时间戳一致；
     *       2. Input File行记录命令行参数，用于追溯输入来源；
     *       3. sessionDateTime为null时回退到当前时间，保证健壮性。
     *
     * @param[in]  writer: CSV报告写入器。
     * @param[in]  binFileName: BIN文件名称。
     * @param[in]  sessionDateTime: 会话日期时间（格式 yyyy-MM-dd HH:mm:ss），用于Created Date。
     * @param[in]  commandLine: 命令行字符串，用于Input File行。
     * @return
     * N/A
     * @implements
     * @scope internal
     * @errno:
     * IOException: 写入报告头部失败。
     */
    private void writeReportHeader(BufferedWriter writer, String binFileName,
            String sessionDateTime, String commandLine) throws IOException {
        /* 002146: 使用统一会话日期时间，保证CSV和LOG时间戳完全一致 */
        String createdDate = sessionDateTime != null
                ? sessionDateTime : LocalDateTime.now().format(DATE_TIME_FORMATTER);

        writer.write("Tool Name : " + TOOL_NAME);
        writer.newLine();
        writer.write("Tool Version : " + TOOL_VERSION);
        writer.newLine();
        writer.write("Created Date : " + createdDate);
        writer.newLine();
        /* 001050: Input File 行冒号前无空格 */
        writer.write("Input File:" + (commandLine != null ? commandLine : ""));
        writer.newLine();
        writer.write("Validation File : " + binFileName);
        writer.newLine();
    }

    /**
     * writeReportFooter
     *
     * @brief
     * 功能: 写入验证报告尾部统计信息，包括整体验证状态、成功数量和失败数量。
     * 
     * @param[in]  writer: CSV报告写入器。
     * @param[in]  totalSuccess: 验证成功的字段数。
     * @param[in]  totalFails: 验证失败的字段数。
     * @param[in]  validationPassed: 整体验证是否通过。
     * @return
     * N/A
     * @implements 
     * @scope internal
     * @errno:
     * IOException: 写入报告尾部失败。
     */
    private void writeReportFooter(BufferedWriter writer, int totalSuccess, int totalFails, boolean validationPassed)
            throws IOException {
        String status = validationPassed ? "Passed" : "Failed";

        /* 001050: 尾部汇总信息在同一行输出 */
        writer.write("Validation Status : " + status
                + " Total Success : " + totalSuccess
                + " Total Fails : " + totalFails);
        writer.newLine();
    }

    /**
     * escapeCSV
     *
     * @brief
     * 功能: 转义CSV字段值。
     *       转义规则如下：
     *       1. value为null时返回空字符串；
     *       2. 字段包含逗号、引号或换行符时使用双引号包围；
     *       3. 字段内部的双引号转义为两个双引号。
     * 
     * @param[in]  value: 待转义的字段值。
     * @return
     * String: 转义后的CSV字段值。
     * @implements 
     * @scope internal
     * @errno:
     * N/A
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        /* 如果包含逗号、引号或换行符，需要用引号包围 */
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            /* 将引号转义为两个引号 */
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }
}
