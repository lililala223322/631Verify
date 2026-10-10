/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: ReportGenerationModule.java
 * 描述    : 配置验证报告生成接口。
 */
package com.nwpu.verify;

import com.nwpu.verify.entity.CodeConstant;
import com.nwpu.verify.entity.ComparisonResult;
import com.nwpu.verify.layers.output.Log;
import com.nwpu.verify.layers.output.ReportGenerator;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 配置验证报告生成功能，对应设计文档中的 {@code generatorReport()}。
 */
final class ReportGenerationModule {
    /**
     * TDD-CPMGenVerify-009：运行状态记录文件的生成。
     *
     * 初始化运行状态记录文件。
     *
     * @param outputPath 日志目录
     * @param timestamp 会话时间戳
     * @return 初始化是否成功
     */
    boolean initialize(String outputPath, String timestamp) {
        return Log.initialize(outputPath, timestamp);
    }

    /**
     * TDD-CPMGenVerify-008：CPM 配置验证报告的生成。
     *
     * 生成配置验证报告。
     *
     * @param validationResult 配置符合性校验结果
     * @param outputVerifyReportFolder 验证报告输出目录
     * @param sessionTimestamp 本次运行时间戳
     * @param sessionDateTimeStr 本次运行显示时间
     * @param commandLineStr 命令行参数字符串
     */
    void generatorReport(ConsistencyValidationResult validationResult, String outputVerifyReportFolder,
            String sessionTimestamp, String sessionDateTimeStr, String commandLineStr) {
        if (!validationResult.hasBinFiles) {
            return;
        }

        try {
            String reportFile = Application.generateTimestampedReportFileName(
                    outputVerifyReportFolder, sessionTimestamp);
            String binFileNames = String.join(", ", validationResult.processedBinFileNames);
            new ReportGenerator().generatorReport(validationResult.comparisonResults, reportFile,
                    binFileNames, validationResult.totalSuccessCount, validationResult.totalFailCount,
                    validationResult.validationPassed, sessionTimestamp,
                    sessionDateTimeStr, commandLineStr);
            Log.infoLower("succeeded to generate report file.");
        } catch (Exception e) {
            Log.error(CodeConstant.ERROR_GENERATE_REPORT_FAILED, "failed to generate report file.");
            Application.exitWithErrorCode(CodeConstant.ERROR_GENERATE_REPORT_FAILED);
        }
    }

    /**
     * 无 BIN 文件时生成基础报告。
     *
     * @param outputVerifyReportFolder 验证报告输出目录
     * @param sessionTimestamp 本次运行时间戳
     * @param sessionDateTimeStr 本次运行显示时间
     * @param commandLineStr 命令行参数字符串
     */
    void generatorEmptyReport(String outputVerifyReportFolder, String sessionTimestamp,
            String sessionDateTimeStr, String commandLineStr) {
        try {
            String reportFile = Application.generateTimestampedReportFileName(
                    outputVerifyReportFolder, sessionTimestamp);
            new ReportGenerator().generatorReport(new ArrayList<ComparisonResult>(), reportFile,
                    "No BIN Files", 0, 0, true, sessionTimestamp,
                    sessionDateTimeStr, commandLineStr);
            Log.infoLower("succeeded to generate report file.");
        } catch (Exception e) {
            Log.error(CodeConstant.ERROR_GENERATE_REPORT_FAILED, "failed to generate report file.");
            Application.exitWithErrorCode(CodeConstant.ERROR_GENERATE_REPORT_FAILED);
        }
    }

    /**
     * XML 阶段发生停止条件时生成空验证报告并结束进程。
     *
     * @param outputVerifyReportFolder 验证报告输出目录
     * @param sessionTimestamp 本次运行时间戳
     * @param sessionDateTimeStr 本次运行显示时间
     * @param commandLineStr 命令行参数字符串
     */
    void finishXmlValidationError(String outputVerifyReportFolder, String sessionTimestamp,
            String sessionDateTimeStr, String commandLineStr) {
        try {
            String reportFile = Application.generateTimestampedReportFileName(
                    outputVerifyReportFolder, sessionTimestamp);
            new ReportGenerator().generatorReport(new ArrayList<ComparisonResult>(), reportFile,
                    "", 0, 0, false, sessionTimestamp, sessionDateTimeStr, commandLineStr);
            Log.infoLower("succeeded to generate report file.");
        } catch (IOException e) {
            Log.error(CodeConstant.ERROR_GENERATE_REPORT_FAILED, "failed to generate report file.");
            Application.exitWithErrorCode(CodeConstant.ERROR_GENERATE_REPORT_FAILED);
        }
        Application.clearDeferredErrorState();
        System.out.flush();
        Log.close();
        System.exit(1);
    }

    /**
     * 日志初始化失败时尽力生成简化报告。
     *
     * @param outputVerifyReportFolder 验证报告输出目录
     * @param sessionTimestamp 本次运行时间戳
     * @param sessionDateTimeStr 本次运行显示时间
     * @param commandLineStr 命令行参数字符串
     * @return 报告是否生成成功
     */
    boolean generateFallbackReportAfterLogFailure(String outputVerifyReportFolder, String sessionTimestamp,
            String sessionDateTimeStr, String commandLineStr) {
        try {
            String reportFile = Application.generateTimestampedReportFileName(
                    outputVerifyReportFolder, sessionTimestamp);
            new ReportGenerator().generatorReport(new ArrayList<ComparisonResult>(), reportFile,
                    "", 0, 0, false, sessionTimestamp, sessionDateTimeStr, commandLineStr);
            Log.infoLower("succeeded to generate report file.");
            return true;
        } catch (Exception e) {
            Log.error(CodeConstant.ERROR_GENERATE_REPORT_FAILED, "failed to generate report file.");
            return false;
        }
    }
}
