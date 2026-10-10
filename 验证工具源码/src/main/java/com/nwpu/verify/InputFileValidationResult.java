/**
 * 版权中航工业西安航空计算技术研究所
 * All rights reserved.
 *
 * 文件名称: InputFileValidationResult.java
 * 描述    : 保存输入输出文件校验结果。
 */
package com.nwpu.verify;

import java.io.File;

/**
 * 输入文件和输出目录校验结果。
 */
final class InputFileValidationResult {
    /** XML 配置文件。 */
    final File xmlFile;

    /** BIN 文件目录。 */
    final File binFolder;

    /** 验证报告输出目录。 */
    final File reportFolder;

    /** 运行状态日志输出目录。 */
    final File logFolder;

    /** 本次运行使用的文件时间戳。 */
    final String sessionTimestamp;

    /** 本次运行使用的显示时间。 */
    final String sessionDateTimeStr;

    /**
     * 创建输入输出文件校验结果。
     *
     * @param xmlFile XML 配置文件
     * @param binFolder BIN 文件目录
     * @param reportFolder 验证报告输出目录
     * @param logFolder 运行状态日志输出目录
     * @param sessionTimestamp 文件时间戳
     * @param sessionDateTimeStr 显示时间
     */
    InputFileValidationResult(File xmlFile, File binFolder, File reportFolder, File logFolder,
            String sessionTimestamp, String sessionDateTimeStr) {
        this.xmlFile = xmlFile;
        this.binFolder = binFolder;
        this.reportFolder = reportFolder;
        this.logFolder = logFolder;
        this.sessionTimestamp = sessionTimestamp;
        this.sessionDateTimeStr = sessionDateTimeStr;
    }
}
